package com.xiaojinzi.tally.module.user.spi

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.xiaojinzi.component.anno.ServiceAnno
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.lib.common.res.wx.sdk.WXLoginSuccessDto
import com.xiaojinzi.module.common.base.spi.spObjectConverterPersistence
import com.xiaojinzi.module.common.base.spi.spPersistence
import com.xiaojinzi.module.common.base.support.CommonServices
import com.xiaojinzi.support.activity_stack.ActivityStack
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.SuspendAction0
import com.xiaojinzi.support.ktx.commonTimeFormat1
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.suspendAction0
import com.xiaojinzi.support.ktx.tickerFlow
import com.xiaojinzi.tally.lib.res.model.exception.NotLoggedInException
import com.xiaojinzi.tally.lib.res.model.exception.UserIdDoesNotMatchTheLastLoginException
import com.xiaojinzi.tally.lib.res.model.exception.WxNotInstallException
import com.xiaojinzi.tally.lib.res.model.user.ThirdLoginBindPhoneException
import com.xiaojinzi.tally.lib.res.model.user.UserInfoDto
import com.xiaojinzi.tally.lib.res.model.user.UserVipResDto
import com.xiaojinzi.tally.lib.res.ui.APP_ACTIVITY_FLAG_LOGIN
import com.xiaojinzi.tally.module.base.spi.UserSpi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.isInstallWx
import com.xiaojinzi.tally.module.user.module.my.view.MyView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@ServiceAnno(UserSpi::class)
class UserSpiImpl : UserSpi {

    override val userTokenStateOb = MutableSharedStateFlow<String?>()
        .spPersistence(
            scope = AppScope,
            key = "userToken",
            def = null,
        )

    override val userTokenNow: String?
        get() = userTokenStateOb.value

    override val userInfoStateOb = MutableSharedStateFlow<UserInfoDto?>()
        .spObjectConverterPersistence(
            scope = AppScope,
            key = "userInfo",
            def = null,
        )

    override val isLoginStateOb = userInfoStateOb
        .map {
            it != null
        }

    override val latestUserIdStateOb = MutableSharedStateFlow<String?>()
        .spPersistence(
            scope = AppScope,
            key = "latestUserId",
            def = null,
        )

    override val vipInfoStateOb = MutableSharedStateFlow<UserVipResDto?>()
        .spObjectConverterPersistence(
            scope = AppScope,
            key = "userVipInfo",
            def = null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isVipStateOb = vipInfoStateOb
        .flatMapLatest { vipInfo ->
            tickerFlow(period = 60 * 1000)
                .map {
                    (vipInfo?.expiredTime ?: 0) > System.currentTimeMillis()
                }
        }

    @OptIn(
        InternalCoroutinesApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
    )
    @Composable
    override fun MyViewShared() {
        MyView()
    }

    override suspend fun requiredUserInfo(): UserInfoDto {
        return userInfoStateOb.firstOrNull() ?: throw NotLoggedInException()
    }

    override suspend fun requiredLastUserId(): String {
        return latestUserIdStateOb.firstOrNull() ?: throw NotLoggedInException()
    }

    override suspend fun loginByCheckCode(
        phoneNumber: String,
        checkCode: String,
    ) {
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "loginByCheckCode start",
        )
        val loginResult = AppServices.appNetworkSpi.loginByCheckCode(
            phoneNumber = phoneNumber,
            checkCode = checkCode,
        )
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "loginByCheckCode afterLogin start, tokenInfo = ${loginResult.tokenInfo}, userInfo = ${loginResult.userInfo}",
        )
        afterLogin(
            userToken = loginResult.tokenInfo.token,
            userInfo = loginResult.userInfo,
        )
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "loginByCheckCode afterLogin end",
        )
    }

    override suspend fun loginByBindWx(
        authId: String,
        phoneNumber: String,
        checkCode: String,
    ) {
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "loginByBindWx start",
        )
        val loginResult = AppServices.appNetworkSpi.loginByBindWx(
            authId = authId,
            phoneNumber = phoneNumber,
            checkCode = checkCode,
        )
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "loginByBindWx afterLogin start, tokenInfo = ${loginResult.tokenInfo}, userInfo = ${loginResult.userInfo}",
        )
        afterLogin(
            userToken = loginResult.tokenInfo.token,
            userInfo = loginResult.userInfo,
        )
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "loginByBindWx afterLogin end",
        )
    }

    override suspend fun loginByWx() {
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "loginByWx start",
        )
        if (!isInstallWx()) {
            throw WxNotInstallException()
        }
        when (
            val result = CommonServices
                .wxLoginSpi
                ?.login()
        ) {
            is WXLoginSuccessDto -> {
                LogSupport.d(
                    tag = UserSpi.TAG,
                    content = "loginByWx 微信授权 code 成功, code = ${result.code}",
                )
                val wxResult = AppServices
                    .appNetworkSpi
                    .loginByWx(
                        wxCode = result.code,
                    )
                LogSupport.d(
                    tag = UserSpi.TAG,
                    content = "loginByWx 微信登录结果: $wxResult",
                )
                when {
                    wxResult.loginResult != null -> {
                        LogSupport.d(
                            tag = UserSpi.TAG,
                            content = "loginByWx 微信登录成功",
                        )
                        afterLogin(
                            userToken = wxResult.loginResult!!.tokenInfo.token,
                            userInfo = wxResult.loginResult!!.userInfo,
                        )
                    }

                    wxResult.authId != null -> {
                        LogSupport.d(
                            tag = UserSpi.TAG,
                            content = "loginByWx 需要绑定手机号",
                        )
                        throw ThirdLoginBindPhoneException(
                            authId = wxResult.authId!!,
                        )
                    }

                    else -> throw RuntimeException()
                }
            }

            else -> {
                LogSupport.d(
                    tag = UserSpi.TAG,
                    content = "loginByWx 微信授权失败",
                )
                throw RuntimeException()
            }
        }
    }

    private suspend fun afterLogin(
        userToken: String,
        userInfo: UserInfoDto,
    ) {
        val lastUserId = latestUserIdStateOb.firstOrNull().orNull()
        // 如果登录的和上次不同, 是不行的
        if (lastUserId != null && lastUserId != userInfo.id) {
            throw UserIdDoesNotMatchTheLastLoginException()
        }
        // 初始化数据库
        AppServices
            .tallyDataSourceInitSpi
            .initTallyDataBase(
                userId = userInfo.id,
            )
        // 销毁一些使用了数据库的服务发现
        AppServices
            .destroySpiAboutTallyDatabase()
        userTokenStateOb.emit(
            value = userToken,
        )
        userInfoStateOb.emit(
            value = userInfo,
        )
        latestUserIdStateOb.emit(
            value = userInfo.id,
        )
    }

    override suspend fun clearUserInfo() {
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "clearUserInfo start",
        )
        userTokenStateOb.emit(
            value = null,
        )
        userInfoStateOb.emit(
            value = null,
        )
        vipInfoStateOb.emit(
            value = null,
        )
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "clearUserInfo end",
        )
    }

    override suspend fun logoutForBusinessLogic() {
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "logoutForBusinessLogic start",
        )
        // 等待同步完成, 不想影响数据同步. 现在不会影响了. 管他成功失败呢
        /*AppServices
            ?.tallyDataSyncSpi
            ?.isSyncingStateOb
            ?.filter { !it }
            ?.first()*/
        // 设置空账本
        AppServices
            .tallyDataSourceSpi
            .setNullSelectedBook()
        // 通知后台退出登录
        val logoutResult = runCatching {
            AppServices
                .appNetworkSpi
                .logout()
        }
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "logoutResult: ${logoutResult.isSuccess}",
        )
        // 关闭同步
        AppServices
            .tallyDataSyncSpi
            ?.setSyncSwitch(
                enable = false,
            )
        userTokenStateOb.emit(
            value = null,
        )
        userInfoStateOb.emit(
            value = null,
        )
        vipInfoStateOb.emit(
            value = null,
        )
        latestUserIdStateOb.emit(
            value = null,
        )
        // 打开登录界面
        val topAct = ActivityStack.first { it.isAlive() }
        if (topAct == null) {
            // 打开登录界面
            AppRouterUserApi::class
                .routeApi()
                .toLoginViewInNewTaskSuspend()
            // 杀死所有界面, 除了登录界面
            ActivityStack.finish {
                it.noFlag(flag = APP_ACTIVITY_FLAG_LOGIN)
            }
        } else // 占位
        {
            // 打开登录界面
            AppRouterUserApi::class
                .routeApi()
                .toLoginViewSuspend(
                    context = topAct,
                )
            // 杀死所有界面, 除了登录界面
            ActivityStack.finish {
                it.noFlag(flag = APP_ACTIVITY_FLAG_LOGIN)
            }
        }
        // 销毁数据库
        AppServices
            .tallyDataSourceInitSpi
            .destroyTallyDataBase()
        // 让表同步不再等待, 让他等待同步开关开启
        AppServices
            .tallyDataSyncSpi
            ?.trySync()
        LogSupport.d(
            tag = UserSpi.TAG,
            content = "logoutForBusinessLogic end",
        )
    }

    override fun updateTokenInfoAction(): SuspendAction0 {
        return suspendAction0 {
            LogSupport.d(
                tag = UserSpi.TAG,
                content = "updateTokenInfoAction start",
            )
            val tokenInfo = AppServices
                .appNetworkSpi
                .refreshToken()
            LogSupport.d(
                tag = UserSpi.TAG,
                content = "Token 过期时间：${tokenInfo.expireTime.commonTimeFormat1()}",
            )
            userTokenStateOb.emit(
                value = tokenInfo.token,
            )
            LogSupport.d(
                tag = UserSpi.TAG,
                content = "updateTokenInfoAction end",
            )
        }
    }

    override fun updateVipInfoAction(): SuspendAction0 {
        return suspendAction0 {
            LogSupport.d(
                tag = UserSpi.TAG,
                content = "updateVipInfoAction start",
            )
            vipInfoStateOb.emit(
                value = AppServices
                    .appNetworkSpi
                    .getVipInfo().apply {
                        LogSupport.d(
                            tag = UserSpi.TAG,
                            content = "userVipInfo：$this",
                        )
                    },
            )
            LogSupport.d(
                tag = UserSpi.TAG,
                content = "updateVipInfoAction end",
            )
        }
    }

}