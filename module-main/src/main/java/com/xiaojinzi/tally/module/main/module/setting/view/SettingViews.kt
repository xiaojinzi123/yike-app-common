package com.xiaojinzi.tally.module.main.module.setting.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.LocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.support.rememberPainter
import com.xiaojinzi.tally.lib.res.model.support.toLocalImageItemDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_LARGE
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppBackgroundColor
import com.xiaojinzi.tally.lib.res.ui.AppHeightSpace
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import com.xiaojinzi.tally.module.main.module.setting.domain.SettingIntent
import kotlinx.coroutines.InternalCoroutinesApi

@Composable
fun SettingSwitchView1(
    @SuppressLint("ModifierParameter")
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .background(
            color = MaterialTheme.colorScheme.surface,
        )
        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
        .nothing(),
    image: LocalImageItemDto,
    title: StringItemDto,
    value: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
) {

    Row(
        modifier = Modifier
            .then(other = modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(size = 20.dp)
                .nothing(),
            painter = image.rememberPainter(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        AppWidthSpace()
        Text(
            text = title.contentWithComposable(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
        Switch(
            modifier = Modifier
                .scale(scale = 0.6f)
                .nothing(),
            checked = value,
            onCheckedChange = onCheckedChange,
        )
    }

}

@Composable
fun SettingActionView1(
    @SuppressLint("ModifierParameter")
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .background(
            color = MaterialTheme.colorScheme.surface,
        )
        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = APP_PADDING_NORMAL.dp)
        .nothing(),
    image: LocalImageItemDto,
    title: StringItemDto,
    onclick: () -> Unit = {},
) {

    Row(
        modifier = Modifier
            .clickable {
                onclick.invoke()
            }
            .then(other = modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(size = 20.dp)
                .nothing(),
            painter = image.rememberPainter(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        AppWidthSpace()
        Text(
            text = title.contentWithComposable(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
        Icon(
            modifier = Modifier
                .size(size = 18.dp)
                .nothing(),
            painter = rememberVectorPainter(image = Icons.Rounded.KeyboardArrowRight),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }

}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun SettingView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    val isAiBillFirst by AppServices
        .appConfigSpi
        .isAiBillFirstStateOb
        .collectAsState(
            initial = false
        )
    val isShowHourAndMinuteWhenBillCrud by AppServices
        .appConfigSpi
        .isShowHourAndMinuteWhenBillCrudStateOb
        .collectAsState(
            initial = false
        )
    val isAllowZeroAmountWhenBillCrud by AppServices
        .appConfigSpi
        .isAllowZeroAmountWhenBillCrudStateOb
        .collectAsState(
            initial = false
        )
    val isShowAssetsTab by AppServices
        .appConfigSpi
        .isShowAssetsTabStateOb
        .collectAsState(
            initial = false
        )
    BusinessContentView<SettingViewModel>(
        needInit = needInit,
    ) { vm ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = AppBackgroundColor,
                )
                .navigationBarsPadding()
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            AppHeightSpace()

            SettingActionView1(
                image = com.xiaojinzi.tally.lib.res.R.drawable.res_people1.toLocalImageItemDto(),
                title = "账号信息".toStringItemDto(),
            ) {
                AppRouterUserApi::class
                    .routeApi()
                    .toUserInfoView(
                        context = context,
                    )
            }

            AppHeightSpace()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
            ) {
                SettingSwitchView1(
                    image = com.xiaojinzi.tally.lib.res.R.drawable.res_robot1.toLocalImageItemDto(),
                    title = "优先 AI 记账 (Vip)".toStringItemDto(),
                    value = isAiBillFirst,
                ) {
                    AppServices
                        .appConfigSpi
                        .switchAiBillFirst(
                            b = it,
                        )
                }
                SettingSwitchView1(
                    image = com.xiaojinzi.tally.lib.res.R.drawable.res_clock1.toLocalImageItemDto(),
                    title = "记账精确到时分".toStringItemDto(),
                    value = isShowHourAndMinuteWhenBillCrud,
                ) {
                    AppServices
                        .appConfigSpi
                        .switchShowHourAndMinuteWhenBillCrud(
                            b = it,
                        )
                }
                SettingSwitchView1(
                    image = com.xiaojinzi.tally.lib.res.R.drawable.res_zero1.toLocalImageItemDto(),
                    title = "记账允许金额为0".toStringItemDto(),
                    value = isAllowZeroAmountWhenBillCrud,
                ) {
                    AppServices
                        .appConfigSpi
                        .switchAllowZeroAmountWhenBillCrudStateOb(
                            b = it,
                        )
                }
                SettingSwitchView1(
                    image = com.xiaojinzi.tally.lib.res.R.drawable.res_account1.toLocalImageItemDto(),
                    title = "首页显示资产Tab".toStringItemDto(),
                    value = isShowAssetsTab,
                ) {
                    AppServices
                        .appConfigSpi
                        .switchShowAssetsStateOb(
                            b = it,
                        )
                }
            }

            AppHeightSpace()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
            ) {
                SettingActionView1(
                    image = com.xiaojinzi.tally.lib.res.R.drawable.res_tip1.toLocalImageItemDto(),
                    title = "钱迹工具".toStringItemDto(),
                ) {
                    Router.with(context = context)
                        .hostAndPath(hostAndPath = AppRouterConfig.QIANJI_TOOLS)
                        .forward()
                }
                SettingActionView1(
                    image = com.xiaojinzi.tally.lib.res.R.drawable.res_update1.toLocalImageItemDto(),
                    title = "检查更新".toStringItemDto(),
                ) {
                    vm.addIntent(
                        intent = SettingIntent.CheckUpdate(
                            context = context,
                        )
                    )
                }
                SettingActionView1(
                    image = com.xiaojinzi.tally.lib.res.R.drawable.res_tip1.toLocalImageItemDto(),
                    title = "意见反馈".toStringItemDto(),
                ) {
                    vm.addIntent(
                        intent = SettingIntent.Feedback(
                            context = context,
                        )
                    )
                }
                SettingActionView1(
                    image = com.xiaojinzi.tally.lib.res.R.drawable.res_warn1.toLocalImageItemDto(),
                    title = "关于我们".toStringItemDto(),
                ) {
                    AppRouterUserApi::class
                        .routeApi()
                        .toAboutUsView(
                            context = context,
                        )
                }
            }

            AppHeightSpace()

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
                    .clickable {
                        vm.addIntent(
                            intent = SettingIntent.ToLoginOut(
                                context = context,
                            )
                        )
                    }
                    .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                    .nothing(),
                text = "退出登录",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(weight = 1f, fill = true))

            Text(
                modifier = Modifier
                    .clickableNoRipple {
                        vm.addIntent(
                            intent = SettingIntent.ToLogOff(
                                context = context,
                            )
                        )
                    }
                    .wrapContentSize()
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = APP_PADDING_LARGE.dp)
                    .nothing(),
                text = "注销账号",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.8f,
                    ),
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                modifier = Modifier
                    .nothing(),
                text = AppServices.appInfoSpi.appRecordInfo,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.8f,
                    ),
                ),
                textAlign = TextAlign.Center,
            )

        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun SettingViewWrap() {
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = "设置".toStringItemDto(),
            )
        },
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            SettingView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun SettingViewPreview() {
    SettingView(
        needInit = false,
    )
}