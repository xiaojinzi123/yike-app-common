@file:OptIn(
    kotlinx.coroutines.InternalCoroutinesApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.xiaojinzi.tally.module.main.module.main.calendar.view

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.nexioui.theme.NexioTheme
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.GridView
import com.xiaojinzi.support.ktx.NormalMutableSharedFlow
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.getDayInterval
import com.xiaojinzi.support.ktx.getDayOfMonth
import com.xiaojinzi.support.ktx.getMonthByTimeStamp
import com.xiaojinzi.support.ktx.getYearByTimeStamp
import com.xiaojinzi.tally.lib.res.model.support.DateTimeType
import com.xiaojinzi.tally.module.base.module.common_bill_list.view.CommonBillListView
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.base.theme.appPageBackgroundColor
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCase
import com.xiaojinzi.tally.module.main.module.main.calendar.domain.CalendarUseCase
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.Calendar

private val TodayClickEventOb = NormalMutableSharedFlow<Unit>()

@Composable
private fun CalendarView(needInit: Boolean? = null) {
    val context = LocalContext.current
    val scheme = NexioTheme.colorScheme
    val horizontalPadding = if (LocalConfiguration.current.screenWidthDp >= 600) 20.dp else 16.dp
    val bookSelected by AppServices.tallyDataSourceSpi.selectedBookStateOb.collectAsState(initial = null)
    BusinessContentView<CalendarViewModel>(needInit = needInit) { vm ->
        val monthPageListData by vm.monthPageListDataStateOb.collectAsState(initial = null)
        val pageYear by vm.timeSelectUseCase.selectedYearStateOb.collectAsState(initial = null)
        val pageMonth by vm.timeSelectUseCase.selectedMonthStateOb.collectAsState(initial = null)
        val bills by vm.commonBillListViewUseCase.billListStateObVo.collectAsState(initial = null)
        var selectedYear by remember {
            mutableIntStateOf(getYearByTimeStamp(timeStamp = System.currentTimeMillis()))
        }
        var selectedMonth by remember {
            mutableIntStateOf(getMonthByTimeStamp(timeStamp = System.currentTimeMillis()))
        }
        var selectedDayOfMonth by remember {
            mutableIntStateOf(getDayOfMonth(timeStamp = System.currentTimeMillis()))
        }
        Box(
            modifier = Modifier.fillMaxSize().background(appPageBackgroundColor()).windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            val data = monthPageListData
            if (data == null || data.monthPageList.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = scheme.primary)
            } else {
                val pageState = rememberPagerState(initialPage = data.defaultMonthIndex) { data.monthPageList.size }
                LaunchedEffect(pageState) {
                    snapshotFlow { pageState.currentPage }
                        .map { data.monthPageList.getOrNull(it) }
                        .filterNotNull()
                        .onEach {
                            vm.timeSelectUseCase.addIntent(
                                TimeSelectUseCase.Intent.YearAndMonthSet(year = it.year, month = it.month),
                            )
                        }.launchIn(this)
                    snapshotFlow { pageYear to pageMonth }
                        .map { pair -> data.monthPageList.indexOfFirst { it.year == pair.first && it.month == pair.second } }
                        .filter { it > -1 }
                        .onEach { if (pageState.currentPage != it) pageState.scrollToPage(it) }
                        .launchIn(this)
                    TodayClickEventOb.onEach {
                        val currentTime = System.currentTimeMillis()
                        selectedYear = getYearByTimeStamp(timeStamp = currentTime)
                        selectedMonth = getMonthByTimeStamp(timeStamp = currentTime)
                        selectedDayOfMonth = getDayOfMonth(timeStamp = currentTime)
                        pageState.scrollToPage(data.defaultMonthIndex)
                        vm.timeSelectUseCase.addIntent(TimeSelectUseCase.Intent.ResetTime)
                    }.launchIn(this)
                }
                LaunchedEffect(Unit) {
                    snapshotFlow {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = 0
                        calendar[Calendar.HOUR_OF_DAY] = 0
                        calendar[Calendar.YEAR] = selectedYear
                        calendar[Calendar.MONTH] = selectedMonth
                        calendar[Calendar.DAY_OF_MONTH] = selectedDayOfMonth
                        calendar.timeInMillis to bookSelected
                    }.onEach { pair ->
                        val (dayStartTime, dayEndTime) = getDayInterval(timeStamp = pair.first)
                        vm.billQueryConditionUseCase.queryConditionStateOb.emit(
                            TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                bookIdList = pair.second?.let { listOf(it.id) } ?: emptyList(),
                                startTimeInclude = dayStartTime,
                                endTimeInclude = dayEndTime,
                            ),
                        )
                    }.launchIn(this)
                }
                val addBill: (Int, Int, Int) -> Unit = { year, month, day ->
                    val calendar = Calendar.getInstance()
                    calendar[Calendar.YEAR] = year
                    calendar[Calendar.MONTH] = month
                    calendar[Calendar.DAY_OF_MONTH] = day
                    AppRouterCoreApi::class.routeApi().toBillCrudView(
                        context = context,
                        initTime = calendar.timeInMillis,
                    )
                }
                val calendarHeader: @Composable () -> Unit = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding).padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PortSecondaryButton(
                                text = "${pageYear ?: "—"}年${pageMonth?.plus(1) ?: "—"}月 ▾",
                                onClick = {
                                    vm.timeSelectUseCase.addIntent(
                                        TimeSelectUseCase.Intent.DateTimeSelect(context = context, dateTimeType = DateTimeType.Month),
                                    )
                                },
                            )
                            Spacer(Modifier.weight(1f))
                            PortSecondaryButton(text = "今天", onClick = { TodayClickEventOb.add(value = Unit) })
                        }
                        PortCard(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                CalendarUseCase.CalendarTitleList.forEach { title ->
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = title,
                                        fontSize = 13.sp,
                                        color = scheme.onSurfaceVariantSummary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            HorizontalPager(
                                modifier = Modifier.fillMaxWidth(),
                                state = pageState,
                                beyondViewportPageCount = 1,
                                verticalAlignment = Alignment.Top,
                            ) { pageIndex ->
                                data.monthPageList.getOrNull(pageIndex)?.let { month ->
                                    val days by vm.subscribeMonthDayList(year = month.year, month = month.month)
                                        .collectAsState(initial = emptyList())
                                    GridView(items = days, columnNumber = 7, verticalSpace = 4.dp) { item ->
                                        item?.let { day ->
                                            val isSelected = day.year == selectedYear && day.month == selectedMonth &&
                                                day.dayOfMonth == selectedDayOfMonth
                                            val amount = day.amount.value.format2f()
                                            val contentColor = when {
                                                isSelected -> scheme.onPrimary
                                                day.isToday -> scheme.primary
                                                else -> scheme.onSurface
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .padding(horizontal = 2.dp)
                                                    .fillMaxWidth()
                                                    .clip(PortDesign.SmallShape)
                                                    .background(
                                                        when {
                                                            isSelected -> scheme.primary
                                                            day.isToday -> scheme.primary.copy(alpha = 0.08f)
                                                            else -> Color.Transparent
                                                        },
                                                    )
                                                    .semantics {
                                                        selected = isSelected
                                                        contentDescription = "${day.year}年${day.month + 1}月${day.dayOfMonth}日，金额 $amount"
                                                    }
                                                    .combinedClickable(
                                                        role = Role.Button,
                                                        onClick = {
                                                            selectedYear = day.year
                                                            selectedMonth = day.month
                                                            selectedDayOfMonth = day.dayOfMonth
                                                        },
                                                        onLongClick = { addBill(day.year, day.month, day.dayOfMonth) },
                                                        onLongClickLabel = "在这一天记一笔",
                                                    )
                                                    .heightIn(min = 60.dp)
                                                    .padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                                            ) {
                                                Text(
                                                    text = "${day.dayOfMonth}",
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = contentColor,
                                                )
                                                Text(
                                                    text = amount,
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariantSummary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = "左右滑动切换月份，长按日期记一笔。",
                            fontSize = 13.sp,
                            color = scheme.onSurfaceVariantSummary,
                        )
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PortSectionTitle(
                                modifier = Modifier.weight(1f),
                                title = "${selectedMonth + 1}月${selectedDayOfMonth}日的账单",
                                subtitle = "$selectedYear 年 · ${bookSelected?.name ?: "当前账本"}",
                            )
                            PortSecondaryButton(
                                text = "记一笔",
                                onClick = { addBill(selectedYear, selectedMonth, selectedDayOfMonth) },
                            )
                        }
                    }
                }
                Box(modifier = Modifier.widthIn(max = PortDesign.MaxContentWidth).fillMaxSize()) {
                    if (bills.isNullOrEmpty()) {
                        // 空列表不渲染 CommonBillListView 的 header，保留完整可滚动日历。
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            calendarHeader()
                            PortCard(
                                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp),
                            ) {
                                if (bills == null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = scheme.primary)
                                        Text(text = "正在读取账单", fontSize = 14.sp, color = scheme.onSurfaceVariantSummary)
                                    }
                                } else {
                                    Text(text = "这一天还没有账单", fontSize = 17.sp, color = scheme.onSurface)
                                    Text(
                                        modifier = Modifier.padding(top = 8.dp),
                                        text = "选择其他日期回看，或为这一天记下一笔。",
                                        fontSize = 13.sp,
                                        color = scheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    } else {
                        CommonBillListView(
                            modifier = Modifier.fillMaxSize(),
                            headerContent = calendarHeader,
                            commonBillListViewUseCase = vm.commonBillListViewUseCase,
                        )
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
fun CalendarViewWrap() {
    PortTheme { CalendarView() }
}

@Preview
@Composable
private fun CalendarViewPreview() {
    PortTheme { CalendarView(needInit = false) }
}
