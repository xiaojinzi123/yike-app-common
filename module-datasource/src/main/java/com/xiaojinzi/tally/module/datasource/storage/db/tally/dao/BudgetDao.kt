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

    @Query("SELECT * FROM $TableName WHERE bookId = :bookId AND typeInfo = :typeInfo AND flag = $FLAG_TOTAL AND isDeleted = 0 LIMIT 1")
    suspend fun getTotal(bookId: String, typeInfo: String): BudgetDo?

    @Query("SELECT * FROM $TableName WHERE bookId = :bookId AND typeInfo = :typeInfo AND flag = $FLAG_TOTAL AND isDeleted = 0 LIMIT 1")
    fun subscribeTotal(bookId: String, typeInfo: String): kotlinx.coroutines.flow.Flow<BudgetDo?>

    @Query("SELECT * FROM $TableName WHERE bookId = :bookId AND typeInfo = :typeInfo AND flag = $FLAG_CATEGORY AND isDeleted = 0")
    suspend fun getCategoryBudgets(bookId: String, typeInfo: String): List<BudgetDo>

    @Query("DELETE FROM $TableName WHERE id = :id")
    suspend fun deleteById(id: String)
}
