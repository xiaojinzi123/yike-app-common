package com.xiaojinzi.tally.module.base.view.compose

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.OssProcess
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_LARGE
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppHeightSpace
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.module.base.support.AppImageDefault
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AnyThread
fun commonAppToast(context: Context = app, @StringRes contentRsd: Int, isShort: Boolean = true) {
    commonAppToast(context = context, content = context.getString(contentRsd), isShort = isShort)
}

@AnyThread
fun commonAppToast(context: Context = app, content: String, isShort: Boolean = true) {
    val runnable = Runnable {
        Toast.makeText(
            context,
            content,
            if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        ).show()
    }
    AppScope.launch(context = Dispatchers.Main.immediate) {
        runnable.run()
    }
}

@Composable
fun AppCommonInitDataView(
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.xiaojinzi.tally.lib.res.R.raw.res_loading2)
    )
    LottieAnimation(
        modifier = modifier,
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
}

@Composable
fun AppCommonErrorDataView(
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.xiaojinzi.tally.lib.res.R.raw.res_loading2)
    )
    LottieAnimation(
        modifier = modifier,
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
}

@Composable
fun AppCommonEmptyDataView(
    modifier: Modifier = Modifier,
    text: StringItemDto = "空空如也".toStringItemDto(),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(com.xiaojinzi.tally.lib.res.R.raw.res_empty1)
        )
        LottieAnimation(
            modifier = Modifier
                .size(size = 150.dp)
                .nothing(),
            composition = composition,
            iterations = LottieConstants.IterateForever,
        )
        Text(
            text = text.contentWithComposable(),
            style = TextStyle(
                fontSize = 12.sp,
                color = Color(0xFFD9D1D1),
                fontWeight = FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AppCommonNotLoginDataView(
    modifier: Modifier = Modifier,
    text: StringItemDto = "您还未登录".toStringItemDto(),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(com.xiaojinzi.tally.lib.res.R.raw.res_not_login1)
        )
        LottieAnimation(
            modifier = Modifier
                .size(size = 150.dp)
                .nothing(),
            composition = composition,
            iterations = LottieConstants.IterateForever,
        )
        Text(
            text = text.contentWithComposable(),
            style = TextStyle(
                fontSize = 12.sp,
                color = Color(0xFFD9D1D1),
                fontWeight = FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 只会触发刷新, 动画会提前结束
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppPullRefreshView(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val viewHeight = 120f
    val onRefreshState by rememberUpdatedState(newValue = onRefresh)
    var refreshing by remember {
        mutableStateOf(value = false)
    }
    val pullState: PullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { refreshing = true },
    )
    LaunchedEffect(key1 = pullState) {
        snapshotFlow { refreshing }
            .filter { it }
            .onEach {
                delay(800)
                refreshing = false
                onRefreshState.invoke()
            }
            .launchIn(scope = this)
    }
    val targetOffsetY by animateFloatAsState(
        targetValue = if (refreshing) {
            viewHeight
        } else {
            pullState.progress.coerceAtMost(maximumValue = 1f) * viewHeight
        },
        label = "",
    )
    Box(
        modifier = modifier
            .offset(y = targetOffsetY.dp)
            .pullRefresh(
                state = pullState,
            )
            .nothing(),
        contentAlignment = Alignment.TopCenter,
    ) {
        content()
        Box(
            modifier = Modifier
                .offset(y = (-viewHeight).dp)
                .fillMaxWidth()
                .height(viewHeight.dp)
                /*.background(
                    color = Color.Red,
                )*/
                .nothing(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(com.xiaojinzi.tally.lib.res.R.raw.res_loading2)
            )
            LottieAnimation(
                modifier = Modifier
                    .size(60.dp)
                    .nothing(),
                composition = composition,
                iterations = LottieConstants.IterateForever,
            )
        }
    }
}

@Composable
fun AppCommonCoilImage(
    modifier: Modifier,
    imageDefault: Drawable? = AppImageDefault.placeholderImage,
    fallback: Drawable? = imageDefault,
    placeholder: Drawable? = imageDefault,
    error: Drawable? = imageDefault,
    imageRes: Any?,
    contentScale: ContentScale = ContentScale.Crop,
    cosImageProcess: OssProcess? = OssProcess.DEFAULT(),
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {

    AsyncImage(
        modifier = modifier,
        model = ImageRequest
            .Builder(LocalContext.current)
            .data(
                data = imageRes?.run {
                    if (this is String) {
                        cosImageProcess?.let {
                            "${this}?${it.styleStr}"
                        } ?: this
                    } else {
                        this
                    }
                }.apply {
                    LogSupport.d(
                        tag = "AppCommonCoilImage",
                        content = "imageRes: $this",
                    )
                },
            )
            .crossfade(true)
            .apply {
                // 淡入淡出
                this.crossfade(enable = true)
                this.fallback(drawable = fallback)
                this.placeholder(drawable = placeholder)
                this.error(drawable = error)
                this.allowHardware(enable = true)
                this.allowRgb565(enable = true)
                this.scale(scale = Scale.FILL)
            }
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCommonVipButton(
    modifier: Modifier = Modifier,
    isVipCheck: Boolean = true,
    vipTipContent: StringItemDto = "您还不是会员, 请先开通会员".toStringItemDto(),
    text: StringItemDto,
    isAlertDialog: Boolean = true,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val isVip by AppServices.userSpi.isVipStateOb.collectAsState(initial = false)
    val isVipIntercept = isVipCheck && !isVip
    var isShowConfirmDialog by remember {
        mutableStateOf(value = false)
    }
    // 去开通 vip 的弹框
    if (isShowConfirmDialog) {
        AlertDialog(
            modifier = Modifier
                .padding(horizontal = APP_PADDING_SMALL.dp, vertical = 0.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .nothing(),
            onDismissRequest = { isShowConfirmDialog = false },
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_LARGE.dp, vertical = 0.dp)
                    .fillMaxWidth()
                    .clip(
                        shape = MaterialTheme.shapes.medium,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = APP_PADDING_NORMAL.dp)
                    .nothing(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                        .nothing(),
                    text = vipTipContent.contentWithComposable(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Center,
                )
                AppHeightSpace()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nothing(),
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .wrapContentHeight()
                            .nothing(),
                        onClick = {
                            isShowConfirmDialog = false
                        },
                    ) {
                        Text(
                            text = "朕再想想~",
                            style = LocalTextStyle.current.copy(
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                    AppWidthSpace()
                    Button(
                        modifier = Modifier
                            .weight(weight = 1.2f, fill = true)
                            .wrapContentHeight()
                            .nothing(),
                        onClick = {
                            isShowConfirmDialog = false
                            AppRouterUserApi::class
                                .routeApi()
                                .toVipBuyView(
                                    context = context,
                                )
                        },
                    ) {
                        Text(
                            text = "怒开会员",
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
    Button(
        modifier = modifier,
        onClick = {
            if (isVipIntercept) {
                if (isAlertDialog) {
                    isShowConfirmDialog = true
                } else {
                    AppRouterUserApi::class
                        .routeApi()
                        .toVipBuyView(
                            context = context,
                        )
                }
            } else {
                onClick.invoke()
            }
        },
    ) {
        AnimatedVisibility(visible = isVipIntercept) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    modifier = Modifier
                        .size(size = 24.dp)
                        .nothing(),
                    painter = painterResource(
                        id = com.xiaojinzi.tally.lib.res.R.drawable.res_vip3,
                    ),
                    contentDescription = null,
                )
                AppWidthSpace()
            }
        }
        Text(
            text = text.contentWithComposable(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AppLifecycleStateEventHandler(
    targetState: Lifecycle.Event,
    onStateChanged: (event: Lifecycle.Event) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            onStateChanged.invoke(event)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
