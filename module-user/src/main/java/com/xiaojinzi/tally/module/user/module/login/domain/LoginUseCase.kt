package com.xiaojinzi.tally.module.user.module.login.domain

import android.content.Context
import androidx.annotation.UiContext
import androidx.compose.ui.text.input.TextFieldValue
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.DialogUseCase
import com.xiaojinzi.support.activity_stack.ActivityStack
import com.xiaojinzi.support.ktx.HotEventFlow
import com.xiaojinzi.support.ktx.HotStateFlow
import com.xiaojinzi.support.ktx.MutableInitOnceData
import com.xiaojinzi.support.ktx.NormalMutableSharedFlow
import com.xiaojinzi.support.ktx.findException
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.exception.UserIdDoesNotMatchTheLastLoginException
import com.xiaojinzi.tally.lib.res.model.network.AppNetworkException
import com.xiaojinzi.tally.lib.res.model.network.AppNetworkException.Companion.CODE_ACCOUNT_ALREADY_BIND_WX
import com.xiaojinzi.tally.lib.res.model.user.ThirdLoginBindPhoneException
import com.xiaojinzi.tally.lib.res.ui.APP_ACTIVITY_FLAG_LOGIN
import com.xiaojinzi.tally.lib.res.ui.APP_ACTIVITY_FLAG_MAIN
import com.xiaojinzi.tally.module.base.support.AppRouterMainApi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class LoginIntent {

    data class LoginByCheckCode(
        @UiContext val context: Context
    ) : LoginIntent()

    data class LoginByBindWx(
        @UiContext val context: Context
    ) : LoginIntent()

    data class LoginByWx(
        @UiContext val context: Context
    ) : LoginIntent()

    data class SendCheckCode(
        val usage: Usage,
    ) : LoginIntent() {
        enum class Usage(
            val str: String,
        ) {
            LOGIN(
                str = "login",
            ),
            BIND_WX(
                str = "bindWx",
            ),
        }

    }
}

interface LoginUseCase : BusinessMVIUseCase {

    /**
     * 微信绑定的 authId
     */
    val wxAuthIdInitData: MutableInitOnceData<String?>

    /**
     * 手机号焦点请求事件
     */
    val phoneNumberRequestForceEvent: HotEventFlow<Unit>

    /**
     * 手机号
     */
    val phoneNumberStateOb: MutableStateFlow<TextFieldValue>

    /**
     * 验证码
     */
    val checkCodeStateOb: MutableStateFlow<TextFieldValue>

    /**
     * 可再次发送验证码的时间
     */
    val sendCheckCodeAvailableTimeStateOb: HotStateFlow<Long?>

    /**
     * 发送验证码的倒计时
     * 60, 59
     */
    val sendCheckCodeCountDownStateOb: HotStateFlow<Long?>

    /**
     * 是否可以发送验证码
     */
    val canSendCheckCodeStateOb: HotStateFlow<Boolean>

    /**
     * 协议的抖动事件
     */
    val agreementViewShakeEvent: HotEventFlow<Unit>

    /**
     * 是否已经读取了协议
     */
    val hasReadAgreementState: MutableStateFlow<Boolean>

    /**
     * 是否可提交
     */
    val canSubmitStateOb: HotStateFlow<Boolean>

    /**
     * 是否可提交, 绑定手机号的界面
     */
    val canSubmitForBindPhoneNUmberStateOb: HotStateFlow<Boolean>

}

class LoginUseCaseImpl(
) : BusinessMVIUseCaseImpl(), LoginUseCase {

    override val wxAuthIdInitData = MutableInitOnceData<String?>()

    override val phoneNumberRequestForceEvent = NormalMutableSharedFlow<Unit>()

    override val phoneNumberStateOb = MutableStateFlow(value = TextFieldValue(text = ""))

    override val checkCodeStateOb = MutableStateFlow(value = TextFieldValue(text = ""))

    override val sendCheckCodeAvailableTimeStateOb = MutableStateFlow<Long?>(value = null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val sendCheckCodeCountDownStateOb = sendCheckCodeAvailableTimeStateOb
        .flatMapLatest { targetTime ->
            if (targetTime == null) {
                flowOf(value = null)
            } else {
                var currentTime = System.currentTimeMillis()
                flow<Long?> {
                    while (currentCoroutineContext().isActive && targetTime > currentTime) {
                        val countDown = ((targetTime - currentTime) / 1000)
                        emit(value = countDown)
                        delay(timeMillis = 1000)
                        currentTime = System.currentTimeMillis()
                    }
                    emit(value = null)
                }
            }
        }
        .sharedStateIn(
            scope = scope,
            initValue = null,
        )

    override val canSendCheckCodeStateOb = sendCheckCodeCountDownStateOb
        .map {
            it == null
        }

    override val agreementViewShakeEvent = NormalMutableSharedFlow<Unit>()

    override val hasReadAgreementState = MutableStateFlow(value = false)

    override val canSubmitStateOb = combine(
        phoneNumberStateOb,
        checkCodeStateOb,
        hasReadAgreementState,
    ) { name, password, hasReadAgreement ->
        name.text.isNotBlank() && password.text.isNotBlank() && hasReadAgreement
    }

    override val canSubmitForBindPhoneNUmberStateOb = combine(
        phoneNumberStateOb,
        checkCodeStateOb,
    ) { phoneNumber, checkCode ->
        phoneNumber.text.isNotBlank() && checkCode.text.isNotBlank()
    }

    private suspend fun reset() {
        phoneNumberStateOb.emit(
            value = TextFieldValue(text = ""),
        )
        checkCodeStateOb.emit(
            value = TextFieldValue(text = ""),
        )
        sendCheckCodeAvailableTimeStateOb.emit(
            value = null,
        )
        phoneNumberRequestForceEvent.emit(
            value = Unit,
        )
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun sendCheckCode(
        intent: LoginIntent.SendCheckCode,
    ) {
        val canSend = canSendCheckCodeStateOb.first()
        if (!canSend) {
            return
        }
        val phoneNumber = phoneNumberStateOb.first().text
        if (phoneNumber.isEmpty()) {
            tip(
                content = "手机号不能为空".toStringItemDto(),
            )
            return
        }
        AppServices
            .appNetworkSpi
            .sendCheckCode(
                usage = intent.usage.str,
                phoneNumber = phoneNumber,
            )
        sendCheckCodeAvailableTimeStateOb.emit(
            value = System.currentTimeMillis() + 1000 * 60,
        )
        tip(
            content = "发送成功".toStringItemDto(),
        )
    }

    private suspend fun login(
        @UiContext context: Context,
        loginAction: suspend () -> Unit,
    ) {
        withLoading {
            try {
                loginAction.invoke()
                tip(content = "登录成功".toStringItemDto())
                val mainView = ActivityStack
                    .first {
                        it.hasFlag(flag = APP_ACTIVITY_FLAG_MAIN)
                    }
                if (mainView == null) {
                    AppRouterMainApi::class
                        .routeApi()
                        .toMainView(
                            context = context,
                        ) {
                            ActivityStack.finish {
                                it.hasFlag(flag = APP_ACTIVITY_FLAG_LOGIN)
                            }
                        }
                } else {
                    ActivityStack.finish {
                        it.hasFlag(flag = APP_ACTIVITY_FLAG_LOGIN)
                    }
                }
            } catch (e: Exception) {
                val appNetworkException = e.findException(
                    targetClass = AppNetworkException::class,
                )
                val isAccountAlreadyBindWx =
                    appNetworkException?.code == CODE_ACCOUNT_ALREADY_BIND_WX
                val bindPhoneException = e.findException(
                    targetClass = ThirdLoginBindPhoneException::class,
                )
                val userIdDoesNotMatchTheLastLoginException = e.findException(
                    targetClass = UserIdDoesNotMatchTheLastLoginException::class,
                )
                if (isAccountAlreadyBindWx) {
                    confirmDialog(
                        content = "手机号已绑定其他微信\n请绑定其他手机号".toStringItemDto(),
                        negative = null,
                        positive = "知道了".toStringItemDto(),
                    )
                    reset()
                } else if (bindPhoneException != null) {
                    AppRouterUserApi::class
                        .routeApi()
                        .toBindPhoneView(
                            context = context,
                            wxAuthId = bindPhoneException.authId,
                        )
                } else if (userIdDoesNotMatchTheLastLoginException != null) {
                    val dialogResult = confirmDialog(
                        content = "本次登录账号和上次登录不一致\n登录其他账号请到设置界面退出登录".toStringItemDto(),
                        negative = "重新登录".toStringItemDto(),
                        positive = "去设置".toStringItemDto(),
                    )
                    when (dialogResult) {
                        DialogUseCase.ConfirmDialogResultType.CANCEL -> {
                            reset()
                        }

                        DialogUseCase.ConfirmDialogResultType.CONFIRM -> {
                            AppRouterMainApi::class
                                .routeApi()
                                .toSettingView(
                                    context = context,
                                ) {
                                    ActivityStack.finish {
                                        it.hasFlag(flag = APP_ACTIVITY_FLAG_LOGIN)
                                    }
                                }
                        }
                    }
                } else {
                    throw e
                }
            }
        }
    }

    @IntentProcess
    private suspend fun loginByCheckCode(
        intent: LoginIntent.LoginByCheckCode,
    ) {
        login(
            context = intent.context,
        ) {
            val phoneNumber = phoneNumberStateOb.first().text
            val checkCode = checkCodeStateOb.first().text
            AppServices
                .userSpi
                .loginByCheckCode(
                    phoneNumber = phoneNumber,
                    checkCode = checkCode,
                )
        }
    }

    @IntentProcess
    private suspend fun loginByBindWx(
        intent: LoginIntent.LoginByBindWx,
    ) {
        val wxAuthId = wxAuthIdInitData.awaitValue()
        if (wxAuthId != null) {
            login(
                context = intent.context,
            ) {
                val phoneNumber = phoneNumberStateOb.first().text
                val checkCode = checkCodeStateOb.first().text
                AppServices
                    .userSpi
                    .loginByBindWx(
                        authId = wxAuthId,
                        phoneNumber = phoneNumber,
                        checkCode = checkCode,
                    )
            }
        }
    }

    @IntentProcess
    private suspend fun loginByWx(
        intent: LoginIntent.LoginByWx,
    ) {
        if (!hasReadAgreementState.first()) {
            agreementViewShakeEvent.emit(
                value = Unit,
            )
            return
        }
        login(
            context = intent.context,
        ) {
            AppServices
                .userSpi
                .loginByWx()
        }
    }

    init {
        val forOpenSource = AppServices.appInfoSpi.forOpenSource
        // 如果是开源版本, 自动完成登录
        if (forOpenSource) {
            scope.launch {
                withLoading {
                    var temp = ""
                    "18888888888".forEach {
                        temp += it
                        phoneNumberStateOb.emit(
                            value = TextFieldValue(text = temp),
                        )
                        delay(200)
                    }
                    temp = ""
                    "123456".forEach {
                        temp += it
                        checkCodeStateOb.emit(
                            value = TextFieldValue(text = temp),
                        )
                        delay(200)
                    }
                    hasReadAgreementState.emit(
                        value = true,
                    )
                }
                ActivityStack.topAlive?.let { context ->
                    addIntent(
                        intent = LoginIntent.LoginByCheckCode(
                            context = context,
                        ),
                    )
                }
            }
        }
    }

}

