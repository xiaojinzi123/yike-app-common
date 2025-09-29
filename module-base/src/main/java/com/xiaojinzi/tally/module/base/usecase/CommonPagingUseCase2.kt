package com.xiaojinzi.tally.module.base.usecase

import androidx.annotation.Keep
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xiaojinzi.reactive.domain.BaseUseCase
import com.xiaojinzi.reactive.view.ViewUseCase
import com.xiaojinzi.support.annotation.HotObservable
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.timeAtLeast
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.support.DevelopHelper
import com.xiaojinzi.tally.module.base.view.compose.AppCommonEmptyDataView
import com.xiaojinzi.tally.module.base.view.compose.AppCommonErrorDataView
import com.xiaojinzi.tally.module.base.view.compose.AppCommonInitDataView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

const val TAG_CommonPagingUseCase2 = "CommonRefreshUseCase2"

/**
 * 用户交互的接口
 * 重试、刷新、加载更多
 */
interface UserInteraction {

    /**
     * 重试
     */
    fun retry()

    /**
     * 刷新
     */
    fun refresh()

    /**
     * 加载更多
     */
    fun loadMore()

}

interface PageDataFetch<T, K> {

    data class LoadDataResult<T, K>(
        val data: List<T>,
        val nextKey: K?,
    )

    /**
     * 加载数据, 允许抛异常
     */
    @Throws(Exception::class)
    suspend fun loadData(currentKey: K?, pageSize: Int): LoadDataResult<T, K>

}

/**
 * 分页的状态
 */
enum class PagingState1 {

    // 无
    NONE,

    // 初始化
    INITING,

    // 初始化失败
    INIT_FAILED,

    // 初始化成功
    INIT_SUCCESS,

    // 刷新中
    REFRESHING,

    // 刷新成功
    REFRESH_SUCCESS,

    // 刷新失败
    REFRESH_FAILED,

    // 加载更多中
    LOADING_MORE,

    // 加载更多成功
    LOAD_MORE_SUCCESS,

    // 加载更多失败
    LOAD_MORE_FAILED,

    // 加载更多完毕
    LOAD_MORE_END,
    ;

    companion object {
        val SHOW_CONTENT_LIST = listOf(
            INIT_SUCCESS,
            REFRESHING,
            REFRESH_FAILED,
            REFRESH_SUCCESS,
            LOADING_MORE,
            LOAD_MORE_SUCCESS,
            LOAD_MORE_FAILED,
            LOAD_MORE_END,
        )
    }

    val isShowList: Boolean
        get() = this in SHOW_CONTENT_LIST

    val isInit: Boolean
        get() = this == INITING

    val isInitFail: Boolean
        get() = this == INIT_FAILED

    val isRefreshing: Boolean
        get() = this == REFRESHING

    val isLoadMore: Boolean
        get() = this == LOADING_MORE

    val isLoadMoreFail: Boolean
        get() = this == LOAD_MORE_FAILED

    val isLoadMoreEnd: Boolean
        get() = this == LOAD_MORE_END

}

@Keep
data class PagingCarrier<T>(
    private val userInteraction: UserInteraction = object : UserInteraction {
        override fun retry() {
        }

        override fun refresh() {
        }

        override fun loadMore() {
        }
    },
    val state: PagingState1 = PagingState1.NONE,
    val dataList: List<T> = emptyList<T>(),
) : UserInteraction // 占位
{

    fun <R> transform(transform: (item: T) -> R): PagingCarrier<R> {
        return PagingCarrier(
            userInteraction = userInteraction,
            state = state,
            dataList = dataList.map { transform(it) },
        )
    }

    fun <R> convert(transform: (item: List<T>) -> List<R>): PagingCarrier<R> {
        return PagingCarrier(
            userInteraction = userInteraction,
            state = state,
            dataList = transform(dataList),
        )
    }

    override fun retry() {
        userInteraction.retry()
    }

    override fun refresh() {
        userInteraction.refresh()
    }

    override fun loadMore() {
        userInteraction.loadMore()
    }

    init {

        if (DevelopHelper.isDevelop) {
            /*tickerFlow(period = 1000)
                .map { state }
                .distinctUntilChanged()
                .onEach {
                    LogSupport.d(
                        tag = TAG_CommonPagingUseCase2,
                        content = "监听 state = $it"
                    )
                }
                .launchIn(scope = AppScope)*/
        }

    }

}

fun <T> defaultPagingCarrier(): PagingCarrier<T> = PagingCarrier()

abstract class PagingImpl<T, K>(
    private val pageSize: Int = 20,
    private val loadingTimeAtLeast: Long = 500L,
) : PageDataFetch<T, K> {

    private var scope: CoroutineScope? = null

    private val userInteraction = object : UserInteraction {
        override fun retry() {
            doRetry()
        }

        override fun refresh() {
            doRefresh()
        }

        override fun loadMore() {
            doLoadMore()
        }

    }

    private val defaultPagingInfo = PagingCarrier<T>(
        userInteraction = userInteraction,
        state = PagingState1.NONE,
        dataList = emptyList(),
    )

    private val initPagingInfo = PagingCarrier<T>(
        userInteraction = userInteraction,
        state = PagingState1.INITING,
        dataList = emptyList(),
    )

    private val pagingCarrierObservable: MutableSharedStateFlow<PagingCarrier<T>> =
        MutableSharedStateFlow(initValue = defaultPagingInfo)

    val flow: Flow<PagingCarrier<T>>
        get() = pagingCarrierObservable
            .onStart { }
            .onEach {
                launch()
            }

    private var isBusy = false
    private var currentKey: K? = null

    @Synchronized
    private fun doRetry() {
        LogSupport.d(
            tag = TAG_CommonPagingUseCase2,
            content = "doRetry isBusy = $isBusy",
        )
        if (isBusy) {
            return
        }
        isBusy = true
        scope?.launch(Dispatchers.IO) {
            val currentPagingInfo = pagingCarrierObservable.first()
            if (currentPagingInfo.state in listOf(
                    PagingState1.NONE,
                    PagingState1.INIT_FAILED,
                )
            ) {
                val dataList = currentPagingInfo.dataList.toMutableList()
                if (currentPagingInfo.state != PagingState1.INITING) {
                    pagingCarrierObservable.emit(value = initPagingInfo)
                }
                dataList.clear()
                try {
                    val loadResult = timeAtLeast(
                        timeMillis = loadingTimeAtLeast,
                    ) {
                        loadData(
                            currentKey = null,
                            pageSize = pageSize,
                        )
                    }
                    currentKey = loadResult.nextKey
                    dataList.addAll(loadResult.data)
                    val state = if (loadResult.data.size < pageSize) {
                        PagingState1.LOAD_MORE_END
                    } else {
                        PagingState1.INIT_SUCCESS
                    }
                    pagingCarrierObservable.emit(
                        value = PagingCarrier(
                            userInteraction = userInteraction,
                            state = state,
                            dataList = dataList,
                        )
                    )
                } catch (e: Exception) {
                    pagingCarrierObservable.emit(
                        value = PagingCarrier(
                            userInteraction = userInteraction,
                            state = PagingState1.INIT_FAILED,
                            dataList = dataList,
                        )
                    )
                }
            }
        }?.invokeOnCompletion { isBusy = false } ?: run {
            isBusy = false
        }
    }

    @Synchronized
    private fun doRefresh() {
        LogSupport.d(
            tag = TAG_CommonPagingUseCase2,
            content = "doRefresh isBusy = $isBusy",
        )
        if (isBusy) {
            return
        }
        isBusy = true
        scope?.launch(Dispatchers.IO) {
            val currentPagingInfo = pagingCarrierObservable.first()
            if (currentPagingInfo.state in listOf(
                    PagingState1.NONE,
                    PagingState1.INIT_SUCCESS,
                    PagingState1.REFRESH_FAILED,
                    PagingState1.REFRESH_SUCCESS,
                    PagingState1.LOAD_MORE_FAILED,
                    PagingState1.LOAD_MORE_SUCCESS,
                    PagingState1.LOAD_MORE_END,
                )
            ) {
                LogSupport.d(
                    tag = TAG_CommonPagingUseCase2,
                    content = "doRefresh need refresh",
                )
                val dataList = currentPagingInfo.dataList.toMutableList()
                pagingCarrierObservable.emit(
                    value = currentPagingInfo.copy(
                        state = PagingState1.REFRESHING,
                    )
                )
                try {
                    val loadResult = timeAtLeast(
                        timeMillis = loadingTimeAtLeast,
                    ) {
                        loadData(
                            currentKey = null,
                            pageSize = pageSize,
                        )
                    }
                    currentKey = loadResult.nextKey
                    dataList.clear()
                    dataList.addAll(loadResult.data)
                    val state = if (loadResult.data.size < pageSize) {
                        PagingState1.LOAD_MORE_END
                    } else {
                        PagingState1.REFRESH_SUCCESS
                    }
                    pagingCarrierObservable.emit(
                        value = PagingCarrier(
                            userInteraction = userInteraction,
                            state = state,
                            dataList = dataList,
                        )
                    )
                } catch (e: Exception) {
                    pagingCarrierObservable.emit(
                        value = PagingCarrier(
                            userInteraction = userInteraction,
                            state = PagingState1.REFRESH_FAILED,
                            dataList = dataList,
                        )
                    )
                }
            } else {
                LogSupport.d(
                    tag = TAG_CommonPagingUseCase2,
                    content = "doRefresh not need refresh, state = ${currentPagingInfo.state}",
                )
            }
        }?.invokeOnCompletion { isBusy = false } ?: run {
            isBusy = false
        }
    }

    @Synchronized
    private fun doLoadMore() {
        LogSupport.d(
            tag = TAG_CommonPagingUseCase2,
            content = "doLoadMore isBusy = $isBusy",
        )
        if (isBusy) {
            return
        }
        isBusy = true
        scope?.launch(Dispatchers.IO) {
            val currentPagingInfo = pagingCarrierObservable.first()
            if (currentPagingInfo.state in listOf(
                    PagingState1.INIT_SUCCESS,
                    PagingState1.REFRESH_SUCCESS,
                    PagingState1.LOAD_MORE_SUCCESS,
                )
            ) {
                val dataList = currentPagingInfo.dataList.toMutableList()
                pagingCarrierObservable.emit(
                    value = currentPagingInfo.copy(
                        state = PagingState1.LOADING_MORE,
                    )
                )
                try {
                    val loadResult = timeAtLeast(
                        timeMillis = loadingTimeAtLeast,
                    ) {
                        loadData(
                            currentKey = currentKey,
                            pageSize = pageSize,
                        )
                    }
                    currentKey = loadResult.nextKey
                    dataList.addAll(loadResult.data)
                    val state = if (loadResult.data.size < pageSize) {
                        PagingState1.LOAD_MORE_END
                    } else {
                        PagingState1.LOAD_MORE_SUCCESS
                    }
                    pagingCarrierObservable.emit(
                        value = PagingCarrier(
                            userInteraction = userInteraction,
                            state = state,
                            dataList = dataList,
                        )
                    )
                } catch (e: Exception) {
                    pagingCarrierObservable.emit(
                        value = PagingCarrier(
                            userInteraction = userInteraction,
                            state = PagingState1.LOAD_MORE_FAILED,
                            dataList = dataList,
                        )
                    )
                }
            }
        }?.invokeOnCompletion { isBusy = false } ?: run {
            isBusy = false
        }
    }

    @Synchronized
    private fun launch() {
        LogSupport.d(
            tag = TAG_CommonPagingUseCase2,
            content = "launch 方法被调用",
        )
        if (scope == null) {
            LogSupport.d(
                tag = TAG_CommonPagingUseCase2,
                content = "launch 创建 scope",
            )
            val tempScope = MainScope()
            // 监听数量
            pagingCarrierObservable
                .subscriptionCount
                .onEach { count ->
                    LogSupport.d(
                        tag = TAG_CommonPagingUseCase2,
                        content = "launch count = $count",
                    )
                    if (count == 0) {
                        LogSupport.d(
                            tag = TAG_CommonPagingUseCase2,
                            content = "launch 销毁",
                        )
                        scope?.cancel()
                        scope = null
                    }
                }
                .launchIn(scope = tempScope)
            scope = tempScope
            doRetry()
        }
    }

    /**
     * 更新 item
     */
    suspend fun updateItem(index: Int, item: T) {
        val oldValue = pagingCarrierObservable.first()
        pagingCarrierObservable.emit(
            value = oldValue.copy(
                dataList = oldValue.dataList.toMutableList().apply {
                    this[index] = item
                }
            )
        )
    }

    /**
     * 添加 item
     */
    suspend fun addItem(index: Int, item: T) {
        val oldValue = pagingCarrierObservable.first()
        pagingCarrierObservable.emit(
            value = oldValue.copy(
                dataList = oldValue.dataList.toMutableList().apply {
                    this.add(index, item)
                }
            )
        )
    }

    /**
     * 删除 item
     */
    suspend fun removeItem(index: Int? = null, item: T? = null): Boolean {
        var isRemove: Boolean

        if (index == null && item == null) {
            throw IllegalArgumentException("index 和 item 不能同时为空")
        }
        val oldValue = pagingCarrierObservable.first()

        pagingCarrierObservable.emit(
            value = oldValue.copy(
                dataList = oldValue.dataList.toMutableList().apply {
                    val data = if (index != null) {
                        this[index]
                    } else {
                        item
                    }
                    isRemove = this.remove(data)
                }
            )
        )

        return isRemove
    }

    suspend fun convertItems(transform: suspend (List<T>) -> List<T>) {
        val oldValue = pagingCarrierObservable.first()
        pagingCarrierObservable.emit(
            value = oldValue.copy(
                dataList = transform(oldValue.dataList)
            )
        )
    }

}

abstract class PagingCommonKeyImpl<T, K>(
    pageSize: Int = 20,
) : PagingImpl<T, K>(
    pageSize = pageSize,
) {

    abstract suspend fun getNextKey(
        currentKey: K?,
        pageList: List<T>,
    ): K?

    abstract suspend fun loadDataAdapter(
        currentKey: K?,
        pageSize: Int,
    ): List<T>

    final override suspend fun loadData(
        currentKey: K?,
        pageSize: Int,
    ): PageDataFetch.LoadDataResult<T, K> {
        LogSupport.d(
            tag = TAG_CommonPagingUseCase2,
            content = "loadData, currentIntKey = $currentKey, pageSize = $pageSize",
        )
        val pageList = loadDataAdapter(
            currentKey = currentKey,
            pageSize = pageSize,
        ).apply {
            LogSupport.d(
                tag = TAG_CommonPagingUseCase2,
                content = "loadData loadDataAdapter, 加载的数量 = ${this.size}",
            )
        }
        return PageDataFetch.LoadDataResult(
            data = pageList,
            nextKey = getNextKey(
                currentKey = currentKey,
                pageList = pageList,
            ),
        )
    }

}

abstract class PagingIntKeyImpl<T>(
    pageSize: Int = 20,
) : PagingImpl<T, Int>(
    pageSize = pageSize,
) {

    abstract suspend fun loadDataAdapter(
        currentKey: Int,
        pageSize: Int,
    ): List<T>

    final override suspend fun loadData(
        currentKey: Int?,
        pageSize: Int,
    ): PageDataFetch.LoadDataResult<T, Int> {
        val currentIntKey = currentKey ?: 1
        LogSupport.d(
            tag = TAG_CommonPagingUseCase2,
            content = "loadData, currentIntKey = $currentIntKey, pageSize = $pageSize",
        )
        return PageDataFetch.LoadDataResult(
            data = loadDataAdapter(
                currentKey = currentIntKey,
                pageSize = pageSize,
            ).apply {
                LogSupport.d(
                    tag = TAG_CommonPagingUseCase2,
                    content = "loadData loadDataAdapter, 加载的数量 = ${this.size}",
                )
            },
            nextKey = currentIntKey + 1,
        )
    }

}

interface CommonRefreshEventUseCase2 : BaseUseCase {

    @HotObservable(HotObservable.Pattern.PUBLISH, isShared = true)
    val refreshEventObservableDto: Flow<Unit>

    @HotObservable(HotObservable.Pattern.PUBLISH, isShared = true)
    val reloadEventObservableDto: Flow<Unit>

    /**
     * 发送事件
     */
    fun postRefreshEvent()

    /**
     * 发送事件
     */
    fun postReloadEvent()

}

interface CommonPagingUseCase2<T : Any> : BaseUseCase {

    /**
     * 数据列表
     */
    @HotObservable(HotObservable.Pattern.BEHAVIOR, isShared = true)
    val dataListObservableDto: Flow<PagingCarrier<T>>

}

@ViewLayer
interface CommonPagingViewUseCase2<T : Any> : ViewUseCase {

    /**
     * 数据列表
     */
    @HotObservable(HotObservable.Pattern.BEHAVIOR, isShared = true)
    val dataListObservableVo: Flow<PagingCarrier<T>>

}