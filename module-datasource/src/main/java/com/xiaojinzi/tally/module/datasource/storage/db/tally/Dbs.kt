package com.xiaojinzi.tally.module.datasource.storage.db.tally

import androidx.room.Room
import com.xiaojinzi.support.ktx.SnowflakeIdGenerator
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.AccountDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillChatDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillImageDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillLabelDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.LabelDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.UserInfoCacheDao

object TallyDb {

    const val TAG = "TallyDb"
    const val TAG_DATABASE = "TallyDbDatabase"

    private val snowflakeIdGenerator = SnowflakeIdGenerator(
        startTimestamp = 1704038400000L,
    )

    fun generateUniqueStr(): String = snowflakeIdGenerator.nextId().toHexString()

    val totalTableNames = listOf(
        UserInfoCacheDao.TableName,
        BookDao.TableName,
        CategoryDao.TableName,
        AccountDao.TableName,
        LabelDao.TableName,
        BillDao.TableName,
        BillImageDao.TableName,
        BillLabelDao.TableName,
        BillChatDao.TableName,
        BudgetDao.TableName,
    )

    @Volatile
    private var localDatabase: TallyDatabase? = null

    val database: TallyDatabase
        get() = localDatabase ?: error("本地账本尚未初始化")

    @Synchronized
    fun initDataBase(userId: String) {
        if (localDatabase != null) return
        require(userId.isNotBlank() && userId.none { it == '/' || it == '\\' || it == '\u0000' }) {
            "本地账本标识无效"
        }
        val ownerDatabaseName = "tally_$userId"
        // 直接打开既有文件，保留旧 tally 库及 WAL；不重命名、覆盖或删除旧库。
        val databaseName = when {
            app.getDatabasePath(ownerDatabaseName).exists() -> ownerDatabaseName
            app.getDatabasePath("tally").exists() -> "tally"
            else -> ownerDatabaseName
        }
        localDatabase = Room.databaseBuilder(app, TallyDatabase::class.java, databaseName)
            .addMigrations(Migration1to2(), Migration2to3(), Migration3to4())
            .build()
    }
}
