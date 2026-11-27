package com.xiaojinzi.tally.module.imagepreview.module.image_preview.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.ktx.launchIgnoreError
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.tally.module.base.view.compose.AppCommonCoilImage
import com.xiaojinzi.tally.module.imagepreview.lib.viewer.ComposeModel
import com.xiaojinzi.tally.module.imagepreview.lib.viewer.ImageViewer
import com.xiaojinzi.tally.module.imagepreview.lib.viewer.rememberViewerState
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun ImagePreviewView(
    needInit: Boolean? = false,
    urlList: List<String> = emptyList(),
    index: Int = 0,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    BusinessContentView<ImagePreviewViewModel>(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color.Black.copy(
                    alpha = 0.9f,
                )
            )
            .nothing(),
        needInit = needInit,
    ) { vm ->
        if (urlList.isNotEmpty()) {
            val pageState = rememberPagerState(
                initialPage = index.coerceIn(
                    minimumValue = 0,
                    maximumValue = urlList.lastIndex,
                )
            ) {
                urlList.size
            }
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .nothing(),
                state = pageState,
            ) { pageIndex ->
                val imageViewerState = rememberViewerState()
                LaunchedEffect(key1 = pageIndex) {
                    imageViewerState.scale.animateTo(
                        targetValue = 1f
                    )
                    imageViewerState.offsetX.animateTo(
                        targetValue = 0f,
                    )
                    imageViewerState.offsetY.animateTo(
                        targetValue = 0f,
                    )
                }
                ImageViewer(
                    state = imageViewerState,
                    model = ComposeModel {
                        AppCommonCoilImage(
                            modifier = Modifier
                                .fillMaxSize()
                                .nothing(),
                            imageRes = urlList[pageIndex],
                            contentScale = ContentScale.Fit,
                        )
                    },
                    boundClip = false,
                    detectGesture = {
                        onDoubleTap = {
                            scope.launchIgnoreError {
                                imageViewerState.toggleScale(
                                    offset = it,
                                )
                            }
                        }
                    },
                )
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
fun ImagePreviewViewWrap(
    urlList: List<String>,
    index: Int = 0,
) {
    ImagePreviewView(
        urlList = urlList,
        index = index,
    )
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun ImagePreviewViewPreview() {
    ImagePreviewView(
        needInit = false,
    )
}