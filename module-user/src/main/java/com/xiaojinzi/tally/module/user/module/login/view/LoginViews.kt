package com.xiaojinzi.tally.module.user.module.login.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.tally.lib.res.R
import com.xiaojinzi.tally.lib.res.SupportLoginMethod
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_LARGE
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.user.module.login.domain.LoginIntent
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun LoginView(
    needInit: Boolean? = false,
    previewDefault: LoginPreviewDefault? = null,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    BusinessContentView<LoginViewModel>(
        needInit = needInit,
    ) { vm ->

        val phoneNumber by vm.phoneNumberStateOb.collectAsState(initial = TextFieldValue(text = ""))
        val checkCode by vm.checkCodeStateOb.collectAsState(initial = TextFieldValue(text = ""))
        val canSubmit by vm.canSubmitStateOb.collectAsState(initial = false)
        val sendCheckCodeCountDown by vm.sendCheckCodeCountDownStateOb.collectAsState(initial = null)
        val hasReadAgreement by vm.hasReadAgreementState.collectAsState(initial = false)

        val focusRequesterForPhoneName = remember { FocusRequester() }
        val focusRequesterForCheckCodeName = remember { FocusRequester() }

        LaunchedEffect(key1 = Unit) {
            vm
                .phoneNumberRequestForceEvent
                .onEach {
                    focusRequesterForPhoneName.requestFocus()
                    keyboardController?.show()
                }
                .launchIn(scope = this)
        }

        // 有微信登录就默认不弹出键盘
        /*LaunchedEffect(key1 = focusRequesterForPhoneName) {
            focusRequesterForPhoneName.requestFocus()
            keyboardController?.show()
        }*/

        Column(
            modifier = Modifier
                .clickableNoRipple {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                )
                .statusBarsPadding()
                .padding(horizontal = 38.dp, vertical = 0.dp)
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(
                modifier = Modifier
                    .height(height = 50.dp)
                    .nothing()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    modifier = Modifier
                        .size(size = 48.dp)
                        .clip(shape = MaterialTheme.shapes.small)
                        .nothing(),
                    painter = rememberAsyncImagePainter(
                        model = AppServices.appInfoSpi.appLauncherIconRsd,
                    ),
                    contentDescription = null,
                )
                AppWidthSpace()
                Text(
                    modifier = Modifier
                        .wrapContentSize()
                        .nothing(),
                    text = "一刻记账",
                    fontFamily = FontFamily(Font(R.font.res_font_xdks)),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(
                modifier = Modifier
                    .height(height = 40.dp)
                    .nothing()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(
                        shape = MaterialTheme.shapes.small,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                            elevation = 1.dp,
                        ),
                    )
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .nothing(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (phoneNumber.text.isEmpty()) {
                    Text(
                        text = "请输入手机号",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.outline,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
                BasicTextField(
                    modifier = Modifier
                        .focusRequester(
                            focusRequester = focusRequesterForPhoneName,
                        )
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_LARGE.dp)
                        .nothing(),
                    value = phoneNumber,
                    onValueChange = {
                        vm.phoneNumberStateOb.value = it.copy(
                            text = it.text.trim(),
                        )
                    },
                    cursorBrush = SolidColor(
                        value = MaterialTheme.colorScheme.primary,
                    ),
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                        },
                    ),
                )
            }

            Spacer(
                modifier = Modifier
                    .height(height = 24.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(
                        shape = MaterialTheme.shapes.small,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                            elevation = 1.dp,
                        ),
                    )
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .nothing(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (checkCode.text.isEmpty()) {
                    Text(
                        text = "请输入验证码",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.outline,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
                BasicTextField(
                    modifier = Modifier
                        .focusRequester(
                            focusRequester = focusRequesterForCheckCodeName,
                        )
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_LARGE.dp)
                        .nothing(),
                    value = checkCode,
                    onValueChange = {
                        vm.checkCodeStateOb.value = it.copy(
                            text = it.text.trim().take(n = 6),
                        )
                    },
                    cursorBrush = SolidColor(
                        value = MaterialTheme.colorScheme.primary,
                    ),
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Row(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .wrapContentSize()
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .clickableNoRipple {
                                if (phoneNumber.text.isNotEmpty()) {
                                    if (sendCheckCodeCountDown == null) {
                                        vm.addIntent(
                                            intent = LoginIntent.SendCheckCode(
                                                usage = LoginIntent.SendCheckCode.Usage.LOGIN
                                            ),
                                        )
                                    }
                                    focusRequesterForCheckCodeName.requestFocus()
                                    keyboardController?.show()
                                }
                            }
                            .wrapContentSize()
                            .padding(horizontal = 0.dp, vertical = 6.dp)
                            .nothing(),
                        text = if (sendCheckCodeCountDown == null) {
                            "发送验证码"
                        } else {
                            "重新发送(${sendCheckCodeCountDown})"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (phoneNumber.text.isEmpty() || sendCheckCodeCountDown != null) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(height = 28.dp)
                    .nothing()
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                enabled = previewDefault?.canSubmit ?: canSubmit,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(true)
                    vm.addIntent(
                        intent = LoginIntent.LoginByCheckCode(
                            context = context,
                        )
                    )
                },
            ) {
                Text(text = "登录")
            }

            val offsetX = remember { Animatable(0f) }

            LaunchedEffect(key1 = Unit) {
                vm.agreementViewShakeEvent
                    .collectLatest {
                        for (i in 0..10) {
                            when (i % 2) {
                                0 -> offsetX.animateTo(
                                    targetValue = 5f,
                                    animationSpec = spring(stiffness = 100_000f),
                                )

                                else -> offsetX.animateTo(
                                    targetValue = -5f,
                                    animationSpec = spring(stiffness = 100_000f),
                                )
                            }
                        }
                        offsetX.animateTo(0f)
                    }
            }

            Row(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = offsetX.value.roundToInt(),
                            y = 0,
                        )
                    }
                    .clickableNoRipple {
                        vm.hasReadAgreementState.value = vm.hasReadAgreementState.value.not()
                    }
                    .padding(horizontal = 0.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Spacer(
                    modifier = Modifier
                        .width(width = 2.dp)
                        .nothing()
                )
                Icon(
                    modifier = Modifier
                        .size(size = 12.dp)
                        .nothing(),
                    painter = painterResource(
                        id = if (hasReadAgreement) {
                            R.drawable.res_radio1_selected
                        } else {
                            R.drawable.res_radio1
                        },
                    ),
                    contentDescription = null,
                    tint = if (hasReadAgreement) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
                Spacer(
                    modifier = Modifier
                        .width(width = 4.dp)
                        .nothing()
                )
                val annotatedString = buildAnnotatedString {
                    this.append("阅读并同意")
                    withLink(
                        link = LinkAnnotation.Clickable(
                            tag = "userProtocol1",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    color = Color(
                                        color = MaterialTheme.colorScheme.outline.toArgb(),
                                    ),
                                ),
                            ),
                            linkInteractionListener = { _ ->
                                AppRouterBaseApi::class
                                    .routeApi()
                                    .toWebView(
                                        context = context,
                                        url = AppServices.appInfoSpi.userAgreementUrl,
                                    )
                            },
                        )
                    ) {
                        append("《用户协议》")
                    }
                    this.append("和")
                    withLink(
                        link = LinkAnnotation.Clickable(
                            tag = "userProtocol2",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    color = Color(
                                        color = MaterialTheme.colorScheme.outline.toArgb(),
                                    ),
                                ),
                            ),
                            linkInteractionListener = { _ ->
                                AppRouterBaseApi::class
                                    .routeApi()
                                    .toWebView(
                                        context = context,
                                        url = AppServices.appInfoSpi.userAgreementUrl,
                                    )
                            },
                        )
                    ) {
                        append("《隐私政策》")
                    }
                }
                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.outline,
                    ),
                )
            }

            Spacer(
                modifier = Modifier
                    .height(height = 20.dp)
                    .nothing()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = APP_PADDING_LARGE.dp, vertical = 0.dp)
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .height(2.dp)
                        .circleClip()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.outline,
                                ),
                            )
                        )
                        .nothing()
                )
                AppWidthSpace()
                Text(
                    text = "其他登录方式",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.outline,
                    ),
                    textAlign = TextAlign.Start,
                )
                AppWidthSpace()
                Spacer(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .height(2.dp)
                        .circleClip()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.outline,
                                    Color.Transparent,
                                ),
                            )
                        )
                        .nothing()
                )
            }

            /*其他登录方式*/
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {

                AppServices
                    .appInfoSpi
                    .supportLoginMethodList
                    .forEach { loginMethod ->
                        IconButton(
                            modifier = Modifier
                                .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                                .nothing(),
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                when (loginMethod) {
                                    SupportLoginMethod.WX -> {
                                        vm.addIntent(
                                            intent = LoginIntent.LoginByWx(
                                                context = context,
                                            )
                                        )
                                    }

                                    SupportLoginMethod.Google -> {

                                    }
                                }
                            },
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(size = 32.dp)
                                    .nothing(),
                                painter = painterResource(
                                    id = when (loginMethod) {
                                        SupportLoginMethod.WX -> {
                                            R.drawable.res_wechat1
                                        }

                                        SupportLoginMethod.Google -> {
                                            R.drawable.res_google1
                                        }
                                    },
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

            }

        }

    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun LoginViewWrap() {
    LoginView()
}

private data class LoginPreviewDefault(
    val name: String = "admin",
    val password: String = "123",
    val canSubmit: Boolean = true,
)

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview(
    showBackground = true,
)
@Composable
private fun LoginViewPreview() {
    LoginView(
        needInit = false,
        previewDefault = LoginPreviewDefault()
    )
}