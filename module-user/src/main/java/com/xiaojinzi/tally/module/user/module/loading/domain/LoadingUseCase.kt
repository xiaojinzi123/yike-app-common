package com.xiaojinzi.tally.module.user.module.loading.domain

import android.content.Context
import android.util.Log
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.reactive.template.domain.DialogUseCase
import com.xiaojinzi.support.activity_stack.ActivityStack
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.await
import com.xiaojinzi.support.ktx.findException
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.tally.lib.res.model.exception.CommonBusinessException
import com.xiaojinzi.tally.lib.res.ui.APP_ACTIVITY_FLAG_MAIN
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.LocalLedgerIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

sealed class LoadingIntent {

    data class GO(
        @UiContext val context: Context,
    ) : LoadingIntent()

}

@ViewModelLayer
interface LoadingUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class LoadingUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), LoadingUseCase {

    private suspend fun goNext(
        @UiContext context: Context,
    ) {
        // 先完成旧账本身份迁移，不能用默认标识抢先初始化空数据库。
        val ownerId = LocalLedgerIdentity.requiredId()
        val databaseReady = withTimeoutOrNull(10_000) {
            AppServices.tallyDataSourceInitSpi.initTallyDataBase(userId = ownerId)
            AppServices.tallyDataSourceInitSpi.isInitStateOb.first { it }
        } == true
        if (!databaseReady) {
            throw CommonBusinessException(message = "本地账本尚未准备好，请稍后重试")
        }
        // 如果主界面存在, 就关闭当前界面, 否则就启动一个
        val isMainViewExist = ActivityStack.any {
            it.hasFlag(
                flag = APP_ACTIVITY_FLAG_MAIN,
            )
        }
        if (isMainViewExist) {
            // 不然太快了, 会闪
            delay(800)
            context.tryFinishActivity()
        } else {
            // 只有路由成功才关闭加载页，失败留给调用方显示重试。
            Router.with(context)
                .hostAndPath(AppRouterConfig.MAIN_MAIN)
                .await()
            context.tryFinishActivity()
        }
    }

    @IntentProcess
    private suspend fun go(
        intent: LoadingIntent.GO,
    ) {
        while (true) {
            try {
                goNext(
                    context = intent.context,
                )
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LoadingUseCase", "启动失败，保留加载页等待重试", e)
                val message = e.findException(
                    targetClass = CommonBusinessException::class,
                )?.message ?: "暂时无法打开账本，请重试；若持续失败，请重新启动应用"
                // 复用现有业务对话框，关闭弹窗后仍保留错误和重试入口。
                while (true) {
                    val result = confirmDialog(
                        title = "启动失败".toStringItemDto(),
                        content = message.toStringItemDto(),
                        negative = null,
                        positive = "重试".toStringItemDto(),
                    )
                    if (result == DialogUseCase.ConfirmDialogResultType.CONFIRM) {
                        break
                    }
                }
            }
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}