package com.xiaojinzi.tally.module.base.support

import android.app.Application
import android.content.Context
import android.content.Intent
import com.xiaojinzi.component.Component
import com.xiaojinzi.component.Config
import com.xiaojinzi.component.error.ignore.ErrorIgnore
import com.xiaojinzi.component.support.ASMUtil
import com.xiaojinzi.module.common.base.support.CommonServices
import com.xiaojinzi.reactive.template.ReactiveTemplate
import com.xiaojinzi.support.activity_stack.ActivityStack
import com.xiaojinzi.support.init.AppInstance
import com.xiaojinzi.support.init.CheckInit
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.launchIgnoreError
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.exception.CommonBusinessException
import com.xiaojinzi.tally.lib.res.model.network.AppNetworkException
import com.xiaojinzi.tally.module.base.ktx.getProcessName
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

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

                    is AppNetworkException -> {
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

        // 其他初始化
        AppScope.launchIgnoreError {
            // 等待同意隐私协议
            AppServices
                .appConfigSpi
                .isAgreedPrivacyAgreementStateOb
                .filter { it }
                .first()
            val latestUserId = AppServices
                .userSpi
                .latestUserIdStateOb
                .firstOrNull()
            // 说明没登录过或者退出登录了
            if (latestUserId.isNullOrBlank()) {
                // 不做任何初始化
            } else {
                // 登陆过, 但是可能用户信息为空
                // 初始化数据库
                AppServices
                    .tallyDataSourceInitSpi
                    .initTallyDataBase(
                        userId = latestUserId,
                    )
                // 销毁一些使用了数据库的服务发现
                AppServices
                    .destroySpiAboutTallyDatabase()
                // 启动 更新 widget 的 服务
                AppServices
                    .appWidgetSpi?.apply {
                        this.startWidgetServiceIfUsed()
                    }
            }
            // 初始化阿里云 OSS
            CommonServices.aliOssSpi?.init(
                endpoint = "oss-cn-hangzhou.aliyuncs.com",
                bucket = "tally2",
            )
            // 微信相关初始化
            CommonServices
                .wxSpi
                ?.init(
                    appId = AppServices.appInfoSpi.wxAppId,
                )
        }

    }

}