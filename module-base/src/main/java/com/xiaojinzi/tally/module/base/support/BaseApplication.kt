package com.xiaojinzi.tally.module.base.support

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xiaojinzi.component.Component
import com.xiaojinzi.component.Config
import com.xiaojinzi.component.error.ignore.ErrorIgnore
import com.xiaojinzi.component.support.ASMUtil
import com.xiaojinzi.reactive.template.ReactiveTemplate
import com.xiaojinzi.support.activity_stack.ActivityStack
import com.xiaojinzi.support.init.AppInstance
import com.xiaojinzi.support.init.CheckInit
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.exception.CommonBusinessException
import com.xiaojinzi.tally.module.base.ktx.getProcessName
import com.xiaojinzi.tally.module.base.view.compose.commonAppToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TODO:
 * 1. 日历视图感觉是可以优化的,
 * [com.xiaojinzi.tally.module.main.module.main.calendar.domain.CalendarUseCase.subscribeMonthDayList] 这个方法的使用可以被优化的感觉
 */
abstract class BaseApplication : Application() {

    abstract val tag: String

    abstract val debug: Boolean

    abstract val applicationIdFormat: String

    override fun attachBaseContext(baseContext: Context) {
        super.attachBaseContext(baseContext)
        AppInstance.app = this
        AppInstance.isDebug = debug
        LogSupport.logAble = debug
        DevelopHelper.init(debug)
    }

    override fun onCreate() {
        super.onCreate()

        /*if (!WorkManager.isInitialized()) {
            WorkManager.initialize(
                app,
                androidx.work.Configuration.Builder().build(),
            )
        }*/

        val processName = this.getProcessName()
        LogSupport.d(
            tag = tag,
            content = "初始化的进程名称：$processName",
        )
        if (processName != packageName) {
            LogSupport.d(
                tag = tag,
                content = "不是主进程, 不给初始化",
            )
            return
        }

        // 阻止系统恢复
        CheckInit.init(
            app = this,
            bootActAction = "app_boot_$applicationIdFormat",
            bootActCategory = Intent.CATEGORY_DEFAULT,
            rebootActAction = "app_reboot_$applicationIdFormat",
            rebootActCategory = Intent.CATEGORY_DEFAULT,
        )

        ActivityStack.init(
            this, debug,
        )

        ReactiveTemplate.config(
            isDebug = debug,
            errorCustom = { error ->
                when (error) {

                    is CommonBusinessException -> {
                        error.message.orNull()?.toStringItemDto()
                    }

                    else -> null
                }
            },
            errorCustomIgnore = { error ->
                when {
                    ErrorIgnore.isIgnore(
                        throwable = error,
                    ) -> {
                        true
                    }

                    else -> false
                }
            }
        )

        LogSupport.d(
            tag = tag,
            content = "所有模块: ${ASMUtil.getModuleNames().joinToString()}",
        )

        // 初始化组件化
        Component.init(
            application = this,
            isDebug = debug,
            config = Config.Builder()
                .initRouterAsync(true)
                .errorCheck(true)
                .optimizeInit(true)
                .autoRegisterModule(true)
                .build()
        )

        // 由进程启动恢复本地账本，桌面组件冷启动也不依赖页面或账号流程。
        AppScope.launch(Dispatchers.IO) {
            try {
                val ownerId = LocalLedgerIdentity.requiredId()
                // 初始化入口在本地模板和选中账本准备完成后才报告数据库可用。
                AppServices.tallyDataSourceInitSpi.initTallyDataBase(
                    userId = ownerId,
                )
                AppServices.appWidgetSpi?.startWidgetServiceIfUsed()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "恢复本地账本失败", e)
                withContext(Dispatchers.Main) {
                    commonAppToast(
                        content = "恢复本地账本失败，请重新启动应用后重试",
                        isShort = false,
                    )
                }
            }
        }

    }

}