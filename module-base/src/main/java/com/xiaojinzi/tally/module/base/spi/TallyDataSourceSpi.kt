package com.xiaojinzi.tally.module.base.spi

import android.os.Parcelable
import androidx.annotation.Keep
import com.xiaojinzi.support.annotation.PublishHotObservable
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.ktx.SuspendAction0
import com.xiaojinzi.tally.lib.res.model.exception.NoBookSelectException
import com.xiaojinzi.tally.lib.res.model.support.TallyTimeDay
import com.xiaojinzi.tally.lib.res.model.tally.BillChatDto
import com.xiaojinzi.tally.lib.res.model.tally.BillChatInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.lib.res.model.tally.TallyAccountDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyAccountInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDetailDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillImageDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillImageInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillLabelDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillLabelInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookNecessaryInfoResDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.lib.res.model.tally.toInsertDto
import com.xiaojinzi.tally.lib.res.model.user.UserInfoCacheDto
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

/**
 * 提供了数据层的一个强大的支持
 */
interface TallyDataSourceSpi {

    companion object {

        const val TAG = "TallyDataSourceSpi"

        @Keep
        @Parcelize
        data class PageInfo(
            val pageStartIndex: Int,
            val pageSize: Int,
        ) : Parcelable

        @Keep
        @Parcelize
        data class SearchQueryConditionDto(
            val billIdList: List<String>? = null,
            val categoryIdList: List<String>? = null,
            val aboutAccountIdList: List<String>? = null,
            val noteKey: String? = null,
        ) : Parcelable

        /**
         * 这里的每个条件都是 and 的关系
         * 如果要搜索之类的. 用 [BillQueryConditionDto.searchQueryInfo] 对象.
         * 这个对象内的属性都会是 or 的关系
         */
        @Keep
        @Parcelize
        data class BillQueryConditionDto(
            val businessLogKey: String? = null,
            // 一般是一些多对多的时候用的, 比如要显示某个标签下的账单
            val idList: List<String> = emptyList(),
            /**
             * 查询的账单类别
             * 默认是查询普通和退款的
             */
            val typeList: List<TallyBillDto.Type> = emptyList(),
            val userIdList: List<String> = emptyList(),
            val bookIdList: List<String> = emptyList(),
            val categoryIdList: List<String> = emptyList(),
            val accountIdList: List<String> = emptyList(),
            val transferTargetAccountIdList: List<String> = emptyList(),
            val originBillIdList: List<String> = emptyList(),
            val aboutAccountIdList: List<String> = emptyList(),
            val searchQueryInfo: SearchQueryConditionDto? = null,
            val startTimeInclude: Long? = null,
            val endTimeInclude: Long? = null,
            val amountMoreThanZero: Boolean? = null,
            val amountLessThanZero: Boolean? = null,
            val amountMin: Long? = null,
            val amountMax: Long? = null,
            val pageInfo: PageInfo? = null,
            val isNotCalculate: Boolean? = null,
            val isDeleted: Boolean? = false,
        ) : Parcelable

    }

    /**
     * 数据库变化的通知
     */
    @PublishHotObservable
    val dataBaseTablesChangedEventOb: Flow<List<TallyTable>>

    /**
     * 数据库任意一个表变化的通知
     */
    @PublishHotObservable
    val dataBaseChangedEventOb: Flow<Unit>

    /**
     * 所有的账本
     * 排序是无序的, 因为可能有不同用户的. 所以这里面做不了排序
     */
    @StateHotObservable
    val allBookStateOb: Flow<List<TallyBookDto>>

    /**
     * 全局选择的账本, 只能选择一个
     */
    @StateHotObservable
    val selectedBookStateOb: Flow<TallyBookDto?>

    /**
     * 同步成功的账单数量
     */
    @StateHotObservable
    val syncSuccessBillCountStateOb: Flow<Long>

    /**
     * 同步成功的账单数量
     */
    @StateHotObservable
    val syncSuccessBillExcludeDeletedCountStateOb: Flow<Long>

    @StateHotObservable
    val unSyncCategoryCountStateOb: Flow<Long>

    @StateHotObservable
    val unSyncAccountCountStateOb: Flow<Long>

    @StateHotObservable
    val unSyncLabelCountStateOb: Flow<Long>

    @StateHotObservable
    val unSyncBillLabelCountStateOb: Flow<Long>

    @StateHotObservable
    val unSyncBillImageCountStateOb: Flow<Long>

    @StateHotObservable
    val unSyncBillCountStateOb: Flow<Long>

    /**
     * 当前选择的账本
     */
    @Throws(NoBookSelectException::class)
    suspend fun requiredSelectedBookInfo(): TallyBookDto

    /**
     * 设置空账本
     */
    suspend fun setNullSelectedBook()

    /**
     * 充值全局选择的账本
     */
    suspend fun resetSelectedBook()

    /**
     * 清理所有数据
     */
    suspend fun clearAllData()

    /**
     * 清理某个账本下的所有数据
     */
    suspend fun clearAllDataByBookId(
        bookId: String,
    )

    /**
     * 尝试刷新用户信息的缓存
     */
    fun tryRefreshUserInfoCache(
        userIdList: List<String>,
    ): SuspendAction0

    /**
     * 获取缓存的用户信息
     */
    suspend fun getCacheUserInfo(
        userId: String,
    ): UserInfoCacheDto?

    /**
     * 尝试更新账本
     */
    fun tryRefreshBookList(
        userId: String,
    ): SuspendAction0

    /**
     * 切换账本
     */
    suspend fun switchBook(
        bookId: String,
        isTipAfterSwitch: Boolean = false,
    )

    suspend fun insertBookNecessaryInfo(
        bookNecessaryInfo: TallyBookNecessaryInfoResDto,
    ) {
        withTransaction {
            this.insertBookList(
                targetList = listOf(
                    bookNecessaryInfo.book.toInsertDto(),
                ),
            )
            this.insertOrUpdateCategoryList(
                targetList = bookNecessaryInfo.categoryList.map {
                    it.toInsertDto()
                },
            )
        }
    }

    /**
     * 订阅某些表的变化
     * State 或者 Event
     */
    @PublishHotObservable
    @StateHotObservable
    fun subscribeDataBaseTableChangedOb(
        vararg tables: TallyTable,
        emitOneWhileSubscribe: Boolean = false,
    ): Flow<Unit>

    /**
     * 开始事务
     */
    suspend fun <R> withTransaction(
        block: suspend () -> R,
    ): R

    /**
     * 插入测试数据
     * 多次调用, 只有一次有效
     */
    suspend fun insertTestDataOnce()

    /**
     * 是否初始化了数据
     */
    suspend fun isInitData(): Boolean

    /**
     * 同步第一次的数据
     */
    suspend fun syncFirstData(
        userId: String,
    )

    /**
     * 插入账本列表
     */
    suspend fun insertBookList(
        targetList: List<TallyBookInsertDto>,
    ): List<String>

    /**
     * 获取所有账本
     */
    suspend fun getAllBookList(): List<TallyBookDto>

    /**
     * 根据 Id 获取账本
     */
    suspend fun getBookById(
        id: String,
    ): TallyBookDto?

    /**
     * 根据 id 获取账单
     */
    suspend fun getBillDetailById(
        id: String,
    ): TallyBillDetailDto?

    /**
     * 插入一个账单
     */
    suspend fun insertBill(
        target: TallyBillInsertDto,
        labelIdList: List<String> = emptyList(),
        imageUrlList: List<String> = emptyList(),
        isNeedSync: Boolean = false,
    ): String

    /**
     * 更新账单
     */
    suspend fun updateBill(
        target: TallyBillDto,
        labelIdList: List<String> = emptyList(),
        imageUrlList: List<String> = emptyList(),
        isNeedSync: Boolean = false,
    )

    /**
     * 插入或者更新账单列表
     * 基本是给批量操作用的
     * 1. 从远程同步
     */
    suspend fun insertOrUpdateBillList(
        targetList: List<TallyBillInsertDto>,
        isNeedSync: Boolean = false,
    ): List<String>

    /**
     * 根据 timeModify 倒序排列, 获取最新一条数据
     */
    suspend fun getLatestOneBillOrderByTimeModify(
        bookId: String,
    ): TallyBillDto?

    /**
     * 获取未同步的账单列表
     */
    suspend fun getUnSyncBillList(
        bookId: String,
        pageSize: Int,
    ): List<TallyBillDto>

    /**
     * 查询 DayTime 列表
     */
    suspend fun getBillDayTimeListByCondition(
        queryCondition: BillQueryConditionDto = BillQueryConditionDto(),
    ): List<TallyTimeDay>

    /**
     * 查询 DayTime 列表
     */
    fun subscribeDayTimeListByCondition(
        queryCondition: BillQueryConditionDto = BillQueryConditionDto(),
    ): Flow<List<TallyTimeDay>>

    /**
     * 查询账单详情列表
     */
    suspend fun getBillDetailListByCondition(
        queryCondition: BillQueryConditionDto = BillQueryConditionDto(),
    ): List<TallyBillDetailDto>

    /**
     * 获取满足条件的账单的金钱总和
     * 内置的值有正负之分
     * 比如：
     * -. 日消费和收入
     * -. 月消费和收入
     * -. 年消费和收入
     */
    suspend fun getBillAmountByCondition(
        queryCondition: BillQueryConditionDto = BillQueryConditionDto(),
    ): MoneyFen

    /**
     * 获取满足条件的账单的数量
     */
    suspend fun getBillCountByCondition(
        queryCondition: BillQueryConditionDto = BillQueryConditionDto(),
    ): Long

    /**
     * 获取满足条件的账单的金钱总和
     * 比如：
     * -. 日消费和收入
     * -. 月消费和收入
     * -. 年消费和收入
     * 支持的账单类型：
     * [TallyBillDto.Type.NORMAL]
     * [TallyBillDto.Type.REFUND]
     */
    fun subscribeBillAmountByCondition(
        queryCondition: BillQueryConditionDto = BillQueryConditionDto(),
    ): Flow<MoneyFen>

    /**
     * 订阅账单详情
     */
    fun subscribeBillDetailById(
        id: String,
    ): Flow<TallyBillDetailDto?>

    /**
     * 订阅关联的退款账单列表个数
     */
    fun subscribeAssociatedRefundBillListCount(
        billId: String,
    ): Flow<Long>

    /**
     * 订阅账单详情列表
     */
    fun subscribeBillDetailList(
        queryCondition: BillQueryConditionDto = BillQueryConditionDto(),
    ): Flow<List<TallyBillDetailDto>>

    /**
     * 查询账单的数量
     */
    fun subscribeBillCount(
        queryCondition: BillQueryConditionDto = BillQueryConditionDto(),
    ): Flow<Long>

    /**
     * 插入或者更新列表
     */
    suspend fun insertOrUpdateBillLabelList(
        targetList: List<TallyBillLabelInsertDto>,
        isNeedSync: Boolean = false,
    ): List<String>

    /**
     * 获取未同步的账单&标签列表
     */
    suspend fun getUnSyncBillLabelList(
        bookId: String,
        pageSize: Int,
    ): List<TallyBillLabelDto>

    /**
     * 插入或者更新列表
     */
    suspend fun insertOrUpdateBillImageList(
        targetList: List<TallyBillImageInsertDto>,
        isNeedSync: Boolean = false,
    ): List<String>

    /**
     * 获取未同步的账单&图片列表
     */
    suspend fun getUnSyncBillImageList(
        bookId: String,
        pageSize: Int,
    ): List<TallyBillImageDto>

    /**
     * 订阅所有类别
     */
    @StateHotObservable
    fun subscribeAllCategory(
        bookIdList: List<String> = emptyList(),
    ): Flow<List<TallyCategoryDto>>

    /**
     * 插入类别
     */
    suspend fun insertCategory(
        target: TallyCategoryInsertDto,
        isNeedSync: Boolean = false,
    ): String

    /**
     * 插入类别
     */
    suspend fun insertOrUpdateCategoryList(
        targetList: List<TallyCategoryInsertDto>,
        isNeedSync: Boolean = false,
    ): List<String>

    /**
     * 更新类别
     */
    suspend fun updateCategory(
        target: TallyCategoryDto,
        isNeedSync: Boolean = false,
    )

    /**
     * 更新类别列表
     */
    suspend fun updateCategoryList(
        targetList: List<TallyCategoryDto>,
        isNeedSync: Boolean = false,
    )

    /**
     * 根据 timeModify 倒序排列, 获取最新一条数据
     */
    suspend fun getLatestOneCategoryOrderByTimeModify(
        bookId: String,
    ): TallyCategoryDto?

    /**
     * 根据 id 获取类别
     */
    suspend fun getCategoryById(
        id: String,
    ): TallyCategoryDto?

    /**
     * 根据 id 获取类别
     */
    suspend fun getCategoryByIdAndBookId(
        id: String,
        bookId: String,
    ): TallyCategoryDto?

    /**
     * 根据 bookId 获取类别
     */
    suspend fun getCategoryByBookId(
        bookId: String,
    ): List<TallyCategoryDto>

    /**
     * 根据 parentId 获取类别列表
     */
    suspend fun getCategoryByParentId(
        parentId: String,
    ): List<TallyCategoryDto>

    suspend fun getUnSyncCategoryList(
        bookId: String,
        pageSize: Int,
    ): List<TallyCategoryDto>

    /**
     * 搜索类别, 这个支持所有账本的, 没有 bookId 参数
     */
    suspend fun searchCategory(
        key: String,
    ): List<TallyCategoryDto>

    /**
     * 订阅所有账户
     */
    @StateHotObservable
    fun subscribeAllAccount(
        bookId: String,
    ): Flow<List<TallyAccountDto>>

    /**
     * 插入账户
     */
    suspend fun insertOrUpdateAccount(
        target: TallyAccountInsertDto,
        isNeedSync: Boolean = false,
    ): String

    /**
     * 插入账户
     */
    suspend fun insertOrUpdateAccountList(
        targetList: List<TallyAccountInsertDto>,
        isNeedSync: Boolean = false,
    ): List<String>

    /**
     * 更新账号
     */
    suspend fun updateAccount(
        target: TallyAccountDto,
        isNeedSync: Boolean = false,
    )

    /**
     * 根据 timeModify 倒序排列, 获取最新一条数据
     */
    suspend fun getLatestOneAccountOrderByTimeModify(
        bookId: String,
    ): TallyAccountDto?

    /**
     * 根据 id 获取账户
     */
    suspend fun getAccountById(
        id: String,
    ): TallyAccountDto?

    /**
     * 根据 id 获取账户
     */
    suspend fun getAccountByIdAndBookId(
        id: String,
        bookId: String,
    ): TallyAccountDto?

    /**
     * 更新账户为非默认
     */
    suspend fun updateAllAccountToNotDefaultByBookId(
        bookId: String,
    )

    /**
     * 获取所有账户
     */
    suspend fun getAccountByBookId(
        bookId: String,
        isExcludeDeleted: Boolean = false,
    ): List<TallyAccountDto>

    suspend fun getUnSyncAccountList(
        bookId: String,
        pageSize: Int,
    ): List<TallyAccountDto>

    /**
     * 搜索账户, 这个支持所有账本的, 没有 bookId 参数
     */
    suspend fun searchAccount(
        key: String,
    ): List<TallyAccountDto>


    suspend fun insertLabel(
        target: TallyLabelInsertDto,
        isNeedSync: Boolean = false,
    ): String

    suspend fun updateLabel(
        target: TallyLabelDto,
        isNeedSync: Boolean = false,
    )

    /**
     * 插入标签
     */
    suspend fun insertOrUpdateLabelList(
        targetList: List<TallyLabelInsertDto>,
        isNeedSync: Boolean = false,
    ): List<String>

    suspend fun getLabel(
        id: String,
    ): TallyLabelDto?

    suspend fun getLabelUnderBook(
        bookId: String,
        id: String,
    ): TallyLabelDto?

    /**
     * 获取所有标签
     */
    suspend fun getAllLabelUnderBook(
        bookId: String,
        isExcludeDeleted: Boolean = false,
    ): List<TallyLabelDto>

    /**
     * 根据 账单 Id 获取标签列表
     */
    suspend fun getLabelListByBillId(
        bookId: String,
        billId: String,
    ): List<TallyLabelDto>

    /**
     * 根据 timeModify 倒序排列, 获取最新一条数据
     */
    suspend fun getLatestOneLabelOrderByTimeModify(
        bookId: String,
    ): TallyLabelDto?

    /**
     * 获取未同步的标签列表
     */
    suspend fun getUnSyncLabelList(
        bookId: String,
        pageSize: Int,
    ): List<TallyLabelDto>

    /**
     * 搜索标签, 这个支持所有账本的, 没有 bookId 参数
     */
    suspend fun searchLabel(
        key: String,
    ): List<TallyLabelDto>

    /**
     * 订阅所有的标签
     */
    fun subscribeAllLabel(
        bookId: String,
    ): Flow<List<TallyLabelDto>>

    /**
     * 根据 LabelId 获取 billId list
     */
    suspend fun getBillIdListByLabelId(
        bookId: String,
        labelId: String,
        isExcludeDeleted: Boolean = false,
    ): List<String>

    /**
     * 根据 LabelIdList 获取 billId list
     */
    suspend fun getBillIdListByLabelIdList(
        bookId: String? = null,
        labelIdList: List<String>,
        isExcludeDeleted: Boolean = false,
    ): List<String>

    /**
     * 根据 timeModify 倒序排列, 获取最新一条数据
     */
    suspend fun getLatestOneBillLabelOrderByTimeModify(
        bookId: String,
    ): TallyBillLabelDto?

    /**
     * 根据 timeModify 倒序排列, 获取最新一条数据
     */
    suspend fun getLatestOneBillImageOrderByTimeModify(
        bookId: String,
    ): TallyBillImageDto?

    /**
     * 获取账单图片列表根据 BillId
     */
    suspend fun getBillImageListByBillId(
        bookId: String,
        billId: String,
    ): List<TallyBillImageDto>

    /**
     * 获取账单图片列表根据 BillId
     */
    suspend fun getBillImageList(
        bookId: String,
    ): List<TallyBillImageDto>

    /**
     * 获取图片表中所有的 billId
     */
    suspend fun getBillIdListInImage(
        bookId: String? = null,
        isExcludeDeleted: Boolean = false,
    ): List<String>

    /**
     *  插入
     */
    suspend fun insertBillChat(
        target: BillChatInsertDto,
    ): Long

    /**
     * 查询 DayTime 列表
     */
    suspend fun getBillChatDayTimeList(
        bookId: String,
        pageStartIndex: Long = 0,
        pageSize: Long,
    ): List<Long>

    /**
     * 根据 Id 获取
     */
    suspend fun getBillChatById(
        id: Long,
    ): BillChatDto?

    /**
     * 根据 Id 删除
     */
    suspend fun deleteBillChatById(
        id: Long,
    )

    /**
     * 根据 Id 获取
     */
    suspend fun updateBillChat(
        target: BillChatDto,
    )

    /**
     * 更新所有 init 状态去失败的
     */
    suspend fun updateAllInitBillChatToFail()

    /**
     * 获取 Ai 账单聊天的数据
     */
    suspend fun getBillChatList(
        bookId: String,
        afterTime: Long,
    ): List<BillChatDto>

}