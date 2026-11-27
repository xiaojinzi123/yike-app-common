package com.xiaojinzi.tally.module.datasource.storage.db.tally

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.AccountDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.AccountDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillChatDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillChatDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillImageDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillImageDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillLabelDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillLabelDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.LabelDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.LabelDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.UserInfoCacheDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.UserInfoCacheDo

/**
 * 所有数据的查询,
 */
@Database(
    entities = [
        UserInfoCacheDo::class,
        BookDo::class,
        CategoryDo::class,
        AccountDo::class,
        LabelDo::class,
        BillLabelDo::class,
        BillImageDo::class,
        BillChatDo::class,
        BillDo::class,
        BudgetDo::class,
    ],
    views = [
    ],
    version = 4,
    exportSchema = false,
)
abstract class TallyDatabase : RoomDatabase() {

    abstract fun userInfoCacheDao(): UserInfoCacheDao

    abstract fun bookDao(): BookDao

    abstract fun categoryDao(): CategoryDao

    abstract fun accountDao(): AccountDao

    abstract fun labelDao(): LabelDao

    abstract fun billLabelDao(): BillLabelDao

    abstract fun billImageDao(): BillImageDao

    abstract fun billChatDao(): BillChatDao

    abstract fun billDao(): BillDao

    abstract fun budgetDao(): BudgetDao

}