package com.xiaojinzi.tally.module.datasource.storage.db.tally

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration1to2 : Migration(
    startVersion = 1,
    endVersion = 2,
) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(sql = "ALTER TABLE `account` ADD COLUMN `isDefault` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS `bill_chat` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `state` TEXT NOT NULL, `content` TEXT, `bookId` TEXT, `billId` TEXT, `timeCreated` INTEGER NOT NULL)");
    }

}

class Migration2to3 : Migration(
    startVersion = 2,
    endVersion = 3,
) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(sql = "ALTER TABLE `bill` ADD COLUMN `originBillId` TEXT")
    }

}

/** 3 -> 4：新增钱迹预算表 */
class Migration3to4 : Migration(
    startVersion = 3,
    endVersion = 4,
) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            sql = "CREATE TABLE IF NOT EXISTS `budget` (" +
                    "`id` TEXT NOT NULL, " +
                    "`userId` TEXT NOT NULL, " +
                    "`bookId` TEXT NOT NULL, " +
                    "`flag` INTEGER NOT NULL, " +
                    "`categoryId` TEXT, " +
                    "`typeInfo` TEXT NOT NULL, " +
                    "`amount` INTEGER NOT NULL, " +
                    "`timeCreate` INTEGER NOT NULL, " +
                    "`timeModify` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`isSync` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            sql = "CREATE INDEX IF NOT EXISTS `index_budget_bookId_typeInfo` ON `budget` (`bookId`, `typeInfo`)"
        )
    }

}