package com.xiaojinzi.tally.module.user.module.about_us.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.AppHeightSpace
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun AboutUsView(
    needInit: Boolean? = false,
) {
    BusinessContentView<AboutUsViewModel>(
        needInit = needInit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surface)
                .verticalScroll(state = rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 24.dp)
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(height = 24.dp))

            Image(
                modifier = Modifier.size(size = 60.dp),
                painter = painterResource(id = AppServices.appInfoSpi.appIconRsd),
                contentDescription = null,
            )

            AppHeightSpace()

            Text(
                text = AppServices.appInfoSpi.appName,
                fontFamily = FontFamily(Font(com.xiaojinzi.tally.lib.res.R.font.res_font_xdks)),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
            )

            AppHeightSpace()

            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .circleClip()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 1.dp),
                    )
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 6.dp),
                text = "v${AppServices.appInfoSpi.appVersionName}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
            )

            AppHeightSpace()

            Text(
                text = AppServices.appInfoSpi.appPackageName,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(height = 32.dp))

            AboutUsInfoSection(
                title = "本地记账",
                content = "本版本无需账号，账本、账单和设置保存在本机，不上传到服务器。",
            )
            AboutUsInfoSection(
                title = "数据与备份",
                content = "账本与照片仅保存在本机。卸载应用或清除应用数据会删除这些内容；本版本不提供云端同步和恢复。",
            )
            AboutUsInfoSection(
                title = "NexioUI · SoftGlass",
                content = "界面组件使用 NexioUI 0.1.1（Copyright 2026 HaoZai000），并包含其 SoftGlass 0.1.3 依赖，采用 Apache License 2.0。NexioUI 的部分实现派生自 compose-miuix-ui 和 AndroidLiquidGlass-kmp，保留各自的版权声明。完整许可及 NOTICE 随应用打包于 assets/open_source。",
            )
            AboutUsInfoSection(
                title = "开源许可",
                content = "本应用基于一刻记账开源项目修改，项目采用 GNU 通用公共许可证第 2 版（GPL v2）。使用、修改和再分发应遵循该许可证；第三方组件保留各自的许可证及版权声明。",
            )
        }
    }
}

@Composable
private fun AboutUsInfoSection(
    title: String,
    content: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun AboutUsViewWrap() {
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = "关于应用".toStringItemDto(),
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            AboutUsView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun AboutUsViewPreview() {
    AboutUsView(
        needInit = false,
    )
}
