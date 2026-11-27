package com.xiaojinzi.tally.module.core.module.bill_album.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.commonTimeFormat2
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.view.compose.AppCommonCoilImage
import com.xiaojinzi.tally.module.base.view.compose.AppCommonEmptyDataView
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import com.xiaojinzi.tally.module.core.module.bill_album.domain.BillAlbumIntent
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BillAlbumView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    BusinessContentView<BillAlbumViewModel>(
        needInit = needInit,
    ) { vm ->
        val billImageList by vm.billImageListStateObVo.collectAsState(initial = emptyList())
        if (billImageList.isEmpty()) {
            AppCommonEmptyDataView(
                modifier = Modifier
                    .fillMaxSize()
                    .nothing(),
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .navigationBarsPadding()
                    .nothing(),
                columns = GridCells.Fixed(count = 3),
            ) {
                billImageList.forEach { item ->
                    when (item) {
                        is BillAlbumItemHeaderVo -> {
                            item(
                                span = {
                                    GridItemSpan(currentLineSpan = 3)
                                },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .padding(horizontal = 0.dp, vertical = 0.dp)
                                        .nothing(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .width(width = 4.dp)
                                            .height(height = 12.dp)
                                            .circleClip()
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                            .nothing()
                                    )
                                    Spacer(
                                        modifier = Modifier
                                            .width(width = 4.dp)
                                            .nothing()
                                    )
                                    Text(
                                        text = item.billTime.commonTimeFormat2(),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        textAlign = TextAlign.Start,
                                    )
                                    AppWidthSpace()
                                    Row(
                                        modifier = Modifier
                                            .wrapContentSize()
                                            .padding(
                                                horizontal = 0.dp,
                                                vertical = APP_PADDING_NORMAL.dp
                                            )
                                            .clickableNoRipple {
                                                AppRouterCoreApi::class
                                                    .routeApi()
                                                    .toBillDetailView(
                                                        context = context,
                                                        billId = item.billId,
                                                    )
                                            }
                                            .nothing(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "查看账单",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                MaterialTheme.colorScheme.onSurface,
                                            ),
                                            textAlign = TextAlign.Start,
                                        )
                                        Icon(
                                            modifier = Modifier
                                                .size(size = 16.dp)
                                                .nothing(),
                                            painter = rememberVectorPainter(image = Icons.Default.KeyboardArrowRight),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        is BillAlbumItemNormalVo -> {
                            item {
                                AppCommonCoilImage(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp, vertical = 0.dp)
                                        .fillMaxWidth()
                                        .aspectRatio(ratio = 1f)
                                        .clip(
                                            shape = MaterialTheme.shapes.small,
                                        )
                                        .clickable {
                                            vm.addIntent(
                                                intent = BillAlbumIntent.ToImagePreview(
                                                    context = context,
                                                    billImageId = item.billImageId,
                                                )
                                            )
                                        }
                                        .nothing(),
                                    imageRes = item.url,
                                )
                            }
                        }
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
fun BillAlbumViewWrap() {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = "账单相册".toStringItemDto(),
                menu1TextValue = "切换账本".toStringItemDto(),
                menu1ClickListener = {
                    AppRouterCoreApi::class
                        .routeApi()
                        .toBookSwitch(
                            context = context,
                        )
                },
            )
        },
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            BillAlbumView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BillAlbumViewPreview() {
    BillAlbumView(
        needInit = false,
    )
}