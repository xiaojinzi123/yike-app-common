package com.xiaojinzi.tally.module.datasource.storage.db.tally.dao

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xiaojinzi.tally.lib.res.model.tally.BillChatDto

@Keep
@Entity(
    tableName = BillChatDao.TableName,
)
data class BillChatDo(
    // 主键 ID
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null,
    @ColumnInfo(name = "state")
    val state: String,
    // 输入的值
    @ColumnInfo(name = "content")
    val content: String? = null,
    @ColumnInfo(name = "bookId")
    val bookId: String? = null,
    @ColumnInfo(name = "billId")
    val billId: String? = null,
    @ColumnInfo(name = "timeCreated")
    val timeCreated: Long,
)

@Dao
interface BillChatDao {
    companion object {
        const val TableName = "bill_chat"
    }

    @Insert
    suspend fun insert(
        target: BillChatDo,
    ): Long

    @Update
    suspend fun update(
        target: BillChatDo,
    )

    @Transaction
    @Query("UPDATE $TableName SET state = '${BillChatDto.STATE_FAIL}' WHERE state = '${BillChatDto.STATE_INIT}'")
    suspend fun updateAllInitToFail()

    @Query("SELECT * FROM $TableName WHERE id = :id")
    suspend fun getById(
        id: Long,
    ): BillChatDo?

    @Transaction
    @Query("DELETE FROM $TableName WHERE id = :id")
    suspend fun deleteById(
        id: Long,
    )

    @Query("DELETE FROM $TableName WHERE bookId = :bookId")
    suspend fun deleteAllByBookId(bookId: String)

    @Query("SELECT timeCreated FROM $TableName WHERE bookId = :bookId ORDER BY timeCreated DESC LIMIT :pageStartIndex, :pageSize")
    suspend fun getTimeList(
        bookId: String,
        pageStartIndex: Long,
        pageSize: Long,
    ): List<Long>

    @Query("SELECT * FROM $TableName WHERE bookId = :bookId AND timeCreated >= :afterTime ORDER BY timeCreated DESC")
    suspend fun getListAfterTimeByBookId(
        bookId: String,
        afterTime: Long,
    ): List<BillChatDo>

}