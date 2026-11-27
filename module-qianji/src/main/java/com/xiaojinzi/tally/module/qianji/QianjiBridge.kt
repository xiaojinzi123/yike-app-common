package com.xiaojinzi.tally.module.qianji

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.AccountDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillLabelDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.LabelDo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * B 方案：把钱迹的数据与规则移植到 yike。
 *
 * 不依赖钱迹 apk/dex，只依赖：
 * 1) 钱迹原 SQLite 文件 qianjiapp（greenDAO 表结构）
 * 2) yike 自己的 Room TallyDatabase
 *
 * 数据写入调用方指定的本地账本，省略参数时兼容默认账本（id = 1）：
 * - 不覆盖账本自身的名称/图标（只在账本不存在时创建）
 * - 钱迹分类按名字对齐 yike 分类，缺失的按原名新建，不塞种子分类
 */
object QianjiBridge {

    const val LOCAL_USER_ID = "opensourceUserId"
    const val DEFAULT_BOOK_ID = "1"
    const val DEFAULT_CATEGORY_ID = "qj_other"

    private const val DEFAULT_BOOK_NAME = "默认账本"

    /** 钱迹账单类型（见 Bill.java 常量） */
    private val TRANSFER_TYPES = setOf(
        2, // TRANSFER 账户转账
        3, // CREDIT_HUANKUAN 信用卡还款
    )
    private val INCOME_TYPES = setOf(
        1, // INCOME 收入
        7, // ZHAIWU_LOAN 借款（钱进来）
        11, // ZHAIWU_LIXI_INCOME 债务利息收入
    )

    /** 同一目标账本内按源 id 覆盖；账本 1 保留旧 id，其他账本使用独立命名空间。 */
    suspend fun importQianjiDatabase(
        context: Context,
        source: File,
        targetBookId: String = DEFAULT_BOOK_ID,
        targetUserId: String = LOCAL_USER_ID,
    ): ImportResult = withContext(Dispatchers.IO) {
        require(targetBookId.isNotBlank() && targetUserId.isNotBlank()) { "请选择有效的目标账本" }
        require(source.exists() && source.length() > 0) { "钱迹数据库不存在或为空" }
        val db = SQLiteDatabase.openDatabase(source.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val now = System.currentTimeMillis()
        var billCount = 0
        var budgetCount = 0
        var labelLinkCount = 0
        db.use { qdb -> TallyDb.database.withTransaction {
            ensureBook(now, targetBookId, targetUserId)
            val accountMap = importAccounts(qdb, now, targetBookId, targetUserId)
            val labelMap = importLabels(qdb, now, targetBookId, targetUserId)
            val categoryCache = mutableMapOf<String, String>()
            val bills = mutableListOf<BillDo>()
            val billLabels = mutableListOf<BillLabelDo>()

            // 先收原始行，游标关掉之后再走 suspend 的分类解析
            data class RawBill(
                val id: Long,
                val timeMs: Long,
                // 钱迹 Bill 类型：0 支出 / 1 收入 / 2 转账
                val type: Int,
                val amountCent: Long,
                val categoryName: String,
                val note: String?,
                val assetId: Long?,
                val fromId: Long?,
                val targetId: Long?,
                val tagIds: List<String>,
                val refundSourceId: Long?,
            )

            val rawBills = mutableListOf<RawBill>()
            qdb.rawQuery(
                """
                SELECT billid, TIME, TYPE, MONEY, CATEGORY_ID, REMARK, STATUS, ASSETID, FROMID, TARGETID, EXTRA
                FROM user_bill
                """.trimIndent(), null
            )?.use { c ->
                while (c.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    if (c.getInt(6) == 0) continue // 钱迹软删
                    val type = c.getInt(2)
                    rawBills += RawBill(
                        id = c.getLong(0),
                        // 钱迹 TIME 是秒；兼容已经存成毫秒的备份
                        timeMs = c.getLong(1).let { if (it > 100_000_000_000L) it else it * 1000L },
                        type = type,
                        amountCent = (abs(c.getDouble(3)) * 100.0).roundToLong(),
                        categoryName = if (c.isNull(4)) "" else queryCategoryName(qdb, c.getLong(4)),
                        note = c.getString(5).orEmpty().ifBlank { null },
                        assetId = if (c.isNull(7)) null else c.getLong(7),
                        fromId = if (c.isNull(8)) null else c.getLong(8),
                        targetId = if (c.isNull(9)) null else c.getLong(9),
                        tagIds = parseExtraTagIds(c.getString(10)),
                        refundSourceId = parseExtraRefundSource(c.getString(10)),
                    )
                }
            }

            rawBills.forEach { raw ->
                currentCoroutineContext().ensureActive()
                // 只有源/目标账户都有效时才能表达为转账
                val isTransfer = raw.type in TRANSFER_TYPES &&
                        raw.fromId != null && raw.targetId != null
                // 钱迹 type=20 是退款账单，refundsid 指向原账单
                val isRefund = raw.type == 20 && raw.refundSourceId != null
                // 退款即使没有来源也应按收入处理（钱回来）
                val income = raw.type in INCOME_TYPES || raw.type == 20
                val categoryId = if (isTransfer) {
                    null
                } else {
                    resolveCategory(
                        name = raw.categoryName.ifBlank { "其他" },
                        income = income,
                        now = now,
                        cache = categoryCache,
                        targetBookId = targetBookId,
                        targetUserId = targetUserId,
                    )
                }
                val billId = scopedImportId("qj_${raw.id}", targetBookId)
                bills += BillDo(
                    id = billId,
                    userId = targetUserId,
                    bookId = targetBookId,
                    type = when {
                        isTransfer -> "transfer"
                        isRefund -> "refund"
                        else -> "normal"
                    },
                    originBillId = if (isRefund) scopedImportId("qj_${raw.refundSourceId}", targetBookId) else null,
                    time = raw.timeMs,
                    categoryId = categoryId,
                    accountId = raw.assetId?.let { accountMap[it] }
                        ?: raw.fromId?.let { accountMap[it] },
                    transferTargetAccountId = if (isTransfer) raw.targetId?.let { accountMap[it] } else null,
                    amount = when {
                        // 转账是资金流出源账户，统一记负数
                        isTransfer -> -raw.amountCent
                        // 退款在 yike 里记正数
                        isRefund -> raw.amountCent
                        !income -> -raw.amountCent
                        else -> raw.amountCent
                    },
                    note = raw.note,
                    isNotCalculate = false,
                    timeCreate = now,
                    timeModify = now,
                    isDeleted = false,
                    isSync = false,
                )
                raw.tagIds.forEach { qianjiTagId ->
                    labelMap[qianjiTagId]?.let { labelId ->
                        billLabels += BillLabelDo(
                            id = scopedImportId("qjbl_${raw.id}_$labelId", targetBookId),
                            userId = targetUserId,
                            bookId = targetBookId,
                            billId = billId,
                            labelId = labelId,
                            timeModify = now,
                            isDeleted = false,
                            isSync = false,
                        )
                    }
                }
            }
            if (bills.isNotEmpty()) TallyDb.database.billDao().insert(bills)
            if (billLabels.isNotEmpty()) TallyDb.database.billLabelDao().insert(billLabels)
            billCount = bills.map { it.id }.toSet().size
            labelLinkCount = billLabels.map { it.id }.toSet().size

            budgetCount = importBudgets(qdb, now, targetBookId, targetUserId)
            currentCoroutineContext().ensureActive()
        } }
        ImportResult(
            billCount = billCount,
            budgetCount = budgetCount,
            labelLinkCount = labelLinkCount,
        )
    }

    data class ImportResult(
        val billCount: Int,
        val budgetCount: Int,
        val labelLinkCount: Int = 0,
    )

    /** 只为旧 API 补建默认账本；显式选择的账本必须存在，绝不改写已有账本信息。 */
    private suspend fun ensureBook(
        now: Long,
        targetBookId: String = DEFAULT_BOOK_ID,
        targetUserId: String = LOCAL_USER_ID,
    ) {
        val bookDao = TallyDb.database.bookDao()
        val existing = bookDao.getById(targetBookId)
        if (existing != null) {
            require(existing.userId == targetUserId) { "目标账本已变化，请重新选择" }
            return
        }
        require(targetBookId == DEFAULT_BOOK_ID && targetUserId == LOCAL_USER_ID) { "目标账本不存在，请重新选择" }
        bookDao.insert(
            BookDo(
                id = targetBookId,
                userId = targetUserId,
                isSystem = true,
                type = "normal",
                name = DEFAULT_BOOK_NAME,
                iconName = "book1",
                timeCreate = now,
                timeModify = now,
            )
        )
    }

    /**
     * 导入钱迹资产为 yike 账户。
     *
     * - 钱迹 user_asset.STATUS=0 为有效资产
     * - 钱迹 MONEY 是当前余额（已包含账单影响），这里 balanceInit 置 0，
     *   避免和导入的账单重复计算
     * - 按名字对齐已有账户，没有就用确定性 id `qja_<钱迹id>` 新建
     */
    private suspend fun importAccounts(
        qdb: SQLiteDatabase,
        now: Long,
        targetBookId: String,
        targetUserId: String,
    ): Map<Long, String> {
        data class RawAsset(val id: Long, val name: String)

        val assets = mutableListOf<RawAsset>()
        qdb.rawQuery(
            "SELECT _ID, NAME FROM user_asset WHERE STATUS = 0",
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                assets += RawAsset(
                    id = c.getLong(0),
                    name = c.getString(1).orEmpty().ifBlank { "账户${c.getLong(0)}" },
                )
            }
        }
        if (assets.isEmpty()) return emptyMap()

        val accountDao = TallyDb.database.accountDao()
        val existing = accountDao
            .getAll(bookId = targetBookId)
            .filterNot { it.isDeleted }
        val map = mutableMapOf<Long, String>()
        assets.forEachIndexed { index, asset ->
            currentCoroutineContext().ensureActive()
            val byName = existing.firstOrNull { it.name == asset.name }
            if (byName != null) {
                map[asset.id] = byName.id
            } else {
                val id = scopedImportId("qja_${asset.id}", targetBookId)
                accountDao.insert(
                    AccountDo(
                        id = id,
                        userId = targetUserId,
                        bookId = targetBookId,
                        iconName = null,
                        name = asset.name,
                        balanceInit = 0L,
                        isExcluded = false,
                        isDefault = index == 0,
                        timeCreate = now,
                        timeModify = now,
                        isDeleted = false,
                        isSync = false,
                    )
                )
                map[asset.id] = id
            }
        }
        return map
    }

    /**
     * 导入钱迹标签。
     *
     * - user_tag.STATUS：1 默认 / 2 归档，0 视为无效
     * - 账单与标签的关联不在独立表，存在 user_bill.EXTRA 的 JSON `"tags": [...]`
     * - 按名字对齐已有标签，没有就用确定性 id `qjl_<钱迹tagId>` 新建
     */
    private suspend fun importLabels(
        qdb: SQLiteDatabase,
        now: Long,
        targetBookId: String,
        targetUserId: String,
    ): Map<String, String> {
        val rows = mutableListOf<Pair<String, String>>() // 钱迹 tagId -> name
        qdb.rawQuery(
            "SELECT TAG_ID, NAME, STATUS FROM user_tag",
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                if (c.getInt(2) == 0) continue
                val tagId = c.getString(0) ?: continue
                val name = c.getString(1).orEmpty().ifBlank { tagId }
                rows += tagId to name
            }
        }
        if (rows.isEmpty()) return emptyMap()

        val labelDao = TallyDb.database.labelDao()
        val nameToId = labelDao
            .getAllUnderBook(bookId = targetBookId)
            .filterNot { it.isDeleted }
            .associate { it.name.orEmpty() to it.id }
            .toMutableMap()

        val map = mutableMapOf<String, String>()
        rows.forEach { (tagId, name) ->
            currentCoroutineContext().ensureActive()
            val existingId = nameToId[name]
            if (existingId != null) {
                map[tagId] = existingId
            } else {
                val id = scopedImportId("qjl_$tagId", targetBookId)
                labelDao.insert(
                    LabelDo(
                        id = id,
                        userId = targetUserId,
                        bookId = targetBookId,
                        name = name,
                        timeCreate = now,
                        timeModify = now,
                        isDeleted = false,
                        isSync = false,
                    )
                )
                nameToId[name] = id
                map[tagId] = id
            }
        }
        return map
    }

    /** 钱迹账单标签：user_bill.EXTRA 里的 {"tags": ["id1", "id2"]} */
    private fun parseExtraTagIds(extra: String?): List<String> {
        if (extra.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONObject(extra).optJSONArray("tags") ?: return emptyList()
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf { it.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    /** 钱迹退款账单的原账单 id：user_bill.EXTRA 里的 "refundsid" */
    private fun parseExtraRefundSource(extra: String?): Long? {
        if (extra.isNullOrBlank()) return null
        return runCatching {
            JSONObject(extra)
                .optLong("refundsid", 0L)
                .takeIf { it > 0L }
        }.getOrNull()
    }

    /** 按名字对齐 yike 已有分类；没有就按钱迹原名新建（保留分类语义） */
    private suspend fun resolveCategory(
        name: String,
        income: Boolean,
        now: Long,
        cache: MutableMap<String, String>,
        targetBookId: String = DEFAULT_BOOK_ID,
        targetUserId: String = LOCAL_USER_ID,
    ): String {
        val type = if (income) "income" else "spending"
        val key = "$type:$name"
        cache[key]?.let { return it }

        val dao = TallyDb.database.categoryDao()
        val categories = dao.getByBookId(targetBookId).filterNot { it.isDeleted }
        val existing = categories.firstOrNull { it.name == name && it.type == type }
            ?: categories.firstOrNull { it.name == name }
        if (existing != null) {
            cache[key] = existing.id
            return existing.id
        }

        val id = scopedImportId("qjc_" + UUID.randomUUID().toString(), targetBookId)
        dao.insert(
            CategoryDo(
                id = id,
                userId = targetUserId,
                bookId = targetBookId,
                parentId = null,
                type = type,
                name = name,
                iconName = null,
                sort = 9000L,
                timeCreate = now,
                timeModify = now,
                isDeleted = false,
                isSync = false,
            )
        )
        cache[key] = id
        return id
    }

    /**
     * 钱迹预算：user_budget 的 TYPE=1 表示按月，TYPE_INFO=yyyy-MM，
     * FLAG=1 总预算 / 2 分类预算，MONEY 单位为元。
     */
    private suspend fun importBudgets(
        qdb: SQLiteDatabase,
        now: Long,
        targetBookId: String,
        targetUserId: String,
    ): Int {
        data class RawBudget(
            val typeInfo: String,
            val flag: Int,
            val amountCents: Long,
            val categoryName: String?,
        )

        val rows = mutableListOf<RawBudget>()
        qdb.rawQuery(
            "SELECT TYPE, TYPE_INFO, FLAG, CATEGORY_ID, MONEY FROM user_budget",
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                if (c.getInt(0) != 1) continue // 只取按月预算
                val typeInfo = c.getString(1).orEmpty()
                if (!typeInfo.matches(Regex("\\d{4}-\\d{2}"))) continue
                val money = c.getDouble(4)
                if (money <= 0) continue
                val flag = c.getInt(2)
                val categoryName = if (flag == BudgetDao.FLAG_CATEGORY && !c.isNull(3)) {
                    queryCategoryName(qdb, c.getLong(3))
                } else null
                rows += RawBudget(
                    typeInfo = typeInfo,
                    flag = flag,
                    amountCents = (money * 100.0).roundToLong(),
                    categoryName = categoryName,
                )
            }
        }
        if (rows.isEmpty()) return 0

        val cache = mutableMapOf<String, String>()
        val budgets = rows.map { raw ->
            currentCoroutineContext().ensureActive()
            val categoryId = if (raw.flag == BudgetDao.FLAG_CATEGORY && !raw.categoryName.isNullOrBlank()) {
                resolveCategory(
                    name = raw.categoryName, income = false, now = now, cache = cache,
                    targetBookId = targetBookId, targetUserId = targetUserId,
                )
            } else null
            BudgetDo(
                id = budgetId(raw.typeInfo, raw.flag, categoryId, targetBookId),
                userId = targetUserId,
                bookId = targetBookId,
                flag = raw.flag,
                categoryId = categoryId,
                typeInfo = raw.typeInfo,
                amount = raw.amountCents,
                timeCreate = now,
                timeModify = now,
            )
        }
        TallyDb.database.budgetDao().insert(budgets)
        return budgets.map { it.id }.toSet().size
    }

    /** 与 BudgetActivity 共用；默认账本保留旧 id，其他账本也使用无歧义命名空间。 */
    internal fun budgetId(typeInfo: String, flag: Int, categoryId: String?, targetBookId: String): String {
        return scopedImportId("qjb_${DEFAULT_BOOK_ID}_${typeInfo}_${flag}_${categoryId ?: "total"}", targetBookId)
    }

    /** 长度前缀避免账本 id 中的分隔符产生歧义；旧账本的源 id 完全保留。 */
    internal fun scopedImportId(legacyId: String, targetBookId: String): String =
        if (targetBookId == DEFAULT_BOOK_ID) legacyId else "qjbook_${targetBookId.length}_${targetBookId}_$legacyId"

    private fun queryCategoryName(qdb: SQLiteDatabase, id: Long): String {
        return qdb.rawQuery("SELECT NAME FROM category WHERE _id = ?", arrayOf(id.toString()))?.use { c ->
            if (c.moveToFirst()) c.getString(0).orEmpty() else ""
        }.orEmpty()
    }

    /** 由无障碍/文件导入写入一条 yike 账单 */
    suspend fun insertBill(
        amountYuan: Double,
        income: Boolean,
        categoryName: String?,
        note: String?,
        timeMs: Long = System.currentTimeMillis(),
    ): BillDo = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        ensureBook(now)
        val type = if (income) "income" else "spending"
        val categoryId = TallyDb.database.categoryDao()
            .getByBookId(DEFAULT_BOOK_ID)
            .let { list ->
                list.firstOrNull { it.name == categoryName && it.type == type }?.id
                    ?: list.firstOrNull { it.type == type }?.id
            }
            ?: resolveCategory(name = categoryName ?: "其他", income = income, now = now, cache = mutableMapOf())
        val amount = (abs(amountYuan) * 100.0).roundToLong()
        val bill = BillDo(
            id = "qj_" + UUID.randomUUID().toString(),
            userId = LOCAL_USER_ID,
            bookId = DEFAULT_BOOK_ID,
            type = "normal",
            originBillId = null,
            time = timeMs,
            categoryId = categoryId,
            accountId = null,
            transferTargetAccountId = null,
            amount = if (income) amount else -amount,
            note = note,
            isNotCalculate = false,
            timeCreate = now,
            timeModify = now,
            isDeleted = false,
            isSync = false,
        )
        TallyDb.database.billDao().insert(bill)
        bill
    }
}
