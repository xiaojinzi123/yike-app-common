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
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

@Keep
@Entity(
    tableName = BillImageDao.TableName,
    indices = [
        Index(
            value = ["id"],
            unique = true,
        ),
        Index(
            name = "billId_url",
            value = ["billId", "url"],
            unique = true,
        ),
    ]
)
data class BillImageDo(
    // 全宇宙唯一的 string
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "userId") val userId: String,
    // 所属的账本 ID
    @ColumnInfo(name = "bookId") val bookId: String,
    // 所属的账单
    @ColumnInfo(name = "billId") val billId: String,
    // 标签的名字
    @ColumnInfo(name = "url") val url: String?,
    // 修改的时间
    @ColumnInfo(name = "timeModify") override val timeModify: Long?,
    // 修改的时间的格式化, 就是为了便于开发测试, 实际上没啥用处
    @ColumnInfo(name = "timeModifyFormat") val timeModifyFormat: String? = timeModify?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(java.util.Date(it))
    },
    // 是否被删除了
    @ColumnInfo(name = "isDeleted") override val isDeleted: Boolean,
    // 是否同步了
    @ColumnInfo(name = "isSync") override val isSync: Boolean,
) : SyncTableInter

/**
 * 所有查询不过滤被删除的记录
 * 业务方自己过滤
 * 但是都需要根据 userId 和 bookId 来查
 */
@Dao
interface BillImageDao {

    companion object {
        const val TableName = "bill_image"
        const val SelectAll = "SELECT * FROM $TableName"
    }

    /**
     * 插入数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(target: BillImageDo)

    /**
     * 插入数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(targetList: List<BillImageDo>)

    /**
     * 更新数据
     */
    @Update
    suspend fun update(target: BillImageDo)

    /**
     * 更新数据
     */
    @Update
    suspend fun update(targetList: List<BillImageDo>)

    @Query("DELETE FROM $TableName WHERE bookId = :bookId")
    suspend fun deleteAllByBookId(bookId: String)

    /**
     * 根据 bookId 获取数据
     */
    @Query(value = "select * from $TableName where bookId = :bookId")
    suspend fun getListByBookId(
        bookId: String,
    ): List<BillImageDo>

    /**
     * 根据 billId 获取数据
     */
    @Query(value = "select * from $TableName where bookId = :bookId and billId = :billId")
    suspend fun getListByBookIdAndBillId(
        bookId: String,
        billId: String,
    ): List<BillImageDo>

    /**
     * 根据 billId 获取数据
     */
    @Query(value = "select t.billId from $TableName t")
    suspend fun getAllBillId(): List<String>

    /**
     * 根据 billId 获取数据
     */
    @Query(value = "select t.billId from $TableName t where t.bookId = :bookId")
    suspend fun getAllBillIdByBookId(
        bookId: String,
    ): List<String>

    /**
     * 根据 billId 获取数据
     */
    @Query(value = "select t.billId from $TableName t where t.isDeleted = 0")
    suspend fun getAllNoDeletedBillId(): List<String>

    /**
     * 根据 billId 获取数据
     */
    @Query(value = "select t.billId from $TableName t where t.bookId = :bookId and t.isDeleted = 0")
    suspend fun getAllNoDeletedBillIdByBookId(
        bookId: String,
    ): List<String>

    /**
     * 根据 timeModify 倒序排列, 获取最新一条数据
     */
    @Query(value = "select * from $TableName where bookId = :bookId order by timeModify desc limit 0, 1")
    suspend fun getLatestOneImageOrderByTimeModify(
        bookId: String,
    ): BillImageDo?

    /**
     * 根据 Id 获取数据
     */
    @Query(value = "select * from $TableName where id=:id and bookId = :bookId")
    suspend fun getById(
        id: String,
        bookId: String,
    ): BillImageDo?

    @Query(value = "select * from $TableName where id in (:idList) and bookId = :bookId")
    suspend fun getByIdList(
        idList: List<String>,
        bookId: String,
    ): List<BillImageDo>

    @Query(value = "select * from $TableName where bookId = :bookId and isSync = 0 limit 0, :pageSize")
    suspend fun getUnSyncList(
        bookId: String,
        pageSize: Int,
    ): List<BillImageDo>

    @Query(value = "select * from $TableName where bookId = :bookId order by timeModify desc limit 0, 1")
    suspend fun getLatestOneOrderByTimeModify(
        bookId: String,
    ): BillImageDo?

    /**
     * 所有数据
     */
    @Query(value = "SELECT * FROM $TableName where bookId = :bookId")
    fun subscribeAllImage(
        bookId: String,
    ): Flow<List<BillImageDo>>

    /**
     * 订阅没同步的数量
     * 包括已经删除的
     */
    @Query("select COUNT(t.id) from ${AccountDao.TableName} t where t.isSync = 0")
    fun subscribeUnSyncCount(): Flow<Long>

}
