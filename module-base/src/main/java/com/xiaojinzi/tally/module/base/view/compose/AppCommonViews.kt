package com.xiaojinzi.tally.module.base.view.compose

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.support.AppImageDefault
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
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {

    val localImage = when (imageRes) {
        is String -> android.net.Uri.parse(imageRes).let { uri ->
            when (uri.scheme?.lowercase()) {
                null -> imageRes.takeIf { java.io.File(it).isAbsolute }
                "file", "content", "android.resource" -> uri
                else -> null
            }
        }
        is android.net.Uri -> imageRes.takeIf {
            it.scheme?.lowercase() in setOf("file", "content", "android.resource")
        }
        is java.io.File, is Int, is android.graphics.Bitmap, is Drawable -> imageRes
        else -> null
    }
    AsyncImage(
        modifier = modifier,
        model = ImageRequest
            .Builder(LocalContext.current)
            .data(
                data = localImage.apply {
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

@Composable
fun AppCommonActionButton(
    modifier: Modifier = Modifier,
    text: StringItemDto,
    onClick: () -> Unit = {},
) {
    Button(
        modifier = modifier,
        onClick = onClick,
    ) {
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
