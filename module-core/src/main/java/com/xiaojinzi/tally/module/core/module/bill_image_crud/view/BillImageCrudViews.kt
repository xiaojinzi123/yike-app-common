package com.xiaojinzi.tally.module.core.module.bill_image_crud.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaojinzi.tally.module.base.view.compose.AppCommonCoilImage
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.BottomView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppHeightSpace
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.lib.res.ui.topShape
import com.xiaojinzi.tally.module.base.view.compose.AppCommonActionButton
import com.xiaojinzi.tally.module.core.module.bill_image_crud.domain.BillImageCrudIntent
import com.xiaojinzi.tally.module.core.module.bill_image_crud.domain.BillImageCrudUseCase
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BillImageCrudView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    BusinessContentView<BillImageCrudViewModel>(
        needInit = needInit,
    ) { vm ->
        val imageList by vm.imageListStateOb.collectAsState(initial = emptyList())
        BottomView {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(
                        shape = MaterialTheme.shapes.medium.topShape(),
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
                    .nothing(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                ) {
                    Text(
                        modifier = Modifier
                            .align(
                                alignment = Alignment.Center,
                            )
                            .wrapContentSize()
                            .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                            .nothing(),
                        text = "添加图片",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
                AppHeightSpace()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                        .nothing(),
                ) {
                    (0..<BillImageCrudUseCase.IMAGE_COUNT).forEach { index ->
                        if (index > 0) {
                            AppWidthSpace()
                        }
                        Box(
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .aspectRatio(ratio = 1f)
                                .clip(
                                    shape = MaterialTheme.shapes.small,
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                        elevation = 1.dp,
                                    )
                                )
                                .nothing(),
                        ) {
                            imageList.getOrNull(index = index)?.let { item ->
                                AppCommonCoilImage(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nothing(),
                                    imageRes = item.url.orNull() ?: item.localUri,
                                    contentScale = ContentScale.Crop,
                                )
                                Box(
                                    modifier = Modifier
                                        .align(
                                            alignment = Alignment.TopEnd,
                                        )
                                        .clickableNoRipple {
                                            vm.addIntent(
                                                intent = BillImageCrudIntent.ImageDelete(
                                                    uid = item.uid,
                                                )
                                            )
                                        }
                                        .padding(
                                            horizontal = APP_PADDING_SMALL.dp,
                                            vertical = APP_PADDING_SMALL.dp
                                        )
                                        .nothing(),
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .circleClip()
                                            .background(
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                            .size(size = 20.dp)
                                            .nothing(),
                                        painter = rememberVectorPainter(image = Icons.Default.Close),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                    )
                                }
                            } ?: run {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            vm.addIntent(
                                                intent = BillImageCrudIntent.ImageSelect(
                                                    context = context,
                                                )
                                            )
                                        }
                                        .nothing(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .size(size = 36.dp)
                                            .nothing(),
                                        painter = rememberVectorPainter(image = Icons.Default.Add),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                    }
                }
                AppHeightSpace()

                AppCommonActionButton(
                    modifier = Modifier
                        .padding(bottom = APP_PADDING_NORMAL.dp)
                        .navigationBarsPadding()
                        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    onClick = {
                        vm.addIntent(
                            intent = BillImageCrudIntent.Submit(
                                context = context,
                            ),
                        )
                    },
                    text = "保存".toStringItemDto(),
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
fun BillImageCrudViewWrap() {
    BillImageCrudView()
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BillImageCrudViewPreview() {
    BillImageCrudView(
        needInit = false,
    )
}