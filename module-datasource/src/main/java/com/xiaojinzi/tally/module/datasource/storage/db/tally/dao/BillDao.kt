package com.xiaojinzi.tally.module.datasource.storage.db.tally.dao

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.toOneOrZero
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Keep
@Entity(
    tableName = BillDao.TableName,
    indices = [
        Index(
            value = ["id"],
            unique = true,
        ),
    ]
)
data class BillDo(
    // 全宇宙唯一的 string
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "userId") val userId: String,
    // 所属的账本 ID
    @ColumnInfo(name = "bookId") val bookId: String,
    // 账单的类型: 普通账单: "normal", 转账: "transfer", 退款: "refund"
    @ColumnInfo(name = "type") val type: String,
    // 原账单 Id, 目前只有退款类型的账单会存在
    @ColumnInfo(name = "originBillId") val originBillId: String?,
    // 账单的时间, 时间戳
    @ColumnInfo(name = "time", index = true) val time: Long,
    // 所属的类别的 id, 有些类型是没有这个类别字段的
    @ColumnInfo(name = "categoryId") val categoryId: String?,
    // 账户的 ID, 付款的账户
    @ColumnInfo(name = "accountId") val accountId: String? = null,
    // 目标账户, 当类型是转账的时候, 这个会有值, 但是不保证这个值对应的账户一定存在
    @ColumnInfo(name = "transferTargetAccountId") val transferTargetAccountId: String?,
    // 消耗的金钱. 值可能有正有负. 存的值是计量单位的 100 倍
    // 钱单位是分
    @ColumnInfo(name = "amount") val amount: Long,
    // 备注
    @ColumnInfo(name = "note") val note: String? = null,
    // 是否不计入收支
    @ColumnInfo(
        name = "isNotCalculate",
        defaultValue = "0",
    ) val isNotCalculate: Boolean = false,
    // 创建的时间
    @ColumnInfo(name = "timeCreate") override val timeCreate: Long,
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
) : SyncTableInter, SyncTableTimeCreateFieldInter

@Keep
data class BillDetailDo(
    @Embedded
    val bill: BillDo,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id",
        entity = UserInfoCacheDo::class,
    )
    val user: UserInfoCacheDo?,
    @Relation(
        parentColumn = "bookId",
        entityColumn = "id",
        entity = BookDo::class,
    )
    val book: BookDo?,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id",
        entity = AccountDo::class,
    )
    val account: AccountDo?,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id",
        entity = CategoryDo::class,
    )
    val category: CategoryDo?,
    @Relation(
        parentColumn = "transferTargetAccountId",
        entityColumn = "id",
        entity = AccountDo::class,
    )
    val transferTargetAccount: AccountDo?,
    /*@Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BillLabelDo::class,
            parentColumn = "billId",
            entityColumn = "labelId",
        ),
    )
    val labelList: List<LabelDo>,*/
)

@Dao
interface BillDao {

    companion object {
        const val TableName = "bill"
    }

    /**
     * 根据 id 获取数据
     */
    @Transaction
    @Query("SELECT * FROM $TableName where id = :id")
    suspend fun getById(
        id: String,
    ): BillDetailDo?

    @Query(value = "select * from $TableName where bookId = :bookId order by timeModify desc limit 0, 1")
    suspend fun getLatestOneOrderByTimeModify(
        bookId: String,
    ): BillDo?

    @Query(value = "select * from $TableName where bookId = :bookId and isSync = 0 limit 0, :pageSize")
    suspend fun getUnSyncList(
        bookId: String,
        pageSize: Int,
    ): List<BillDo>

    /**
     * 某时间段的支出合计（金额为负，返回负数），用于预算统计
     */
    @Query(value = "select coalesce(sum(amount), 0) from $TableName where bookId = :bookId and isDeleted = 0 and amount < 0 and time >= :start and time < :end")
    suspend fun sumExpense(
        bookId: String,
        start: Long,
        end: Long,
    ): Long

    /**
     * 某时间段、某分类的支出合计（返回负数），用于分类预算
     */
    @Query(value = "select coalesce(sum(amount), 0) from $TableName where bookId = :bookId and categoryId = :categoryId and isDeleted = 0 and amount < 0 and time >= :start and time < :end")
    suspend fun sumExpenseByCategory(
        bookId: String,
        categoryId: String,
        start: Long,
        end: Long,
    ): Long

    /**
     * 插入数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(target: BillDo)

    /**
     * 插入数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(targetList: List<BillDo>)

    /**
     * 插入数据
     */
    @Update
    suspend fun update(target: BillDo)

    /**
     * 更新数据
     */
    @Transaction
    @Query("update $TableName set categoryId = :categoryId where originBillId = :originBillId")
    suspend fun updateSubRefundBill(
        originBillId: String,
        categoryId: String?,
    )

    /**
     * 更新数据
     */
    @Transaction
    @Query("update $TableName set categoryId = :categoryId, isDeleted = :isDeleted where originBillId = :originBillId and isDeleted = 0")
    suspend fun updateSubRefundBill(
        originBillId: String,
        categoryId: String?,
        isDeleted: Boolean,
    )

    /**
     * 删除所有有关某个 BookId 的数据
     */
    @Transaction
    @Query("DELETE FROM $TableName WHERE bookId = :bookId")
    suspend fun deleteAllByBookId(bookId: String)

    /**
     * 查询所有的时间, 然后按照每天分组, 然后分页
     */
    @RawQuery
    suspend fun queryDayTimeListByCondition(
        rawSqlQuery: SupportBillDetailPageQueryImpl,
    ): List<Long>

    /**
     * 查询所有的时间, 然后按照每天分组, 然后分页
     */
    @RawQuery(
        observedEntities = [
            BillDo::class,
            BillDetailDo::class,
        ],
    )
    fun subscribeDayTimeListByCondition(
        rawSqlQuery: SupportBillDetailPageQueryImpl,
    ): Flow<List<Long>>

    /**
     * 查询账单
     */
    @RawQuery
    suspend fun queryAmountList(
        rawSqlQuery: SupportBillDetailPageQueryImpl,
    ): List<Long>

    @RawQuery
    suspend fun queryCount(
        rawSqlQuery: SupportBillDetailPageQueryImpl,
    ): Long

    /**
     * 查询账单
     */
    @RawQuery
    suspend fun queryBillList(
        rawSqlQuery: SupportBillDetailPageQueryImpl,
    ): List<BillDo>

    /**
     * 查询账单详情
     */
    @RawQuery
    @Transaction
    suspend fun queryBillDetailList(
        rawSqlQuery: SupportBillDetailPageQueryImpl,
    ): List<BillDetailDo>

    /**
     * Flow 查询账单详情列表
     */
    @RawQuery(
        observedEntities = [
            LabelDo::class,
            CategoryDo::class,
            AccountDo::class,
            BillLabelDo::class,
            BillImageDo::class,
            BillDo::class,
            BillDetailDo::class,
        ],
    )
    fun subscribeBillDetailList(
        rawSqlQuery: SupportBillDetailPageQueryImpl,
    ): Flow<List<BillDetailDo>>

    /**
     * Flow 查询账单详情
     */
    @RawQuery(
        observedEntities = [
            BillDo::class,
        ],
    )
    fun subscribeBillCount(
        rawSqlQuery: SupportBillDetailPageQueryImpl,
    ): Flow<Long>

    /**
     * 订阅同步成功的数量
     * 包括已经删除的
     */
    @Query("select COUNT(b.id) from $TableName b where b.isSync = 1")
    fun subscribeBillSyncSuccessCount(): Flow<Long>

    /**
     * 订阅同步成功的数量
     * 剔除已经删除的
     */
    @Query("select COUNT(b.id) from $TableName b where b.isSync = 1 AND b.isDeleted = 0")
    fun subscribeBillSyncSuccessCountExcludeDeleted(): Flow<Long>

    /**
     * 订阅没同步的数量
     * 包括已经删除的
     */
    @Query("select COUNT(b.id) from $TableName b where b.isSync = 0")
    fun subscribeUnSyncCount(): Flow<Long>

}

enum class BillDetailPageQueryType {

    // 详情列表, 并且 time 默认是降序
    DetailListDesc,

    // 详情列表, 并且 time 默认是升序
    DetailListASC,

    // 按照时间过了多少天分组
    DayTime,

    // 查询数量
    Count,

    // 金额
    AmountList,
}

class SupportBillDetailPageQueryImpl(
    private val queryType: BillDetailPageQueryType,
    private val queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
) : SupportSQLiteQuery // 占位
{

    private val TAG = "SupportBillDetailPageQuery"

    override val sql: String
        get() {

            LogSupport.d(
                tag = TAG,
                content = "-------------------${queryCondition.businessLogKey} billPageQuerySql getSql start--------------------------"
            )

            val sb = StringBuffer()
            when (queryType) {
                BillDetailPageQueryType.DayTime -> {
                    sb.append("SELECT (b.time + ${TimeZone.getDefault().rawOffset}) / 86400000 as day_time FROM ${BillDao.TableName} b")
                }

                BillDetailPageQueryType.DetailListDesc,
                BillDetailPageQueryType.DetailListASC -> {
                    sb.append("SELECT b.* FROM ${BillDao.TableName} b")
                }

                BillDetailPageQueryType.Count -> {
                    sb.append("SELECT COUNT(b.id) FROM ${BillDao.TableName} b")
                }

                BillDetailPageQueryType.AmountList -> {
                    sb.append("SELECT b.amount FROM ${BillDao.TableName} b")
                }

            }
            sb.append(" LEFT JOIN ${CategoryDao.TableName} c on c.id = b.categoryId and c.isDeleted = 0")
            /*sb.append(" LEFT JOIN ${BillLabelDao.TableName} bl on bl.billId = b.id and bl.isDeleted = 0")
            sb.append(" LEFT JOIN ${LabelDao.TableName} l on l.id = bl.labelId and l.isDeleted = 0")*/


            val whereList = buildList {
                // 添加条件
                queryCondition.isNotCalculate?.let {
                    this.add("b.isNotCalculate = ${it.toOneOrZero()}")
                }
                queryCondition.isDeleted?.let {
                    this.add("b.isDeleted = ${it.toOneOrZero()}")
                }
                queryCondition.searchQueryInfo?.let { searchQueryInfo ->
                    val searchQuerySqlList = mutableListOf<String>()
                    searchQueryInfo.billIdList?.let { searchBillIdList ->
                        val billIdListCondition = searchBillIdList.joinToString { "'$it'" }
                        searchQuerySqlList.add("b.id in ($billIdListCondition)")
                    }
                    searchQueryInfo.categoryIdList?.let { searchCategoryIdList ->
                        val categoryIdListCondition = searchCategoryIdList.joinToString { "'$it'" }
                        searchQuerySqlList.add("c.id in ($categoryIdListCondition)")
                    }
                    searchQueryInfo.aboutAccountIdList?.let { aboutAccountIdList ->
                        val aboutAccountIdListCondition = aboutAccountIdList.joinToString { "'$it'" }
                        searchQuerySqlList.add("b.accountId in ($aboutAccountIdListCondition) or b.transferTargetAccountId in ($aboutAccountIdListCondition)")
                    }
                    if (!searchQueryInfo.noteKey.isNullOrEmpty()) {
                        val numberKey =
                            searchQueryInfo.noteKey?.toFloatOrNull()?.times(other = 100)?.toLong()
                        if (numberKey == null) {
                            searchQuerySqlList.add("b.note LIKE '%${searchQueryInfo.noteKey}%'")
                        } else {
                            searchQuerySqlList.add("b.note LIKE '%${searchQueryInfo.noteKey}%' or b.amount LIKE '%${numberKey}%'")
                        }
                    }
                    if (searchQuerySqlList.isNotEmpty()) {
                        this.add(
                            element = "(${
                                searchQuerySqlList.joinToString(separator = " or ")
                            })"
                        )
                    }
                }
                if (queryCondition.idList.isNotEmpty()) {
                    if (queryCondition.idList.size == 1) {
                        this.add("b.id = '${queryCondition.idList.first()}'")
                    } else {
                        val idListCondition = queryCondition.idList.joinToString { "'$it'" }
                        this.add("b.id in ($idListCondition)")
                    }
                }
                if (queryCondition.typeList.isNotEmpty()) {
                    if (queryCondition.typeList.size == 1) {
                        this.add("b.type = '${queryCondition.typeList.first().value}'")
                    } else {
                        val typeListCondition = queryCondition.typeList.joinToString { "'${it.value}'" }
                        this.add("b.type in ($typeListCondition)")
                    }
                }
                if (queryCondition.userIdList.isNotEmpty()) {
                    if (queryCondition.userIdList.size == 1) {
                        this.add("b.userId = '${queryCondition.userIdList.first()}'")
                    } else {
                        val userIdListCondition = queryCondition.userIdList.joinToString { "'$it'" }
                        this.add("b.userId in ($userIdListCondition)")
                    }
                }
                if (queryCondition.bookIdList.isNotEmpty()) {
                    if (queryCondition.bookIdList.size == 1) {
                        this.add("b.bookId = '${queryCondition.bookIdList.first()}'")
                    } else {
                        val bookIdIdListCondition = queryCondition.bookIdList.joinToString { "'$it'" }
                        this.add("b.bookId in ($bookIdIdListCondition)")
                    }
                }
                if (queryCondition.categoryIdList.isNotEmpty()) {
                    if (queryCondition.categoryIdList.size == 1) {
                        this.add("b.categoryId = '${queryCondition.categoryIdList.first()}'")
                    } else {
                        val categoryIdListCondition =
                            queryCondition.categoryIdList.joinToString { "'$it'" }
                        this.add("b.categoryId in ($categoryIdListCondition)")
                    }
                }
                if (queryCondition.accountIdList.isNotEmpty()) {
                    if (queryCondition.accountIdList.size == 1) {
                        this.add("b.accountId = '${queryCondition.accountIdList.first()}'")
                    } else {
                        val accountIdListCondition =
                            queryCondition.accountIdList.joinToString { "'$it'" }
                        this.add("b.accountId in ($accountIdListCondition)")
                    }
                }
                if (queryCondition.transferTargetAccountIdList.isNotEmpty()) {
                    if (queryCondition.transferTargetAccountIdList.size == 1) {
                        this.add("b.transferTargetAccountId = '${queryCondition.transferTargetAccountIdList.first()}'")
                    } else {
                        val transferTargetAccountIdListCondition =
                            queryCondition.transferTargetAccountIdList.joinToString { "'$it'" }
                        this.add("b.transferTargetAccountId in ($transferTargetAccountIdListCondition)")
                    }
                }
                if (queryCondition.originBillIdList.isNotEmpty()) {
                    if (queryCondition.originBillIdList.size == 1) {
                        this.add("b.originBillId = '${queryCondition.originBillIdList.first()}'")
                    } else {
                        val originBillIdListCondition =
                            queryCondition.originBillIdList.joinToString { "'$it'" }
                        this.add("b.originBillId in ($originBillIdListCondition)")
                    }
                }
                if (queryCondition.aboutAccountIdList.isNotEmpty()) {
                    if (queryCondition.aboutAccountIdList.size == 1) {
                        val aboutAccountId = queryCondition.aboutAccountIdList.first()
                        this.add("(b.accountId = '${aboutAccountId}' or b.transferTargetAccountId ='${aboutAccountId}')")
                    } else {
                        val aboutAccountIdListCondition =
                            queryCondition.aboutAccountIdList.joinToString { "'$it'" }
                        this.add("(b.accountId in ($aboutAccountIdListCondition) or b.transferTargetAccountId in ($aboutAccountIdListCondition))")
                    }
                }
                queryCondition.startTimeInclude?.let {
                    this.add("b.time >= $it")
                }
                queryCondition.endTimeInclude?.let {
                    this.add("b.time <= $it")
                }
                if (queryCondition.amountMoreThanZero == true) {
                    this.add("b.amount > 0")
                }
                if (queryCondition.amountLessThanZero == true) {
                    this.add("b.amount < 0")
                }
                queryCondition.amountMin?.let {
                    this.add("abs(b.amount) >= $it")
                }
                queryCondition.amountMax?.let {
                    this.add("abs(b.amount) <= $it")
                }
            }

            if (whereList.isNotEmpty()) {
                sb.append(" where ${
                    whereList.joinToString(
                        separator = " and ",
                    )
                }")
            }

            // 条件结束了

            when (queryType) {

                BillDetailPageQueryType.DayTime -> {
                    // day_time 这个是上面 sql 查询的别名
                    sb.append(" group by day_time")
                    sb.append(" order by day_time desc")
                }

                BillDetailPageQueryType.DetailListDesc -> {
                    sb.append(" order by time desc")
                }

                BillDetailPageQueryType.DetailListASC -> {
                    sb.append(" order by time asc")
                }

                else -> {}
            }

            queryCondition.pageInfo?.let { pageInfo ->
                sb.append(" limit ${pageInfo.pageStartIndex}, ${pageInfo.pageSize}")
            }

            return sb.toString().apply {
                LogSupport.d(
                    tag = TAG,
                    content = "-------------------${queryCondition.businessLogKey} billPageQuerySql getSql end--------------------------"
                )
                LogSupport.d(
                    tag = TAG,
                    content = "${queryCondition.businessLogKey} billPageQuerySql = $this"
                )
            }
        }

    override fun bindTo(statement: SupportSQLiteProgram) {
    }

    override val argCount = 0

}
