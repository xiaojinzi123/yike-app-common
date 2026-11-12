package com.xiaojinzi.tally.module.qianji

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.AccountDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * 数据落点固定为 yike 的默认账本（id = 1）：
 * - 不覆盖账本自身的名称/图标（只在账本不存在时创建）
 * - 钱迹分类按名字对齐 yike 分类，缺失的按原名新建，不塞种子分类
 */
object QianjiBridge {

    const val LOCAL_USER_ID = "opensourceUserId"
    const val DEFAULT_BOOK_ID = "1"
    const val DEFAULT_CATEGORY_ID = "qj_other"

    private const val DEFAULT_BOOK_NAME = "默认账本"

    /** 从本机钱迹 data 目录或指定文件导入（幂等：重复导入按 id 覆盖） */
    suspend fun importQianjiDatabase(
        context: Context,
        source: File,
    ): ImportResult = withContext(Dispatchers.IO) {
        require(source.exists() && source.length() > 0) { "钱迹数据库不存在或为空: $source" }
        val db = SQLiteDatabase.openDatabase(source.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val now = System.currentTimeMillis()
        var billCount = 0
        var budgetCount = 0
        db.use { qdb ->
            ensureBook(now)
            val accountMap = importAccounts(qdb, now)
            val categoryCache = mutableMapOf<String, String>()
            val bills = mutableListOf<BillDo>()

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
            )

            val rawBills = mutableListOf<RawBill>()
            qdb.rawQuery(
                """
                SELECT billid, TIME, TYPE, MONEY, CATEGORY_ID, REMARK, STATUS, ASSETID, FROMID, TARGETID
                FROM user_bill
                """.trimIndent(), null
            )?.use { c ->
                while (c.moveToNext()) {
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
                    )
                }
            }

            rawBills.forEach { raw ->
                val isTransfer = raw.type == 2
                val income = raw.type == 1
                val categoryId = if (isTransfer) {
                    null
                } else {
                    resolveCategory(
                        name = raw.categoryName.ifBlank { "其他" },
                        income = income,
                        now = now,
                        cache = categoryCache,
                    )
                }
                bills += BillDo(
                    id = "qj_${raw.id}",
                    userId = LOCAL_USER_ID,
                    bookId = DEFAULT_BOOK_ID,
                    type = if (isTransfer) "transfer" else "normal",
                    originBillId = null,
                    time = raw.timeMs,
                    categoryId = categoryId,
                    accountId = raw.assetId?.let { accountMap[it] }
                        ?: raw.fromId?.let { accountMap[it] },
                    transferTargetAccountId = if (isTransfer) raw.targetId?.let { accountMap[it] } else null,
                    // 转账是资金流出源账户，统一记负数
                    amount = if (isTransfer || !income) -raw.amountCent else raw.amountCent,
                    note = raw.note,
                    isNotCalculate = false,
                    timeCreate = now,
                    timeModify = now,
                    isDeleted = false,
                    isSync = false,
                )
            }
            if (bills.isNotEmpty()) TallyDb.database.billDao().insert(bills)
            billCount = bills.size

            budgetCount = importBudgets(qdb, now)
        }
        ImportResult(billCount = billCount, budgetCount = budgetCount)
    }

    data class ImportResult(
        val billCount: Int,
        val budgetCount: Int,
    )

    /** 只在账本不存在时创建，绝不覆盖现有账本 */
    private suspend fun ensureBook(now: Long) {
        val bookDao = TallyDb.database.bookDao()
        if (bookDao.getById(DEFAULT_BOOK_ID) != null) return
        bookDao.insert(
            BookDo(
                id = DEFAULT_BOOK_ID,
                userId = LOCAL_USER_ID,
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
    private suspend fun importAccounts(qdb: SQLiteDatabase, now: Long): Map<Long, String> {
        data class RawAsset(val id: Long, val name: String)

        val assets = mutableListOf<RawAsset>()
        qdb.rawQuery(
            "SELECT _ID, NAME FROM user_asset WHERE STATUS = 0",
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                assets += RawAsset(
                    id = c.getLong(0),
                    name = c.getString(1).orEmpty().ifBlank { "账户${c.getLong(0)}" },
                )
            }
        }
        if (assets.isEmpty()) return emptyMap()

        val accountDao = TallyDb.database.accountDao()
        val existing = accountDao
            .getAll(bookId = DEFAULT_BOOK_ID)
            .filterNot { it.isDeleted }
        val map = mutableMapOf<Long, String>()
        assets.forEachIndexed { index, asset ->
            val byName = existing.firstOrNull { it.name == asset.name }
            if (byName != null) {
                map[asset.id] = byName.id
            } else {
                val id = "qja_${asset.id}"
                accountDao.insert(
                    AccountDo(
                        id = id,
                        userId = LOCAL_USER_ID,
                        bookId = DEFAULT_BOOK_ID,
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

    /** 按名字对齐 yike 已有分类；没有就按钱迹原名新建（保留分类语义） */
    private suspend fun resolveCategory(
        name: String,
        income: Boolean,
        now: Long,
        cache: MutableMap<String, String>,
    ): String {
        val type = if (income) "income" else "spending"
        val key = "$type:$name"
        cache[key]?.let { return it }

        val dao = TallyDb.database.categoryDao()
        val existing = dao.getByBookId(DEFAULT_BOOK_ID)
            .firstOrNull { it.name == name && it.type == type }
            ?: dao.getByBookId(DEFAULT_BOOK_ID).firstOrNull { it.name == name }
        if (existing != null) {
            cache[key] = existing.id
            return existing.id
        }

        val id = "qjc_" + UUID.randomUUID().toString()
        dao.insert(
            CategoryDo(
                id = id,
                userId = LOCAL_USER_ID,
                bookId = DEFAULT_BOOK_ID,
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
    private suspend fun importBudgets(qdb: SQLiteDatabase, now: Long): Int {
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
            val categoryId = if (raw.flag == BudgetDao.FLAG_CATEGORY && !raw.categoryName.isNullOrBlank()) {
                resolveCategory(name = raw.categoryName, income = false, now = now, cache = cache)
            } else null
            BudgetDo(
                id = budgetId(raw.typeInfo, raw.flag, categoryId),
                userId = LOCAL_USER_ID,
                bookId = DEFAULT_BOOK_ID,
                flag = raw.flag,
                categoryId = categoryId,
                typeInfo = raw.typeInfo,
                amount = raw.amountCents,
                timeCreate = now,
                timeModify = now,
            )
        }
        TallyDb.database.budgetDao().insert(budgets)
        return budgets.size
    }

    /** 与 BudgetActivity 保持一致的确定性 id，重复导入按覆盖处理 */
    private fun budgetId(typeInfo: String, flag: Int, categoryId: String?): String {
        return "qjb_${DEFAULT_BOOK_ID}_${typeInfo}_${flag}_${categoryId ?: "total"}"
    }

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
