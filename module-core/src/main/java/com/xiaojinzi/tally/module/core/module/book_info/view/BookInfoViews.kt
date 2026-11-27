package com.xiaojinzi.tally.module.core.module.book_info.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.commonTimeFormat2
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.rememberPainter
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppBackgroundColor
import com.xiaojinzi.tally.lib.res.ui.copyNew
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import com.xiaojinzi.tally.module.core.module.book_info.domain.BookInfoIntent
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BookItemView(
    modifier: Modifier = Modifier,
    vm: BookInfoViewModel,
    item: BookInfoItemVo,
    isSelected: Boolean,
) {
    val context = LocalContext.current
    ConstraintLayout(
        modifier = modifier,
    ) {
        val (
            textCurrentUse,
            icon, nameValue,
            totalSpendingName, totalSpendingValue,
            totalIncomeName, totalIncomeValue,
            totalBillCountValue, textCreateTimeValue,
            menuMore,
        ) = createRefs()

        Icon(
            modifier = Modifier
                .constrainAs(ref = icon) {
                    this.top.linkTo(anchor = parent.top, margin = APP_PADDING_SMALL.dp)
                    this.start.linkTo(anchor = parent.start, margin = APP_PADDING_NORMAL.dp)
                }
                .circleClip()
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        elevation = 1.dp,
                    )
                )
                .padding(all = 6.dp)
                .size(size = 18.dp)
                .nothing(),
            painter = item.icon?.rememberPainter() ?: ColorPainter(
                color = Color.Transparent,
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )

        if (isSelected) {
            Text(
                modifier = Modifier
                    .constrainAs(ref = textCurrentUse) {
                        this.top.linkTo(anchor = parent.top, margin = 0.dp)
                        this.end.linkTo(anchor = parent.end, margin = 0.dp)
                    }
                    .clip(
                        shape = MaterialTheme.shapes.small.copyNew(
                            topStart = 0.dp,
                            bottomEnd = 0.dp,
                        )
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(
                            0.5f,
                        ),
                    )
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 4.dp)
                    .nothing(),
                text = "当前使用",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onPrimary,
                ),
                textAlign = TextAlign.Start,
            )
        }

        Text(
            modifier = Modifier
                .constrainAs(ref = nameValue) {
                    this.centerVerticallyTo(other = icon)
                    this.start.linkTo(anchor = icon.end, margin = APP_PADDING_SMALL.dp)
                }
                .nothing(),
            text = item.bookName?.contentWithComposable().orEmpty(),
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Start,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = totalSpendingName) {
                    this.start.linkTo(anchor = icon.start, margin = 0.dp)
                    this.top.linkTo(anchor = icon.bottom, margin = APP_PADDING_SMALL.dp)
                }
                .nothing(),
            text = "总支出：",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = totalSpendingValue) {
                    this.start.linkTo(anchor = totalSpendingName.end, margin = 0.dp)
                    this.centerVerticallyTo(other = totalSpendingName)
                }
                .nothing(),
            text = item.totalSpending.value.format2f(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = totalIncomeName) {
                    this.start.linkTo(
                        anchor = totalSpendingValue.end,
                        margin = APP_PADDING_SMALL.dp
                    )
                    this.centerVerticallyTo(other = totalSpendingValue)
                }
                .nothing(),
            text = "总收入：",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = totalIncomeValue) {
                    this.start.linkTo(anchor = totalIncomeName.end, margin = 0.dp)
                    this.centerVerticallyTo(other = totalIncomeName)
                }
                .nothing(),
            text = item.totalIncome.value.format2f(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = totalBillCountValue) {
                    this.start.linkTo(anchor = icon.start, margin = 0.dp)
                    this.top.linkTo(
                        anchor = totalSpendingName.bottom,
                        margin = APP_PADDING_SMALL.dp
                    )
                    this.bottom.linkTo(anchor = parent.bottom, margin = APP_PADDING_SMALL.dp)
                }
                .nothing(),
            text = "共${item.billCount}笔账单",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = textCreateTimeValue) {
                    this.start.linkTo(
                        anchor = totalBillCountValue.end,
                        margin = APP_PADDING_NORMAL.dp
                    )
                    this.centerVerticallyTo(other = totalBillCountValue)
                }
                .nothing(),
            text = "创建于：${item.timeCreate.commonTimeFormat2()}",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )

        IconButton(
            modifier = Modifier
                .constrainAs(ref = menuMore) {
                    this.end.linkTo(anchor = parent.end, margin = 0.dp)
                    this.bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                }
                .nothing(),
            onClick = {
                vm.addIntent(
                    intent = BookInfoIntent.OnMoreClick(
                        context = context,
                        bookId = item.bookId,
                    )
                )
            },
        ) {
            Icon(
                modifier = Modifier
                    .nothing(),
                painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_more3),
                contentDescription = "账本操作",
                tint = MaterialTheme.colorScheme.outline,
            )
        }

    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BookInfoView(
    needInit: Boolean? = false,
) {
    val bookSelected by AppServices
        .tallyDataSourceSpi
        .selectedBookStateOb
        .collectAsState(initial = null)
    BusinessContentView<BookInfoViewModel>(
        needInit = needInit,
    ) { vm ->
        val bookList by vm.bookListStateObVo.collectAsState(initial = emptyList())
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = AppBackgroundColor,
                )
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            bookList.forEach { item ->
                val isSelected = item.bookId == bookSelected?.id
                item {
                    BookItemView(
                        modifier = Modifier
                            .padding(
                                horizontal = APP_PADDING_NORMAL.dp,
                                vertical = APP_PADDING_SMALL.dp
                            )
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(
                                shape = MaterialTheme.shapes.small,
                            )
                            .run {
                                if (isSelected) {
                                    this.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            0.5f,
                                        ),
                                        shape = MaterialTheme.shapes.small,
                                    )
                                } else {
                                    this
                                }
                            }
                            .clickable {
                                vm.addIntent(
                                    intent = BookInfoIntent.BookSwitch(
                                        id = item.bookId,
                                    )
                                )
                            }
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                            )
                            .nothing(),
                        vm = vm,
                        item = item,
                        isSelected = isSelected,
                    )
                }
            }
            item {
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(height = 100.dp)
                        .nothing()
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
fun BookInfoViewWrap() {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = "账本管理".toStringItemDto(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    AppRouterCoreApi::class
                        .routeApi()
                        .toBookCrudView(
                            context = context,
                        )
                }
            ) {
                Icon(
                    modifier = Modifier
                        .nothing(),
                    painter = rememberVectorPainter(image = Icons.Sharp.Add),
                    contentDescription = null,
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            BookInfoView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BookInfoViewPreview() {
    BookInfoView(
        needInit = false,
    )
}