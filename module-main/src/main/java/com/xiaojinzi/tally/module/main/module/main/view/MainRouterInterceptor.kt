package com.xiaojinzi.tally.module.main.module.main.view

import android.util.Log
import com.xiaojinzi.component.impl.RouterInterceptor
import com.xiaojinzi.component.impl.RouterResult
import com.xiaojinzi.tally.lib.res.model.exception.CommonBusinessException
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.view.compose.commonAppToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 等待应用完成本地数据库初始化，再进入已选择的账本。 */
class MainRouterInterceptor : RouterInterceptor {

    override suspend fun intercept(chain: RouterInterceptor.Chain): RouterResult {
        try {
            val isReady = withTimeoutOrNull(10_000L) {
                AppServices.tallyDataSourceInitSpi.isInitStateOb.first { it }
                // 数据源构造时会访问数据库，必须在就绪之后获取。
                val dataSource = AppServices.tallyDataSourceSpi
                dataSource.resetSelectedBook()
                dataSource.selectedBookStateOb.filterNotNull().first()
                true
            } ?: false
            if (!isReady) {
                throw CommonBusinessException(
                    message = "本地账本初始化超时，请重试；若持续失败，请重新启动应用",
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = if (e is CommonBusinessException) e else CommonBusinessException(
                message = "读取本地账本失败，请重试",
                cause = e,
            )
            Log.e("MainRouterInterceptor", "进入首页前检查本地账本失败", error)
            commonAppToast(
                content = error.message ?: "读取本地账本失败，请重试",
                isShort = false,
            )
            throw error
        }
        return chain.proceed(request = chain.request())
    }

}
