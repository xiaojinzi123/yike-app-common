package com.xiaojinzi.tally.module.qianji

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToLong

/**
 * B 方案：把钱迹的数据与规则移植到 yike。
 *
 * 不依赖钱迹 apk/dex，只依赖：
 * 1) 钱迹原 SQLite 文件 qianjiapp（greenDAO 表结构）
 * 2) yike 自己的 Room TallyDatabase
 */
object QianjiBridge {

    const val LOCAL_USER_ID = "opensourceUserId"
    const val DEFAULT_BOOK_ID = "1"
    const val DEFAULT_CATEGORY_ID = "qj_other"

    /** 从本机钱迹 data 目录或指定文件导入 */
    suspend fun importQianjiDatabase(
        context: Context,
        source: File,
    ): Int = withContext(Dispatchers.IO) {
        require(source.exists() && source.length() > 0) { "钱迹数据库不存在或为空: $source" }
        val db = SQLiteDatabase.openDatabase(source.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val rows = mutableListOf<BillDo>()
        val now = System.currentTimeMillis()
        db.use { qdb ->
            ensureBaseData(now)
            val categoryMap = TallyDb.database.categoryDao()
                .getByBookId(DEFAULT_BOOK_ID)
                .associateBy { it.name.orEmpty() }
            val fallback = categoryMap.values.firstOrNull { it.type == "spending" }?.id
                ?: DEFAULT_CATEGORY_ID

            qdb.rawQuery(
                """
                SELECT billid, TIME, TYPE, MONEY, CATEGORY_ID, REMARK, STATUS, bookid
                FROM user_bill
                """.trimIndent(), null
            )?.use { c ->
                while (c.moveToNext()) {
                    val status = c.getInt(6)
                    if (status == 0) continue // 钱迹软删
                    val rawMoney = c.getDouble(3)
                    val money = kotlin.math.abs(rawMoney)
                    val amountCent = (money * 100.0).roundToLong()
                    val type = c.getInt(2)
                    val timeSec = c.getLong(1)
                    val timeMs = timeSec * 1000L
                    val qjCateId = c.getLong(4)
                    val categoryName = queryQianjiCategoryName(qdb, qjCateId)
                    val categoryId = categoryMap[categoryName]?.id ?: fallback
                    val billId = c.getLong(0)
                    val note = c.getString(5).orEmpty().ifBlank { null }
                    rows += BillDo(
                        id = "qj_$billId",
                        userId = LOCAL_USER_ID,
                        bookId = DEFAULT_BOOK_ID,
                        type = "normal",
                        originBillId = null,
                        time = timeMs,
                        categoryId = categoryId,
                        accountId = null,
                        transferTargetAccountId = null,
                        amount = if (type == 1) amountCent else -amountCent,
                        note = note,
                        isNotCalculate = false,
                        timeCreate = now,
                        timeModify = timeMs,
                        isDeleted = false,
                        isSync = false,
                    )
                }
            }
        }
        if (rows.isNotEmpty()) TallyDb.database.billDao().insert(rows)
        rows.size
    }

    private suspend fun ensureBaseData(now: Long) {
        val dao = TallyDb.database
        dao.bookDao().insert(
            BookDo(
                id = DEFAULT_BOOK_ID,
                userId = LOCAL_USER_ID,
                isSystem = true,
                type = "normal",
                name = "钱迹导入账本",
                iconName = "book1",
                timeCreate = now,
                timeModify = now,
            )
        )
        val categoryDao = dao.categoryDao()
        val exists = categoryDao.getByBookId(DEFAULT_BOOK_ID).any { it.id == DEFAULT_CATEGORY_ID }
        if (!exists) {
            categoryDao.insert(
                listOf(
                    category("qj_food", "餐饮", "spending", 0, now),
                    category("qj_transport", "交通", "spending", 1, now),
                    category("qj_shopping", "购物", "spending", 2, now),
                    category("qj_daily", "日用", "spending", 3, now),
                    category("qj_other", "其他", "spending", 99, now),
                    category("qj_salary", "工资", "income", 100, now),
                )
            )
        }
    }

    private fun category(id: String, name: String, type: String, sort: Int, now: Long) = CategoryDo(
        id = id,
        userId = LOCAL_USER_ID,
        bookId = DEFAULT_BOOK_ID,
        parentId = null,
        type = type,
        name = name,
        iconName = null,
        sort = sort.toLong(),
        timeCreate = now,
        timeModify = now,
        isDeleted = false,
        isSync = false,
    )

    private fun queryQianjiCategoryName(qdb: SQLiteDatabase, id: Long): String {
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
        ensureBaseData(System.currentTimeMillis())
        val categoryId = TallyDb.database.categoryDao()
            .getByBookId(DEFAULT_BOOK_ID)
            .firstOrNull { it.name == categoryName && it.type == if (income) "income" else "spending" }
            ?.id
            ?: DEFAULT_CATEGORY_ID
        val amount = (kotlin.math.abs(amountYuan) * 100.0).roundToLong()
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
            timeCreate = System.currentTimeMillis(),
            timeModify = System.currentTimeMillis(),
            isDeleted = false,
            isSync = false,
        )
        TallyDb.database.billDao().insert(bill)
        bill
    }
}
