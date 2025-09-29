package com.xiaojinzi.tally.module.datasource.storage.db.tally

import androidx.room.Room
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.SnowflakeIdGenerator
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.tally.lib.res.LOG_KEYWORDS_TALLY_DATASOURCE
import com.xiaojinzi.tally.module.base.support.DevelopHelper
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.AccountDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillChatDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillImageDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillLabelDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.LabelDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.UserInfoCacheDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.File


object TallyDb {

    const val TAG = "TallyDb"
    const val TAG_DATABASE = "TallyDbDatabase"

    private val snowflakeIdGenerator = SnowflakeIdGenerator(
        // 2024-01-01 00:00:00
        startTimestamp = 1704038400000L,
    )

    fun generateUniqueStr(): String {
        return snowflakeIdGenerator.nextId().toHexString()
    }

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
    )

    // 使用的之前会被初始化
    private var _database: TallyDatabase? = null

    val database: TallyDatabase
        get() = _database ?: throw NullPointerException("数据库未初始化ø")

    fun initDataBase(userId: String) {
        val newDatabaseName = "tally_$userId"
        LogSupport.d(
            tag = TAG_DATABASE,
            content = "initDataBase userId$userId",
            keywords = arrayOf(LOG_KEYWORDS_TALLY_DATASOURCE),
        )
        app.getDatabasePath("tally").run {
            if (this.exists()) {
                this.renameTo(
                    File(this.parentFile, newDatabaseName)
                )
                app.deleteDatabase("tally")
            }
        }
        _database = Room
            .databaseBuilder(
                app,
                TallyDatabase::class.java, newDatabaseName
                // TallyDatabase::class.java, "tally"
            )
            .fallbackToDestructiveMigration()
            .run {
                if (DevelopHelper.isDevelop) {
                    this.setQueryCallback(
                        queryCallback = { sql, bindArgs ->
                            LogSupport.d(
                                tag = TAG,
                                content = "-------------- ttt start--------------",
                            )
                            LogSupport.d(
                                tag = TAG,
                                content = "sql = $sql",
                            )
                            LogSupport.d(
                                tag = TAG,
                                content = "bindArgs = $bindArgs",
                            )
                            LogSupport.d(
                                tag = TAG,
                                content = "-------------- ttt end--------------",
                            )
                        },
                        executor = Dispatchers.IO.asExecutor(),
                    )
                } else {
                    this
                }
            }
            .addMigrations(
                Migration1to2(),
                Migration2to3(),
            )
            .build()
    }

    fun destroyTallyDataBase() {
        LogSupport.d(
            tag = TAG_DATABASE,
            content = "destroyTallyDataBase",
            keywords = arrayOf(LOG_KEYWORDS_TALLY_DATASOURCE),
        )
        _database = Room
            .inMemoryDatabaseBuilder(
                app,
                TallyDatabase::class.java,
            )
            .fallbackToDestructiveMigration()
            .run {
                if (DevelopHelper.isDevelop) {
                    this.setQueryCallback(
                        queryCallback = { sql, bindArgs ->
                            LogSupport.d(
                                tag = TAG,
                                content = "-------------- ttt start--------------",
                            )
                            LogSupport.d(
                                tag = TAG,
                                content = "sql = $sql",
                            )
                            LogSupport.d(
                                tag = TAG,
                                content = "bindArgs = $bindArgs",
                            )
                            LogSupport.d(
                                tag = TAG,
                                content = "-------------- ttt end--------------",
                            )
                        },
                        executor = Dispatchers.IO.asExecutor(),
                    )
                } else {
                    this
                }
            }
            .build()
    }

}
