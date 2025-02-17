package com.xiaojinzi.tally.module.main.module.main.bill.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.DateTimeType
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.module.base.module.common_bill_list.view.CommonBillListView
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCase
import com.xiaojinzi.tally.module.base.view.compose.AppPullRefreshView
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import com.xiaojinzi.tally.module.main.module.main.bill.domain.BillIntent
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BillView(
    needInit: Boolean? = null,
) {
    val context = LocalContext.current
    val isHomeBillStatVisible by AppServices.appConfigSpi.isHomeBillStatVisibleStateOb.collectAsState(
        initial = false
    )
    val bookSelected by AppServices
        .tallyDataSourceSpi
        .selectedBookStateOb
        .collectAsState(initial = null)
    BusinessContentView<BillViewModel>(
        needInit = needInit,
    ) { vm ->
        val selectedYear by vm.timeSelectUseCase.selectedYearStateOb.collectAsState(initial = null)
        val selectedMonth by vm.timeSelectUseCase.selectedMonthStateOb.collectAsState(initial = null)
        val currentMonthIncome by vm.currentMonthIncomeStateOb.collectAsState(
            initial = null
        )
        val currentMonthSpending by vm.currentMonthSpendingStateOb.collectAsState(
            initial = null
        )
        val currentMonthBalance by vm.currentMonthBalanceStateOb.collectAsState(
            initial = null
        )
        AppPullRefreshView(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // color = MaterialTheme.colorScheme.background,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        elevation = 1.dp,
                    ),
                )
                .nothing(),
            onRefresh = {
                bookSelected?.id?.run {
                    AppServices
                        .tallyDataSyncSpi
                        ?.trySyncSingleBook(
                            bookId = this,
                        )
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nothing(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 顶部显示账本 标题 和 搜索按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .statusBarsPadding()
                        .nothing(),
                ) {
                    Row(
                        modifier = Modifier
                            .align(alignment = Alignment.CenterStart)
                            .wrapContentSize()
                            .clickableNoRipple {
                                vm.addIntent(
                                    intent = BillIntent.BookSelect(
                                        context = context,
                                    )
                                )
                            }
                            .padding(start = APP_PADDING_NORMAL.dp)
                            .nothing(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier
                                .scale(scale = 0.8f)
                                .size(size = 20.dp)
                                .nothing(),
                            painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_book3),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            modifier = Modifier
                                .nothing(),
                            text = bookSelected?.name.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                    Text(
                        modifier = Modifier
                            .align(alignment = Alignment.Center)
                            .wrapContentSize()
                            .nothing(),
                        text = "一刻记账",
                        fontFamily = FontFamily(Font(com.xiaojinzi.tally.lib.res.R.font.res_font_xdks)),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        textAlign = TextAlign.Start,
                    )
                    IconButton(
                        modifier = Modifier
                            .align(alignment = Alignment.CenterEnd)
                            .nothing(),
                        onClick = {
                            AppRouterCoreApi::class
                                .routeApi()
                                .toBillSearchView(
                                    context = context,
                                )
                        },
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(size = 18.dp)
                                .nothing(),
                            painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_search1),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                val initPageIndex = Int.MAX_VALUE / 2
                val state = rememberPagerState(
                    initialPage = initPageIndex,
                ) {
                    Int.MAX_VALUE
                }
                var lastPageIndex by remember {
                    mutableIntStateOf(value = initPageIndex)
                }
                LaunchedEffect(key1 = state) {
                    snapshotFlow { state.currentPage }
                        .onEach { pageIndex ->
                            vm.timeSelectUseCase.addIntent(
                                intent = TimeSelectUseCase.Intent.MonthAdjust(
                                    value = pageIndex - lastPageIndex,
                                )
                            )
                            lastPageIndex = pageIndex
                        }
                        .launchIn(this)
                }
                HorizontalPager(
                    modifier = Modifier
                        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(
                            shape = MaterialTheme.shapes.medium,
                        )
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.6f,
                                    ),
                                    MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        )
                        .nothing(),
                    state = state,
                ) {
                    // 当月的支出和收入统计信息
                    ConstraintLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .nothing(),
                    ) {
                        val (
                            iconEye, timeView,
                            textCurrentMonthSpendingName, textCurrentMonthSpendingValue,
                            textCurrentMonthIncomeName, textCurrentMonthIncomeValue,
                            textCurrentMonthBalanceName, textCurrentMonthBalanceValue,
                        ) = createRefs()
                        val line1 = createEndBarrier(
                            textCurrentMonthSpendingName, textCurrentMonthSpendingValue,
                            textCurrentMonthIncomeName, textCurrentMonthIncomeValue,
                        )

                        Text(
                            modifier = Modifier
                                .constrainAs(ref = timeView) {
                                    this.end.linkTo(
                                        anchor = parent.end,
                                        margin = APP_PADDING_NORMAL.dp,
                                    )
                                    this.top.linkTo(
                                        anchor = parent.top,
                                        margin = APP_PADDING_SMALL.dp,
                                    )
                                }
                                .circleClip()
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                )
                                .clickable {
                                    vm.timeSelectUseCase.addIntent(
                                        intent = TimeSelectUseCase.Intent.DateTimeSelect(
                                            context = context,
                                            dateTimeType = DateTimeType.Month,
                                        )
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                .nothing(),
                            text = "${selectedYear ?: "--"}年${selectedMonth?.plus(1) ?: "--"}月",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.8f,
                                ),
                            ),
                            textAlign = TextAlign.Start,
                        )

                        Text(
                            modifier = Modifier
                                .constrainAs(ref = textCurrentMonthSpendingName) {
                                    this.start.linkTo(
                                        anchor = parent.start,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                    this.centerVerticallyTo(other = iconEye)
                                }
                                .nothing(),
                            text = "本月支出",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.outline,
                            ),
                            textAlign = TextAlign.Start,
                        )

                        IconButton(
                            modifier = Modifier
                                .scale(scale = 0.9f)
                                .constrainAs(ref = iconEye) {
                                    this.start.linkTo(
                                        anchor = textCurrentMonthSpendingName.end,
                                        margin = 0.dp,
                                    )
                                    this.top.linkTo(
                                        anchor = parent.top,
                                        margin = 0.dp
                                    )
                                }
                                .nothing(),
                            onClick = {
                                AppServices
                                    .appConfigSpi
                                    .switchHomeBillStatVisible(
                                        b = !isHomeBillStatVisible,
                                    )
                            },
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(size = 16.dp)
                                    .nothing(),
                                painter = painterResource(
                                    id = if (isHomeBillStatVisible) {
                                        com.xiaojinzi.tally.lib.res.R.drawable.res_eye1
                                    } else {
                                        com.xiaojinzi.tally.lib.res.R.drawable.res_eye1_closed
                                    }
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Text(
                            modifier = Modifier
                                .constrainAs(ref = textCurrentMonthSpendingValue) {
                                    this.start.linkTo(
                                        anchor = textCurrentMonthSpendingName.start,
                                        margin = 0.dp
                                    )
                                    this.top.linkTo(
                                        anchor = textCurrentMonthSpendingName.bottom,
                                        margin = APP_PADDING_SMALL.dp
                                    )
                                }
                                .nothing(),
                            text = if (isHomeBillStatVisible) {
                                (currentMonthSpending?.toYuan()?.value ?: 0f).format2f()
                            } else {
                                "*****"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.2f,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            textAlign = TextAlign.Start,
                        )
                        Text(
                            modifier = Modifier
                                .constrainAs(ref = textCurrentMonthIncomeName) {
                                    this.start.linkTo(
                                        anchor = parent.start,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                    this.top.linkTo(
                                        anchor = textCurrentMonthSpendingValue.bottom,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                }
                                .nothing(),
                            text = "本月收入",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.outline,
                            ),
                            textAlign = TextAlign.Start,
                        )
                        Text(
                            modifier = Modifier
                                .constrainAs(ref = textCurrentMonthIncomeValue) {
                                    this.start.linkTo(
                                        anchor = textCurrentMonthIncomeName.start,
                                        margin = 0.dp
                                    )
                                    this.top.linkTo(
                                        anchor = textCurrentMonthIncomeName.bottom,
                                        margin = APP_PADDING_SMALL.dp
                                    )
                                    this.bottom.linkTo(
                                        anchor = parent.bottom,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                }
                                .nothing(),
                            text = if (isHomeBillStatVisible) {
                                (currentMonthIncome?.toYuan()?.value ?: 0f).format2f()
                            } else {
                                "*****"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.8f,
                                ),
                            ),
                            textAlign = TextAlign.Start,
                        )
                        Text(
                            modifier = Modifier
                                .constrainAs(ref = textCurrentMonthBalanceName) {
                                    this.start.linkTo(
                                        anchor = line1,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                    this.top.linkTo(
                                        anchor = textCurrentMonthIncomeName.top,
                                        margin = 0.dp
                                    )
                                }
                                .nothing(),
                            text = "本月结余",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.outline,
                            ),
                            textAlign = TextAlign.Start,
                        )
                        Text(
                            modifier = Modifier
                                .constrainAs(ref = textCurrentMonthBalanceValue) {
                                    this.start.linkTo(
                                        anchor = textCurrentMonthBalanceName.start,
                                        margin = 0.dp
                                    )
                                    this.top.linkTo(
                                        anchor = textCurrentMonthBalanceName.bottom,
                                        margin = APP_PADDING_SMALL.dp
                                    )
                                    this.bottom.linkTo(
                                        anchor = parent.bottom,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                }
                                .nothing(),
                            text = if (isHomeBillStatVisible) {
                                (currentMonthBalance?.toYuan()?.value
                                    ?: 0f).format2f()
                            } else {
                                "*****"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.8f,
                                ),
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
                CommonBillListView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .nothing(),
                    commonBillListViewUseCase = vm.commonBillListViewUseCase,
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
fun BillViewWrap() {
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = "hello".toStringItemDto(),
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            BillView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BillViewPreview() {
    BillView(
        needInit = false,
    )
}