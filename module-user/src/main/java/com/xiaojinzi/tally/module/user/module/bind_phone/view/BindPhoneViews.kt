package com.xiaojinzi.tally.module.user.module.bind_phone.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_LARGE
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import com.xiaojinzi.tally.module.user.module.login.domain.LoginIntent
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val TAG = "BindPhoneView"

@OptIn(ExperimentalComposeUiApi::class)
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BindPhoneView(
    needInit: Boolean? = null,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    BusinessContentView<BindPhoneViewModel>(
        needInit = needInit,
    ) { vm ->
        val phoneNumber by vm.phoneNumberStateOb.collectAsState(initial = TextFieldValue(text = ""))
        val checkCode by vm.checkCodeStateOb.collectAsState(initial = TextFieldValue(text = ""))
        val canSubmit by vm.canSubmitForBindPhoneNUmberStateOb.collectAsState(initial = false)
        val sendCheckCodeCountDown by vm.sendCheckCodeCountDownStateOb.collectAsState(initial = null)

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

        LaunchedEffect(key1 = focusRequesterForPhoneName) {
            focusRequesterForPhoneName.requestFocus()
            keyboardController?.show()
        }

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
                                                usage = LoginIntent.SendCheckCode.Usage.BIND_WX
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

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 0.dp, vertical = 26.dp),
                enabled = canSubmit,
                onClick = {
                    vm.addIntent(
                        intent = LoginIntent.LoginByBindWx(
                            context = context,
                        )
                    )
                },
            ) {
                Text(text = "绑定")
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
fun BindPhoneViewWrap() {
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = "绑定手机号".toStringItemDto(),
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            BindPhoneView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BindPhoneViewPreview() {
    BindPhoneView(
        needInit = false,
    )
}