package com.xiaojinzi.tally.module.datasource.spi

import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import com.xiaojinzi.component.anno.ServiceAnno
import com.xiaojinzi.module.common.base.spi.spPersistence
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.NormalMutableSharedFlow
import com.xiaojinzi.support.ktx.SuspendAction0
import com.xiaojinzi.support.ktx.awaitIgnoreException
import com.xiaojinzi.support.ktx.notSupportError
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.suspendAction0
import com.xiaojinzi.tally.lib.res.model.exception.NoBookSelectException
import com.xiaojinzi.tally.lib.res.model.support.HourMillisecond
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
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.lib.res.model.tally.toInsertDto
import com.xiaojinzi.tally.lib.res.model.user.UserInfoCacheDto
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.view.compose.commonAppToast
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb.totalTableNames
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.AccountDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillChatDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDetailPageQueryType
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillImageDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillImageDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillLabelDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillLabelDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.LabelDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.SupportBillDetailPageQueryImpl
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.UserInfoCacheDo
import com.xiaojinzi.tally.module.datasource.support.toDo
import com.xiaojinzi.tally.module.datasource.support.toDto
import com.xiaojinzi.tally.module.datasource.support.toInsertDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

@ServiceAnno(TallyDataSourceSpi::class, singleTon = false)
class TallyDataSourceSpiImpl : TallyDataSourceSpi {

    override val dataBaseTablesChangedEventOb = NormalMutableSharedFlow<List<TallyTable>>()

    override val dataBaseChangedEventOb = dataBaseTablesChangedEventOb.map { }

    override val allBookStateOb = TallyDb
        .database
        .bookDao()
        .all()
        .map { list ->
            list
                .map {
                    it.toDto()
                }
        }
        .sharedStateIn(
            scope = AppScope,
        )

    private val selectedBookIdStateOb = MutableSharedStateFlow<String?>()
        .spPersistence(
            scope = AppScope,
            key = "selectedBookId",
            def = null,
        )

    override val selectedBookStateOb = combine(
        allBookStateOb, selectedBookIdStateOb
    ) { allBook, selectedBookId ->
        allBook.find { it.id == selectedBookId }
    }

    override val syncSuccessBillCountStateOb = TallyDb
        .database
        .billDao()
        .subscribeBillSyncSuccessCount()
        .sharedStateIn(
            scope = AppScope,
            initValue = 0,
        )

    override val syncSuccessBillExcludeDeletedCountStateOb = TallyDb
        .database
        .billDao()
        .subscribeBillSyncSuccessCountExcludeDeleted()
        .sharedStateIn(
            scope = AppScope,
            initValue = 0,
        )

    override val unSyncCategoryCountStateOb = TallyDb
        .database
        .categoryDao()
        .subscribeUnSyncCount()
        .sharedStateIn(
            scope = AppScope,
            initValue = 0,
        )

    override val unSyncAccountCountStateOb = TallyDb
        .database
        .accountDao()
        .subscribeUnSyncCount()
        .sharedStateIn(
            scope = AppScope,
            initValue = 0,
        )

    override val unSyncLabelCountStateOb = TallyDb
        .database
        .labelDao()
        .subscribeUnSyncCount()
        .sharedStateIn(
            scope = AppScope,
            initValue = 0,
        )

    override val unSyncBillLabelCountStateOb = TallyDb
        .database
        .billLabelDao()
        .subscribeUnSyncCount()
        .sharedStateIn(
            scope = AppScope,
            initValue = 0,
        )

    override val unSyncBillImageCountStateOb = TallyDb
        .database
        .billImageDao()
        .subscribeUnSyncCount()
        .sharedStateIn(
            scope = AppScope,
            initValue = 0,
        )

    override val unSyncBillCountStateOb = TallyDb
        .database
        .billDao()
        .subscribeUnSyncCount()
        .sharedStateIn(
            scope = AppScope,
            initValue = 0,
        )

    override suspend fun requiredSelectedBookInfo(): TallyBookDto {
        return selectedBookStateOb.firstOrNull() ?: throw NoBookSelectException()
    }

    override suspend fun setNullSelectedBook() {
        selectedBookIdStateOb.emit(
            value = null,
        )
    }

    override suspend fun resetSelectedBook() {
        val bookInfo = selectedBookIdStateOb.firstOrNull()?.run {
            getBookById(
                id = this
            )
        }
        if (bookInfo == null) {
            selectedBookIdStateOb.emit(
                value = allBookStateOb.first().find { it.isSystem }?.id
            )
        }
    }

    override suspend fun clearAllData() {
        withContext(context = Dispatchers.IO) {
            TallyDb
                .database
                .clearAllTables()
        }
    }

    override suspend fun clearAllDataByBookId(bookId: String) {
        withContext(context = Dispatchers.IO) {
            // 开启事务
            withTransaction {
                TallyDb
                    .database.apply {
                        this.accountDao()
                            .deleteAllByBookId(
                                bookId = bookId,
                            )
                        this.categoryDao()
                            .deleteAllByBookId(
                                bookId = bookId,
                            )
                        this.labelDao()
                            .deleteAllByBookId(
                                bookId = bookId,
                            )
                        this.billLabelDao()
                            .deleteAllByBookId(
                                bookId = bookId,
                            )
                        this.billDao()
                            .deleteAllByBookId(
                                bookId = bookId,
                            )
                        this.bookDao()
                            .deleteById(
                                id = bookId,
                            )
                    }
            }
            // 重新选择一个账本
            resetSelectedBook()
        }
    }

    override fun tryRefreshUserInfoCache(
        userIdList: List<String>,
    ): SuspendAction0 {
        return suspendAction0 {
            val userInfoCacheDao = TallyDb
                .database
                .userInfoCacheDao()
            val currentTime = System.currentTimeMillis()
            // 没有过期的数据
            val oldUserInfoList = userInfoCacheDao
                .getByIds(
                    ids = userIdList,
                )
                .filter {
                    it.timeExpire > currentTime
                }
            val needInsertOrUpdateUserIdList = userIdList
                .filter { id ->
                    oldUserInfoList.none { it.id == id }
                }
            val newUserList = AppServices.appNetworkSpi.getUserInfoList(
                userIdList = needInsertOrUpdateUserIdList,
            )
            if (newUserList.isNotEmpty()) {
                withTransaction {
                    userInfoCacheDao.insert(
                        targetList = newUserList.map {
                            UserInfoCacheDo(
                                id = it.id,
                                name = it.name,
                                // 过期时间要 1 小时
                                timeExpire = currentTime + HourMillisecond,
                            )
                        }
                    )
                }
            }
        }
    }

    override suspend fun getCacheUserInfo(userId: String): UserInfoCacheDto? {
        return TallyDb
            .database
            .userInfoCacheDao()
            .getById(
                id = userId,
            )?.toDto()
    }

    override fun tryRefreshBookList(
        userId: String,
    ): SuspendAction0 {
        return suspendAction0 {
            val bookList = AppServices.appNetworkSpi.getBookList()
            val needDeleteBookList = getAllBookList()
                .filter { item ->
                    bookList.none { it.id == item.id }
                }
            // 插入或者更新所有账本
            insertBookList(
                targetList = bookList
                    .map { it.toInsertDto() }
            )
            // 删除远程已经删除的账本
            needDeleteBookList.forEach { item ->
                clearAllDataByBookId(
                    bookId = item.id,
                )
            }
            // 刷新缓存的用户数据
            tryRefreshUserInfoCache(
                userIdList = bookList.map {
                    it.userId
                }.filter {
                    it != userId
                }
            ).awaitIgnoreException()
        }
    }

    override suspend fun switchBook(
        bookId: String,
        isTipAfterSwitch: Boolean,
    ) {
        val currentBookId = selectedBookIdStateOb.firstOrNull()
        if (currentBookId != bookId) {
            val allBook = allBookStateOb
                .first()
            val selectedBook = allBook
                .find { it.id == bookId } ?: allBook.firstOrNull()
            selectedBookIdStateOb.emit(
                value = selectedBook?.id,
            )
            selectedBook?.run {
                AppServices
                    .tallyDataSyncSpi
                    ?.trySyncSingleBook(
                        bookId = this.id,
                    )
            }
            if (isTipAfterSwitch) {
                commonAppToast(
                    content = "切换成功",
                )
            }
        }
    }

    override fun subscribeDataBaseTableChangedOb(
        vararg tables: TallyTable,
        emitOneWhileSubscribe: Boolean,
    ): Flow<Unit> {
        return dataBaseTablesChangedEventOb
            .filter { list ->
                list.any { listItem ->
                    tables.any { tableItem ->
                        tableItem == listItem
                    }
                }
            }
            .map { }
            .onStart {
                if (emitOneWhileSubscribe) {
                    emit(Unit)
                }
            }
    }

    override suspend fun <R> withTransaction(
        block: suspend () -> R,
    ): R {
        return TallyDb.database.withTransaction(block = block)
    }

    override suspend fun insertTestDataOnce() {
    }

    override suspend fun isInitData(): Boolean {
        return TallyDb
            .database
            .bookDao()
            .getCount() > 0
    }

    override suspend fun syncFirstData(
        userId: String,
    ) {
        if (!isInitData()) {
            val syncResult = AppServices.appNetworkSpi.initSync()
            // 开启事务
            withTransaction {
                insertBookList(
                    targetList = syncResult.bookList.map {
                        it.toInsertDto()
                    }
                )
                insertOrUpdateCategoryList(
                    targetList = syncResult.categoryList.map {
                        it.toInsertDto()
                    }
                )
            }
            // 刷新缓存的用户数据
            tryRefreshUserInfoCache(
                userIdList = syncResult.bookList.map {
                    it.userId
                }.filter {
                    it != userId
                }
            ).awaitIgnoreException()
        }
    }

    override suspend fun insertBookList(
        targetList: List<TallyBookInsertDto>,
    ): List<String> {
        val targetDoList = targetList.map {
            it.toDo()
        }
        TallyDb
            .database
            .bookDao()
            .insert(
                targetList = targetDoList,
            )
        return targetDoList.map {
            it.id
        }
    }

    override suspend fun getAllBookList(): List<TallyBookDto> {
        return TallyDb
            .database
            .bookDao()
            .getAll()
            .map {
                it.toDto()
            }
    }

    override suspend fun getBookById(id: String): TallyBookDto? {
        return TallyDb
            .database
            .bookDao()
            .getById(
                id = id,
            )
            ?.toDto()
    }

    override suspend fun getBillDetailById(
        id: String,
    ): TallyBillDetailDto? {
        return TallyDb
            .database
            .billDao()
            .getById(
                id = id,
            )?.toDto()
    }

    override suspend fun insertBill(
        target: TallyBillInsertDto,
        labelIdList: List<String>,
        imageUrlList: List<String>,
        isNeedSync: Boolean,
    ): String {
        val targetBillDo = target.toDo()
        withTransaction {
            TallyDb
                .database
                .billDao()
                .insert(
                    target = targetBillDo,
                )
            TallyDb
                .database
                .billLabelDao()
                .insert(
                    targetList = labelIdList.map { labelId ->
                        BillLabelDo(
                            id = TallyDb.generateUniqueStr(),
                            userId = target.userId,
                            bookId = target.bookId,
                            billId = targetBillDo.id,
                            labelId = labelId,
                            timeModify = null,
                            isDeleted = false,
                            isSync = false,
                        )
                    }
                )
            TallyDb
                .database
                .billImageDao()
                .insert(
                    targetList = imageUrlList.map {
                        BillImageDo(
                            id = TallyDb.generateUniqueStr(),
                            userId = targetBillDo.userId,
                            bookId = target.bookId,
                            billId = targetBillDo.id,
                            url = it,
                            timeModify = null,
                            isDeleted = false,
                            isSync = false,
                        )
                    }
                )
        }
        return targetBillDo.id
    }

    override suspend fun updateBill(
        target: TallyBillDto,
        labelIdList: List<String>,
        imageUrlList: List<String>,
        isNeedSync: Boolean,
    ) {

        val billDao = TallyDb
            .database
            .billDao()

        val billLabelDao = TallyDb
            .database
            .billLabelDao()

        val billImageDao = TallyDb
            .database
            .billImageDao()

        val targetBillDo = target.copy(
            isSync = false,
        ).toDo()

        withTransaction {
            // 更新账单本身
            billDao.update(
                target = targetBillDo,
            )

            // 更新相关的退款单
            run {
                // 相关的退款单的一些信息需要一致
                if (targetBillDo.isDeleted) {
                    billDao.updateSubRefundBill(
                        originBillId = targetBillDo.id,
                        categoryId = targetBillDo.categoryId,
                        isDeleted = true,
                    )
                } else {
                    billDao.updateSubRefundBill(
                        originBillId = targetBillDo.id,
                        categoryId = targetBillDo.categoryId,
                    )
                }
            }

            // 更新账单图片
            run {
                val oldBillImageList = billImageDao
                    .getListByBookIdAndBillId(
                        bookId = target.bookId,
                        billId = target.id,
                    )
                    .map {
                        it.copy(
                            isDeleted = true,
                            isSync = false,
                        )
                    }
                billImageDao.update(
                    targetList = oldBillImageList,
                )

                val newBillImageList = imageUrlList.map { imageUrl ->
                    (oldBillImageList.find { item ->
                        item.url == imageUrl
                    } ?: BillImageDo(
                        id = TallyDb.generateUniqueStr(),
                        userId = target.userId,
                        bookId = target.bookId,
                        billId = targetBillDo.id,
                        url = imageUrl,
                        timeModify = null,
                        isDeleted = false,
                        isSync = false,
                    )).copy(
                        isDeleted = false,
                    )
                }
                billImageDao
                    .insert(
                        targetList = newBillImageList,
                    )

            }
            // 更新账单标签
            run {
                // 先把原来的更新为删除状态, 并且更新为未同步
                val oldBillLabelList = billLabelDao
                    .getListByBillId(
                        bookId = target.bookId,
                        billId = target.id,
                    )
                    .map {
                        it.copy(
                            isDeleted = true,
                            isSync = false,
                        )
                    }
                billLabelDao.update(
                    targetList = oldBillLabelList,
                )

                // 然后重新插入, 重复的会自动覆盖
                val newBillLabelList = labelIdList.map { labelId ->
                    (oldBillLabelList.find { item ->
                        item.labelId == labelId
                    } ?: BillLabelDo(
                        id = TallyDb.generateUniqueStr(),
                        userId = target.userId,
                        bookId = target.bookId,
                        billId = targetBillDo.id,
                        labelId = labelId,
                        timeModify = null,
                        isDeleted = false,
                        isSync = false,
                    )).copy(
                        isDeleted = false,
                    )
                }
                billLabelDao
                    .insert(
                        targetList = newBillLabelList,
                    )

            }

        }
    }

    override suspend fun insertOrUpdateBillList(
        targetList: List<TallyBillInsertDto>,
        isNeedSync: Boolean,
    ): List<String> {
        val doList = targetList.map { it.toDo() }
        TallyDb
            .database
            .billDao()
            .insert(
                targetList = doList,
            )
        return doList.map { it.id }
    }

    override suspend fun getLatestOneBillOrderByTimeModify(
        bookId: String,
    ): TallyBillDto? {
        return TallyDb
            .database
            .billDao()
            .getLatestOneOrderByTimeModify(
                bookId = bookId,
            )
            ?.toDto()
    }

    override suspend fun getUnSyncBillList(bookId: String, pageSize: Int): List<TallyBillDto> {
        return TallyDb
            .database
            .billDao()
            .getUnSyncList(
                bookId = bookId,
                pageSize = pageSize,
            ).map {
                it.toDto()
            }
    }

    override suspend fun getBillDayTimeListByCondition(
        queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
    ): List<TallyTimeDay> {
        return TallyDb
            .database
            .billDao()
            .queryDayTimeListByCondition(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.DayTime,
                    queryCondition = queryCondition,
                )
            )
            .map {
                TallyTimeDay(value = it)
            }
    }

    override fun subscribeDayTimeListByCondition(
        queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
    ): Flow<List<TallyTimeDay>> {
        return TallyDb
            .database
            .billDao()
            .subscribeDayTimeListByCondition(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.DayTime,
                    queryCondition = queryCondition,
                )
            )
            .map { list ->
                list.map {
                    TallyTimeDay(value = it)
                }
            }
    }

    override suspend fun getBillDetailListByCondition(
        queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
    ): List<TallyBillDetailDto> {
        return TallyDb
            .database
            .billDao()
            .queryBillDetailList(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.DetailListDesc,
                    queryCondition = queryCondition,
                )
            )
            .map {
                it.toDto()
            }
    }

    override suspend fun getBillAmountByCondition(
        queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
    ): MoneyFen {
        val result = TallyDb
            .database
            .billDao()
            .queryAmountList(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.AmountList,
                    queryCondition = queryCondition,
                )
            )
            .reduceOrNull { acc, item ->
                acc + item
            } ?: 0L
        return MoneyFen(
            value = result,
        )
    }

    override suspend fun getBillCountByCondition(
        queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
    ): Long {
        return TallyDb
            .database
            .billDao()
            .queryCount(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.Count,
                    queryCondition = queryCondition,
                )
            )
    }

    override fun subscribeBillAmountByCondition(
        queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
    ): Flow<MoneyFen> {
        return subscribeDataBaseTableChangedOb(
            TallyTable.Bill,
            emitOneWhileSubscribe = true,
        ).map {
            getBillAmountByCondition(
                queryCondition = queryCondition,
            )
        }
    }

    override fun subscribeMonthTotalBudget(
        bookId: String,
        typeInfo: String,
    ): Flow<MoneyFen> {
        return TallyDb
            .database
            .budgetDao()
            .subscribeTotal(
                bookId = bookId,
                typeInfo = typeInfo,
            )
            .map { budget ->
                MoneyFen(
                    value = budget?.amount ?: 0L,
                )
            }
    }

    override fun subscribeBillDetailById(
        id: String,
    ): Flow<TallyBillDetailDto?> {
        return TallyDb
            .database
            .billDao()
            .subscribeBillDetailList(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.DetailListDesc,
                    queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                        idList = listOf(id),
                    ),
                ),
            )
            .map { it.firstOrNull()?.toDto() }
    }

    override fun subscribeAssociatedRefundBillListCount(
        billId: String,
    ): Flow<Long> {
        return TallyDb
            .database
            .billDao()
            .subscribeBillCount(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.Count,
                    queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                        originBillIdList = listOf(billId),
                    ),
                ),
            )
    }

    override fun subscribeBillDetailList(
        queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
    ): Flow<List<TallyBillDetailDto>> {
        return TallyDb
            .database
            .billDao()
            .subscribeBillDetailList(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.DetailListDesc,
                    queryCondition = queryCondition,
                )
            )
            .map { list ->
                list.map { it.toDto() }
            }
    }

    override fun subscribeBillCount(
        queryCondition: TallyDataSourceSpi.Companion.BillQueryConditionDto,
    ): Flow<Long> {
        return TallyDb
            .database
            .billDao()
            .subscribeBillCount(
                rawSqlQuery = SupportBillDetailPageQueryImpl(
                    queryType = BillDetailPageQueryType.Count,
                    queryCondition = queryCondition,
                )
            )
    }

    override suspend fun insertOrUpdateBillLabelList(
        targetList: List<TallyBillLabelInsertDto>,
        isNeedSync: Boolean
    ): List<String> {
        val targetDoList = targetList.map {
            it.toDo()
        }
        TallyDb
            .database
            .billLabelDao()
            .insert(
                targetList = targetDoList,
            )
        return targetDoList.map {
            it.id
        }
    }

    override suspend fun getUnSyncBillLabelList(
        bookId: String,
        pageSize: Int,
    ): List<TallyBillLabelDto> {
        return TallyDb
            .database
            .billLabelDao()
            .getUnSyncList(
                bookId = bookId,
                pageSize = pageSize,
            ).map {
                it.toDto()
            }
    }

    override suspend fun insertOrUpdateBillImageList(
        targetList: List<TallyBillImageInsertDto>,
        isNeedSync: Boolean
    ): List<String> {
        val targetDoList = targetList.map {
            it.toDo()
        }
        TallyDb
            .database
            .billImageDao()
            .insert(
                targetList = targetDoList,
            )
        return targetDoList.map {
            it.id
        }
    }

    override suspend fun getUnSyncBillImageList(
        bookId: String,
        pageSize: Int,
    ): List<TallyBillImageDto> {
        return TallyDb
            .database
            .billImageDao()
            .getUnSyncList(
                bookId = bookId,
                pageSize = pageSize,
            ).map {
                it.toDto()
            }
    }

    override fun subscribeAllCategory(
        bookIdList: List<String>,
    ): Flow<List<TallyCategoryDto>> {
        if (bookIdList.isEmpty()) {
            notSupportError(message = "subscribeAllCategory.bookIdList 不能为空")
        }
        return TallyDb
            .database
            .categoryDao()
            .subscribeCategory(
                bookIdList = bookIdList,
            )
            .map { list ->
                list.map { it.toDto() }
            }
    }

    override suspend fun insertCategory(
        target: TallyCategoryInsertDto,
        isNeedSync: Boolean,
    ): String {
        val targetDo = target.toDo()
        TallyDb
            .database
            .categoryDao()
            .insert(
                target = targetDo,
            )
        return targetDo.id
    }

    override suspend fun insertOrUpdateCategoryList(
        targetList: List<TallyCategoryInsertDto>,
        isNeedSync: Boolean,
    ): List<String> {
        val targetDoList = targetList.map {
            it.toDo()
        }
        TallyDb
            .database
            .categoryDao()
            .insert(
                targetList = targetDoList,
            )
        return targetDoList.map {
            it.id
        }
    }

    override suspend fun updateCategory(
        target: TallyCategoryDto,
        isNeedSync: Boolean,
    ) {
        TallyDb
            .database
            .categoryDao()
            .update(
                target = target.copy(
                    isSync = false,
                ).toDo(),
            )
    }

    override suspend fun updateCategoryList(
        targetList: List<TallyCategoryDto>,
        isNeedSync: Boolean
    ) {
        TallyDb
            .database
            .categoryDao()
            .update(
                targetList = targetList.map {
                    it.copy(
                        isSync = false
                    ).toDo()
                },
            )
    }

    override suspend fun getLatestOneCategoryOrderByTimeModify(
        bookId: String,
    ): TallyCategoryDto? {
        return TallyDb
            .database
            .categoryDao()
            .getLatestOneCategoryOrderByTimeModify(
                bookId = bookId,
            )
            ?.toDto()
    }

    override suspend fun getCategoryById(
        id: String,
    ): TallyCategoryDto? {
        return TallyDb
            .database
            .categoryDao()
            .getById(
                id = id,
            )
            ?.toDto()
    }

    override suspend fun getCategoryByIdAndBookId(
        id: String,
        bookId: String
    ): TallyCategoryDto? {
        return TallyDb
            .database
            .categoryDao()
            .getByIdAndBookId(
                id = id,
                bookId = bookId,
            )
            ?.toDto()
    }

    override suspend fun getCategoryByBookId(bookId: String): List<TallyCategoryDto> {
        return TallyDb
            .database
            .categoryDao()
            .getByBookId(
                bookId = bookId,
            ).map {
                it.toDto()
            }
    }

    override suspend fun getCategoryByParentId(
        parentId: String,
    ): List<TallyCategoryDto> {
        return TallyDb
            .database
            .categoryDao()
            .getByParentId(
                parentId = parentId,
            ).map {
                it.toDto()
            }
    }

    override suspend fun getUnSyncCategoryList(
        bookId: String,
        pageSize: Int,
    ): List<TallyCategoryDto> {
        return TallyDb
            .database
            .categoryDao()
            .getUnSyncList(
                bookId = bookId,
                pageSize = pageSize,
            )
            .map {
                it.toDto()
            }
    }

    override suspend fun searchCategory(key: String): List<TallyCategoryDto> {
        return TallyDb
            .database
            .categoryDao()
            .search(
                key = key,
            )
            .map {
                it.toDto()
            }
    }

    override fun subscribeAllAccount(
        bookId: String,
    ): Flow<List<TallyAccountDto>> {
        return combine(
            TallyDb
                .database
                .accountDao()
                .subscribeAllAccount(
                    bookId = bookId,
                ),
            subscribeDataBaseTableChangedOb(
                TallyTable.Bill,
                emitOneWhileSubscribe = true,
            )
        ) { allAccount, _ ->
            allAccount
        }.map { list ->
            list.map {
                it.toDto()
            }
        }
    }

    override suspend fun insertOrUpdateAccount(
        target: TallyAccountInsertDto,
        isNeedSync: Boolean,
    ): String {
        val targetDo = target.toDo()
        TallyDb
            .database
            .accountDao()
            .insert(
                target = targetDo,
            )
        return targetDo.id
    }

    override suspend fun insertOrUpdateAccountList(
        targetList: List<TallyAccountInsertDto>,
        isNeedSync: Boolean,
    ): List<String> {
        val doList = targetList.map {
            it.toDo()
        }
        TallyDb
            .database
            .accountDao()
            .insert(
                targetList = doList,
            )
        return doList.map { it.id }
    }

    override suspend fun updateAccount(
        target: TallyAccountDto,
        isNeedSync: Boolean,
    ) {
        TallyDb
            .database
            .accountDao()
            .update(
                target = target.copy(
                    isSync = false,
                ).toDo(),
            )
    }

    override suspend fun getLatestOneAccountOrderByTimeModify(
        bookId: String,
    ): TallyAccountDto? {
        return TallyDb
            .database
            .accountDao()
            .getLatestOneAccountOrderByTimeModify(
                bookId = bookId,
            )
            ?.toDto()
    }

    override suspend fun getAccountById(id: String): TallyAccountDto? {
        return TallyDb
            .database
            .accountDao()
            .getAccountById(
                id = id,
            )
            ?.toDto()
    }

    override suspend fun getAccountByIdAndBookId(
        id: String,
        bookId: String,
    ): TallyAccountDto? {
        return TallyDb
            .database
            .accountDao()
            .getAccountByIdAndBookId(
                id = id,
                bookId = bookId,
            )
            ?.toDto()
    }

    override suspend fun updateAllAccountToNotDefaultByBookId(bookId: String) {
        TallyDb
            .database
            .accountDao()
            .updateAllToNotDefaultByBookId(
                bookId = bookId,
            )
    }

    override suspend fun getAccountByBookId(
        bookId: String,
        isExcludeDeleted: Boolean
    ): List<TallyAccountDto> {
        return TallyDb
            .database
            .accountDao()
            .getAll(
                bookId = bookId,
            )
            .filter {
                if (isExcludeDeleted) {
                    !it.isDeleted
                } else {
                    true
                }
            }.map {
                it.toDto()
            }
    }

    override suspend fun getUnSyncAccountList(
        bookId: String, pageSize: Int,
    ): List<TallyAccountDto> {
        return TallyDb
            .database
            .accountDao()
            .getUnSyncList(
                bookId = bookId,
                pageSize = pageSize,
            ).map {
                it.toDto()
            }
    }

    override suspend fun searchAccount(key: String): List<TallyAccountDto> {
        return TallyDb
            .database
            .accountDao()
            .search(
                key = key,
            ).map {
                it.toDto()
            }
    }

    override suspend fun insertLabel(
        target: TallyLabelInsertDto,
        isNeedSync: Boolean,
    ): String {
        val targetDo = target.toDo()
        TallyDb
            .database
            .labelDao()
            .insert(
                target = targetDo,
            )
        return targetDo.id
    }

    override suspend fun updateLabel(
        target: TallyLabelDto,
        isNeedSync: Boolean,
    ) {
        TallyDb
            .database
            .labelDao()
            .update(
                target = target.copy(
                    isSync = false,
                ).toDo(),
            )
    }

    override suspend fun insertOrUpdateLabelList(
        targetList: List<TallyLabelInsertDto>,
        isNeedSync: Boolean
    ): List<String> {
        val doList = targetList.map {
            it.toDo()
        }
        TallyDb
            .database
            .labelDao()
            .insert(
                targetList = doList,
            )
        return doList.map { it.id }
    }

    override suspend fun getLabel(id: String): TallyLabelDto? {
        return TallyDb
            .database
            .labelDao()
            .getLabel(
                id = id,
            )
            ?.toDto()
    }

    override suspend fun getLabelUnderBook(bookId: String, id: String): TallyLabelDto? {
        return TallyDb
            .database
            .labelDao()
            .getLabelUnderBook(
                bookId = bookId,
                id = id,
            )
            ?.toDto()
    }

    override suspend fun getAllLabelUnderBook(
        bookId: String,
        isExcludeDeleted: Boolean,
    ): List<TallyLabelDto> {
        return TallyDb
            .database
            .labelDao()
            .getAllUnderBook(
                bookId = bookId,
            ).filter {
                if (isExcludeDeleted) {
                    !it.isDeleted
                } else {
                    true
                }
            }.map { it.toDto() }
    }

    override suspend fun getLabelListByBillId(
        bookId: String,
        billId: String,
    ): List<TallyLabelDto> {
        val labelIdList = TallyDb
            .database
            .billLabelDao()
            .getListByBillId(
                bookId = bookId,
                billId = billId,
            )
            .filter {
                !it.isDeleted
            }
            .map {
                it.labelId
            }
        return TallyDb
            .database
            .labelDao()
            .getByIdList(
                idList = labelIdList,
                bookId = bookId,
            ).filter {
                !it.isDeleted
            }.map {
                it.toDto()
            }
    }

    override suspend fun getLatestOneLabelOrderByTimeModify(bookId: String): TallyLabelDto? {
        return TallyDb
            .database
            .labelDao()
            .getLatestOneLabelOrderByTimeModify(
                bookId = bookId,
            )?.toDto()
    }

    override suspend fun getUnSyncLabelList(bookId: String, pageSize: Int): List<TallyLabelDto> {
        return TallyDb
            .database
            .labelDao()
            .getUnSyncList(
                bookId = bookId,
                pageSize = pageSize,
            ).map {
                it.toDto()
            }
    }

    override suspend fun searchLabel(key: String): List<TallyLabelDto> {
        return TallyDb
            .database
            .labelDao()
            .search(
                key = key,
            ).map {
                it.toDto()
            }
    }

    override fun subscribeAllLabel(bookId: String): Flow<List<TallyLabelDto>> {
        return TallyDb
            .database
            .labelDao()
            .subscribeAllLabel(
                bookId = bookId
            ).map { list ->
                list.map {
                    it.toDto()
                }
            }
    }

    override suspend fun getBillIdListByLabelId(
        bookId: String,
        labelId: String,
        isExcludeDeleted: Boolean,
    ): List<String> {
        return TallyDb
            .database
            .billLabelDao()
            .getListByLabelId(
                bookId = bookId,
                labelId = labelId,
            )
            .filter {
                if (isExcludeDeleted) {
                    !it.isDeleted
                } else {
                    true
                }
            }
            .map {
                it.billId
            }
    }

    override suspend fun getBillIdListByLabelIdList(
        bookId: String?,
        labelIdList: List<String>,
        isExcludeDeleted: Boolean,
    ): List<String> {
        return TallyDb
            .database
            .billLabelDao()
            .getBillIdListByLabelIdList(
                labelIdList = labelIdList,
            )
            .run {
                if (bookId.isNullOrEmpty()) {
                    this
                } else {
                    this.filter {
                        it.bookId == bookId
                    }
                }
            }
            .filter {
                if (isExcludeDeleted) {
                    !it.isDeleted
                } else {
                    true
                }
            }
            .map {
                it.billId
            }
    }

    override suspend fun getLatestOneBillLabelOrderByTimeModify(
        bookId: String,
    ): TallyBillLabelDto? {
        return TallyDb
            .database
            .billLabelDao()
            .getLatestOneOrderByTimeModify(
                bookId = bookId,
            )
            ?.toDto()
    }

    override suspend fun getLatestOneBillImageOrderByTimeModify(
        bookId: String,
    ): TallyBillImageDto? {
        return TallyDb
            .database
            .billImageDao()
            .getLatestOneOrderByTimeModify(
                bookId = bookId,
            )
            ?.toDto()
    }

    override suspend fun getBillImageListByBillId(
        bookId: String,
        billId: String
    ): List<TallyBillImageDto> {
        return TallyDb
            .database
            .billImageDao()
            .getListByBookIdAndBillId(
                bookId = bookId,
                billId = billId,
            ).map {
                it.toDto()
            }
    }

    override suspend fun getBillImageList(bookId: String): List<TallyBillImageDto> {
        return TallyDb
            .database
            .billImageDao()
            .getListByBookId(
                bookId = bookId,
            ).map {
                it.toDto()
            }
    }

    override suspend fun getBillIdListInImage(
        bookId: String?,
        isExcludeDeleted: Boolean,
    ): List<String> {
        return TallyDb
            .database
            .billImageDao()
            .run {
                if (bookId.isNullOrEmpty()) {
                    if (isExcludeDeleted) {
                        this.getAllNoDeletedBillId()
                    } else {
                        this.getAllBillId()
                    }
                } else {
                    if (isExcludeDeleted) {
                        this.getAllNoDeletedBillIdByBookId(
                            bookId = bookId,
                        )
                    } else {
                        this.getAllBillIdByBookId(
                            bookId = bookId,
                        )
                    }
                }
            }

    }

    override suspend fun insertBillChat(target: BillChatInsertDto): Long {
        val targetDo = target.toDo()
        return TallyDb
            .database
            .billChatDao()
            .insert(
                target = targetDo,
            )
    }

    override suspend fun getBillChatDayTimeList(
        bookId: String,
        pageStartIndex: Long,
        pageSize: Long,
    ): List<Long> {
        return TallyDb
            .database
            .billChatDao()
            .getTimeList(
                bookId = bookId,
                pageStartIndex = pageStartIndex,
                pageSize = pageSize,
            )
    }

    override suspend fun getBillChatById(id: Long): BillChatDto? {
        return TallyDb
            .database
            .billChatDao()
            .getById(
                id = id,
            )?.toDto()
    }

    override suspend fun deleteBillChatById(id: Long) {
        TallyDb
            .database
            .billChatDao()
            .deleteById(
                id = id,
            )
    }

    override suspend fun updateBillChat(target: BillChatDto) {
        TallyDb
            .database
            .billChatDao()
            .update(
                target = target.toDo(),
            )
    }

    override suspend fun updateAllInitBillChatToFail() {
        TallyDb
            .database
            .billChatDao()
            .updateAllInitToFail()
    }

    override suspend fun getBillChatList(
        bookId: String,
        afterTime: Long,
    ): List<BillChatDto> {
        return TallyDb
            .database
            .billChatDao()
            .getListAfterTimeByBookId(
                bookId = bookId,
                afterTime = afterTime,
            ).map {
                it.toDto()
            }
    }

    private fun Collection<String>.parseToTallyTable(): List<TallyTable> {
        return this.mapNotNull {
            when (it) {
                BookDao.TableName -> TallyTable.Book
                CategoryDao.TableName -> TallyTable.Category
                AccountDao.TableName -> TallyTable.Account
                LabelDao.TableName -> TallyTable.Label
                BillDao.TableName -> TallyTable.Bill
                BillImageDao.TableName -> TallyTable.BillImage
                BillLabelDao.TableName -> TallyTable.BillLabel
                BillChatDao.TableName -> TallyTable.BillChat
                else -> null
            }
        }
    }

    init {

        LogSupport.d(
            tag = TallyDataSourceSpi.TAG,
            content = "初始化了",
        )

        TallyDb
            .database
            .invalidationTracker
            .addObserver(
                object : InvalidationTracker.Observer(
                    totalTableNames
                        .toTypedArray()
                ) {
                    override fun onInvalidated(tables: Set<String>) {
                        dataBaseTablesChangedEventOb.add(
                            value = tables.parseToTallyTable()
                        )
                    }
                }
            )

    }

}