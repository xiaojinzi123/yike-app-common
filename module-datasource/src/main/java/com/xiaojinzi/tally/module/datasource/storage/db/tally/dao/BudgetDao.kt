package com.xiaojinzi.tally.module.datasource.storage.db.tally.dao

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

/**
 * 预算：移植自钱迹的 user_budget 表。
 *
 * 钱迹口径：flag=1 总预算，flag=2 分类预算；type=1 按月，typeInfo = yyyy-MM。
 * 金额单位与 BillDo 一致，都是「分」。
 */
@Keep
@Entity(
    tableName = BudgetDao.TableName,
    indices = [
        Index(
            value = ["bookId", "typeInfo"],
        ),
    ],
)
data class BudgetDo(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "bookId") val bookId: String,
    // 1 = 总预算, 2 = 分类预算
    @ColumnInfo(name = "flag") val flag: Int,
    // flag = 2 时有效
    @ColumnInfo(name = "categoryId") val categoryId: String?,
    // 预算周期，固定 yyyy-MM
    @ColumnInfo(name = "typeInfo") val typeInfo: String,
    // 预算金额（分）
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "timeCreate") val timeCreate: Long,
    @ColumnInfo(name = "timeModify") val timeModify: Long,
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "isSync") val isSync: Boolean = false,
)

@Keep
@Dao
interface BudgetDao {

    companion object {
        const val TableName = "budget"
        const val FLAG_TOTAL = 1
        const val FLAG_CATEGORY = 2
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(target: BudgetDo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(targetList: List<BudgetDo>)

    @Query("SELECT * FROM $TableName WHERE bookId = :bookId AND typeInfo = :typeInfo AND isDeleted = 0 ORDER BY flag, amount DESC")
    suspend fun getByBookAndMonth(bookId: String, typeInfo: String): List<BudgetDo>

    @Query("SELECT * FROM $TableName WHERE bookId = :bookId AND typeInfo = :typeInfo AND flag = $FLAG_TOTAL AND isDeleted = 0 ORDER BY timeModify DESC, timeCreate DESC, id DESC LIMIT 1")
    suspend fun getTotal(bookId: String, typeInfo: String): BudgetDo?

    @Query("SELECT * FROM $TableName WHERE bookId = :bookId AND typeInfo = :typeInfo AND flag = $FLAG_TOTAL AND isDeleted = 0 ORDER BY timeModify DESC, timeCreate DESC, id DESC LIMIT 1")
    fun subscribeTotal(bookId: String, typeInfo: String): kotlinx.coroutines.flow.Flow<BudgetDo?>

    @Query("SELECT b.* FROM $TableName b WHERE b.bookId = :bookId AND b.typeInfo = :typeInfo AND b.flag = $FLAG_CATEGORY AND b.isDeleted = 0 AND b.categoryId IS NOT NULL AND b.id = (SELECT latest.id FROM $TableName latest WHERE latest.bookId = b.bookId AND latest.typeInfo = b.typeInfo AND latest.flag = b.flag AND latest.categoryId = b.categoryId AND latest.isDeleted = 0 ORDER BY latest.timeModify DESC, latest.timeCreate DESC, latest.id DESC LIMIT 1) ORDER BY b.amount DESC, b.categoryId")
    suspend fun getCategoryBudgets(bookId: String, typeInfo: String): List<BudgetDo>

    @Query("SELECT b.* FROM $TableName b WHERE b.bookId = :bookId AND b.typeInfo = :typeInfo AND b.flag = $FLAG_CATEGORY AND b.isDeleted = 0 AND b.categoryId IS NOT NULL AND b.id = (SELECT latest.id FROM $TableName latest WHERE latest.bookId = b.bookId AND latest.typeInfo = b.typeInfo AND latest.flag = b.flag AND latest.categoryId = b.categoryId AND latest.isDeleted = 0 ORDER BY latest.timeModify DESC, latest.timeCreate DESC, latest.id DESC LIMIT 1) ORDER BY b.amount DESC, b.categoryId")
    fun subscribeCategoryBudgets(bookId: String, typeInfo: String): kotlinx.coroutines.flow.Flow<List<BudgetDo>>

    /** 保存一个月份额度时合并同范围旧记录，保留传入记录的本地所有权。 */
    @Transaction
    suspend fun saveMonthBudget(target: BudgetDo) {
        require(target.amount > 0 && !target.isDeleted)
        require(
            (target.flag == FLAG_TOTAL && target.categoryId == null) ||
                (target.flag == FLAG_CATEGORY && !target.categoryId.isNullOrBlank()),
        )
        deleteMonthBudget(target.bookId, target.typeInfo, target.flag, target.categoryId)
        insert(target)
    }

    @Query("DELETE FROM $TableName WHERE bookId = :bookId AND typeInfo = :typeInfo AND flag = :flag AND (:flag = $FLAG_TOTAL OR categoryId = :categoryId)")
    suspend fun deleteMonthBudget(bookId: String, typeInfo: String, flag: Int, categoryId: String?)

    @Query("DELETE FROM $TableName WHERE bookId = :bookId")
    suspend fun deleteAllByBookId(bookId: String)

    @Query("DELETE FROM $TableName WHERE id = :id")
    suspend fun deleteById(id: String)
}
