@file:OptIn(
    kotlinx.coroutines.InternalCoroutinesApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.xiaojinzi.tally.module.main.module.main.statistics.view

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.nexioui.button.ButtonDefaults
import com.haooz.nexioui.button.FlatTextButton
import com.haooz.nexioui.card.Card
import com.haooz.nexioui.theme.NexioTheme
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.launchIgnoreError
import com.xiaojinzi.tally.lib.res.model.support.DateTimeType
import com.xiaojinzi.tally.lib.res.model.support.rememberPainter
import com.xiaojinzi.tally.lib.res.model.tally.MoneyYuan
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortCardExpand
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortDivider
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.base.theme.appPageBackgroundColor
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCase
import com.xiaojinzi.tally.module.base.view.compose.PieChartView
import com.xiaojinzi.tally.module.base.view.compose.PieChartVo
import com.xiaojinzi.tally.module.base.view.compose.TendencyChatVo
import com.xiaojinzi.tally.module.base.view.compose.TendencyView
import com.xiaojinzi.tally.module.main.module.main.statistics.domain.StatisticsCategoryItemUseCaseDto
import com.xiaojinzi.tally.module.main.module.main.statistics.domain.StatisticsIntent
import com.xiaojinzi.tally.module.main.module.main.statistics.domain.StatisticsLabelItemUseCaseDto
import com.xiaojinzi.tally.module.main.module.main.statistics.domain.StatisticsUseCase
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** 无玻璃采样时沿用 Nexio 的实底胶囊，保留同一套主题和按压反馈。 */
@Composable
private fun StatisticsSegments(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = NexioTheme.colorScheme
    val dimBackground = if (scheme.surface.luminance() < 0.5f) Color.White else Color.Black
    Card(
        modifier = modifier,
        cornerRadius = 100.dp,
        insideMargin = PaddingValues(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                FlatTextButton(
                    text = option,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f).semantics { selected = isSelected },
                    cornerRadius = 100.dp,
                    minWidth = 0.dp,
                    minHeight = 40.dp,
                    insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = ButtonDefaults.textButtonColors(
                        color = if (isSelected) scheme.primary else dimBackground,
                        disabledColor = dimBackground,
                        textColor = if (isSelected) scheme.primary else scheme.onSurfaceVariantSummary,
                        alpha = if (isSelected) 0.1f else 0.06f,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatisticsBreakdownItem(
    title: String,
    amount: String,
    percent: String,
    progress: Float,
    onClick: () -> Unit,
    icon: Painter? = null,
) {
    val scheme = NexioTheme.colorScheme
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "statisticsShare")
    Column {
        PortActionRow(
            title = title,
            subtitle = percent,
            icon = icon,
            onClick = onClick,
            trailing = {
                Text(
                    text = "¥ $amount",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.End,
                )
            },
        )
        LinearProgressIndicator(
            modifier = Modifier
                .padding(start = if (icon == null) 16.dp else 68.dp, end = 16.dp, bottom = 14.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(PortDesign.SmallShape),
            progress = { animatedProgress },
            color = scheme.primary,
            trackColor = scheme.onSurfaceVariantActions.copy(alpha = 0.08f),
        )
    }
}

@Composable
private fun StatisticsMetric(label: String, amount: MoneyYuan?) {
    val scheme = NexioTheme.colorScheme
    val amountText = amount?.value?.format2f() ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontSize = 14.sp, color = scheme.onSurfaceVariantSummary)
        Text(
            text = "¥ $amountText",
            fontSize = if (amountText.length > 13) 20.sp else 28.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun StatisticsEmpty(title: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, fontSize = 17.sp, color = NexioTheme.colorScheme.onSurface)
        Text(
            text = "试试切换账期，或先记下一笔收支。",
            fontSize = 13.sp,
            color = NexioTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun StatisticsPageView(
    vm: StatisticsViewModel,
    type: StatisticsUseCase.StatisticsType,
    amountStatistics: MoneyYuan?,
    amountDayAverageStatistics: MoneyYuan?,
    categoryStatisticsList: List<StatisticsCategoryItemUseCaseDto>,
    labelStatisticsList: List<StatisticsLabelItemUseCaseDto>,
    tendencyChatVo: TendencyChatVo?,
    categoryPieChartVo: PieChartVo?,
    labelPieChartVo: PieChartVo?,
) {
    val context = LocalContext.current
    val scheme = NexioTheme.colorScheme
    val horizontalPadding = if (LocalConfiguration.current.screenWidthDp >= 600) 20.dp else 16.dp
    val categoryType by vm.categoryTypeStateOb.collectAsState(initial = StatisticsUseCase.CategoryType.Big)
    var isShowAllCategory by remember { mutableStateOf(false) }
    var isShowAllLabel by remember { mutableStateOf(false) }
    val openCategory: (StatisticsCategoryItemUseCaseDto) -> Unit = { item ->
        AppRouterCoreApi::class.routeApi().toBillListView(
            context = context,
            title = item.categoryName,
            question = TallyDataSourceSpi.Companion.BillQueryConditionDto(categoryIdList = item.categoryIdList),
        )
    }
    val openLabel: (StatisticsLabelItemUseCaseDto) -> Unit = { item ->
        AppRouterCoreApi::class.routeApi().toBillListView(
            context = context,
            title = item.labelName,
            question = if (item.billIdListForQuery.isNullOrEmpty()) null else {
                TallyDataSourceSpi.Companion.BillQueryConditionDto(idList = item.billIdListForQuery)
            },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PortCard {
            BoxWithConstraints {
                val totalLabel = when (type) {
                    StatisticsUseCase.StatisticsType.Spending -> "支出金额"
                    StatisticsUseCase.StatisticsType.Income -> "收入金额"
                }
                val averageLabel = when (type) {
                    StatisticsUseCase.StatisticsType.Spending -> "日均支出"
                    StatisticsUseCase.StatisticsType.Income -> "日均收入"
                }
                if (maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatisticsMetric(totalLabel, amountStatistics)
                        StatisticsMetric(averageLabel, amountDayAverageStatistics)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f)) { StatisticsMetric(totalLabel, amountStatistics) }
                        Box(Modifier.weight(1f)) { StatisticsMetric(averageLabel, amountDayAverageStatistics) }
                    }
                }
            }
        }
        tendencyChatVo?.let { chart ->
            val bottomNumbers by vm.tendencyChatBottomNumberListStateOb.collectAsState(initial = emptyList())
            val items by rememberUpdatedState(chart.items)
            var selectedIndex by remember { mutableIntStateOf(-1) }
            val selectedItem by remember { derivedStateOf { items.getOrNull(selectedIndex) } }
            PortSectionTitle(title = "收支趋势")
            PortCard {
                Text(
                    text = selectedItem?.let { "${it.timeStr} · ¥ ${it.amount.value.format2f()}" }
                        ?: "点选曲线，查看当日金额",
                    fontSize = 13.sp,
                    color = if (selectedItem == null) scheme.onSurfaceVariantSummary else scheme.primary,
                )
                TendencyView(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(120.dp),
                    vo = chart,
                ) { index -> selectedIndex = index }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    bottomNumbers.forEach { number ->
                        Text(text = "$number", fontSize = 11.sp, color = scheme.onSurfaceVariantSummary)
                    }
                }
            }
        }
        PortSectionTitle(title = "分类分析")
        categoryPieChartVo?.let { chart ->
            PortCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "分类占比",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                    )
                    StatisticsSegments(
                        modifier = Modifier.widthIn(max = 160.dp),
                        options = StatisticsUseCase.CategoryTypeList.map {
                            when (it) {
                                StatisticsUseCase.CategoryType.Big -> "大类"
                                StatisticsUseCase.CategoryType.Small -> "小类"
                            }
                        },
                        selectedIndex = StatisticsUseCase.CategoryTypeList.indexOf(categoryType),
                        onSelect = { index ->
                            vm.addIntent(StatisticsIntent.CategoryTypeChange(StatisticsUseCase.CategoryTypeList[index]))
                        },
                    )
                }
                PieChartView(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp), vo = chart)
            }
        }
        PortCard(contentPadding = PaddingValues(0.dp)) {
            if (categoryStatisticsList.isEmpty()) StatisticsEmpty("这个账期还没有分类记录")
            categoryStatisticsList.take(StatisticsUseCase.CATEGORY_MIN_COUNT).forEachIndexed { index, item ->
                if (index > 0) PortDivider()
                StatisticsBreakdownItem(
                    title = item.categoryName?.contentWithComposable().orEmpty(),
                    amount = item.amount.value.format2f(),
                    percent = "${(item.percent * 100).format2f()}%",
                    progress = item.percentForView,
                    icon = item.icon?.rememberPainter(),
                    onClick = { openCategory(item) },
                )
            }
            PortCardExpand(visible = isShowAllCategory) {
                categoryStatisticsList.drop(StatisticsUseCase.CATEGORY_MIN_COUNT).forEach { item ->
                    PortDivider()
                    StatisticsBreakdownItem(
                        title = item.categoryName?.contentWithComposable().orEmpty(),
                        amount = item.amount.value.format2f(),
                        percent = "${(item.percent * 100).format2f()}%",
                        progress = item.percentForView,
                        icon = item.icon?.rememberPainter(),
                        onClick = { openCategory(item) },
                    )
                }
            }
            if (categoryStatisticsList.size > StatisticsUseCase.CATEGORY_MIN_COUNT) {
                PortSecondaryButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp),
                    text = if (isShowAllCategory) "收起" else "显示更多",
                    onClick = { isShowAllCategory = !isShowAllCategory },
                )
            }
        }
        PortSectionTitle(title = "标签分析")
        labelPieChartVo?.let { chart ->
            PortCard {
                Text(
                    text = "标签占比",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                )
                PieChartView(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp), vo = chart)
            }
        }
        PortCard(contentPadding = PaddingValues(0.dp)) {
            if (labelStatisticsList.isEmpty()) StatisticsEmpty("这个账期还没有标签记录")
            labelStatisticsList.take(StatisticsUseCase.LABEL_MIN_COUNT).forEachIndexed { index, item ->
                if (index > 0) PortDivider()
                StatisticsBreakdownItem(
                    title = item.labelName?.contentWithComposable().orEmpty(),
                    amount = item.amount.value.format2f(),
                    percent = "${(item.percent * 100).format2f()}%",
                    progress = item.percentForView,
                    onClick = { openLabel(item) },
                )
            }
            PortCardExpand(visible = isShowAllLabel) {
                labelStatisticsList.drop(StatisticsUseCase.LABEL_MIN_COUNT).forEach { item ->
                    PortDivider()
                    StatisticsBreakdownItem(
                        title = item.labelName?.contentWithComposable().orEmpty(),
                        amount = item.amount.value.format2f(),
                        percent = "${(item.percent * 100).format2f()}%",
                        progress = item.percentForView,
                        onClick = { openLabel(item) },
                    )
                }
            }
            if (labelStatisticsList.size > StatisticsUseCase.LABEL_MIN_COUNT) {
                PortSecondaryButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp),
                    text = if (isShowAllLabel) "收起" else "显示更多",
                    onClick = { isShowAllLabel = !isShowAllLabel },
                )
            }
        }
    }
}

@Composable
private fun StatisticsView(needInit: Boolean? = true) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scheme = NexioTheme.colorScheme
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val currentBookInfo by AppServices.tallyDataSourceSpi.selectedBookStateOb.collectAsState(initial = null)
    BusinessContentView<StatisticsViewModel>(needInit = needInit) { vm ->
        val timeTypeSelected by vm.timeTypeSelectedStateOb.collectAsState(initial = null)
        val selectYear by vm.timeSelectUseCase.selectedYearStateOb.collectAsState(initial = null)
        val selectMonth by vm.timeSelectUseCase.selectedMonthStateOb.collectAsState(initial = null)
        val spendingStatistics by vm.spendingStatisticsStateOb.collectAsState(initial = null)
        val incomeStatistics by vm.incomeStatisticsStateOb.collectAsState(initial = null)
        val spendingDayAverageStatistics by vm.spendingDayAverageStatisticsStateOb.collectAsState(initial = null)
        val incomeDayAverageStatistics by vm.incomeDayAverageStatisticsStateOb.collectAsState(initial = null)
        val categorySpendingStatisticsList by vm.allCategorySpendingStatisticsStateOb.collectAsState(initial = emptyList())
        val categoryIncomeStatisticsList by vm.allCategoryIncomeStatisticsStateOb.collectAsState(initial = emptyList())
        val labelSpendingStatisticsList by vm.allLabelSpendingStatisticsStateOb.collectAsState(initial = emptyList())
        val labelIncomeStatisticsList by vm.allLabelIncomeStatisticsStateOb.collectAsState(initial = emptyList())
        val tendencyChatSpendingVo by vm.tendencyChatSpendingStatisticsStateObVo.collectAsState(initial = null)
        val tendencyChatIncomeVo by vm.tendencyChatIncomeStatisticsStateObVo.collectAsState(initial = null)
        val categorySpendingPieChart by vm.categorySpendingPieChartStateObVo.collectAsState(initial = null)
        val categoryIncomePieChart by vm.categoryIncomePieChartStateObVo.collectAsState(initial = null)
        val labelSpendingPieChart by vm.labelSpendingPieChartStateObVo.collectAsState(initial = null)
        val labelIncomePieChart by vm.labelIncomePieChartStateObVo.collectAsState(initial = null)
        val pageState = rememberPagerState { StatisticsUseCase.Tabs.size }
        LaunchedEffect(pageState) {
            snapshotFlow { pageState.currentPage }.onEach {
                when (it) {
                    0 -> vm.tabSelectedStateOb.emit(StatisticsUseCase.Tab.Spending)
                    1 -> vm.tabSelectedStateOb.emit(StatisticsUseCase.Tab.Income)
                }
            }.launchIn(this)
        }
        Box(
            modifier = Modifier.fillMaxSize().background(appPageBackgroundColor()).windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = PortDesign.MaxContentWidth).fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding)
                        .padding(top = if (isTablet) 24.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.weight(1f).semantics { heading() },
                            text = "统计",
                            style = MaterialTheme.typography.headlineLarge,
                            color = scheme.onSurface,
                        )
                        PortSecondaryButton(
                            text = when (timeTypeSelected) {
                                StatisticsUseCase.TimeType.Year -> "年账单"
                                StatisticsUseCase.TimeType.Month -> "月账单"
                                else -> "账单"
                            },
                            onClick = { vm.addIntent(StatisticsIntent.ToBillList(context = context)) },
                        )
                    }
                    PortCard(contentPadding = PaddingValues(0.dp)) {
                        PortActionRow(
                            title = currentBookInfo?.name ?: "选择账本",
                            icon = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_book3),
                            onClick = { AppRouterCoreApi::class.routeApi().toBookSwitch(context = context) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Card(
                            modifier = Modifier.weight(1.5f),
                            cornerRadius = 100.dp,
                            insideMargin = PaddingValues(0.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.addIntent(StatisticsIntent.YearOrMonthAdjust(value = -1)) }) {
                                    Icon(
                                        painter = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_left1),
                                        contentDescription = "上一账期",
                                        modifier = Modifier.size(20.dp),
                                        tint = scheme.onSurfaceVariantActions,
                                    )
                                }
                                FlatTextButton(
                                    text = when (timeTypeSelected) {
                                        StatisticsUseCase.TimeType.Month -> "${selectYear ?: "—"}年${selectMonth?.plus(1) ?: "—"}月"
                                        StatisticsUseCase.TimeType.Year -> "${selectYear ?: "—"}年"
                                        else -> "选择账期"
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).padding(horizontal = 4.dp),
                                    onClick = {
                                        val dateTimeType = when (timeTypeSelected) {
                                            StatisticsUseCase.TimeType.Year -> DateTimeType.Year
                                            StatisticsUseCase.TimeType.Month -> DateTimeType.Month
                                            null -> null
                                        }
                                        dateTimeType?.let {
                                            vm.timeSelectUseCase.addIntent(
                                                TimeSelectUseCase.Intent.DateTimeSelect(context = context, dateTimeType = it),
                                            )
                                        }
                                    },
                                    minWidth = 0.dp,
                                    minHeight = 48.dp,
                                    insideMargin = PaddingValues(0.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = scheme.onSurface),
                                )
                                IconButton(onClick = { vm.addIntent(StatisticsIntent.YearOrMonthAdjust(value = 1)) }) {
                                    Icon(
                                        painter = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_right1),
                                        contentDescription = "下一账期",
                                        modifier = Modifier.size(20.dp),
                                        tint = scheme.onSurfaceVariantActions,
                                    )
                                }
                            }
                        }
                        StatisticsSegments(
                            modifier = Modifier.weight(1f),
                            options = listOf("月", "年"),
                            selectedIndex = timeTypeSelected?.index ?: -1,
                            onSelect = { index ->
                                vm.timeTypeSelectedStateOb.value = when (index) {
                                    0 -> StatisticsUseCase.TimeType.Month
                                    else -> StatisticsUseCase.TimeType.Year
                                }
                            },
                        )
                    }
                    StatisticsSegments(
                        modifier = Modifier.fillMaxWidth(),
                        options = listOf("支出", "收入"),
                        selectedIndex = pageState.currentPage,
                        onSelect = { index -> scope.launchIgnoreError { pageState.animateScrollToPage(index) } },
                    )
                }
                HorizontalPager(modifier = Modifier.fillMaxWidth().weight(1f), state = pageState) { pageIndex ->
                    when (pageIndex) {
                        0 -> StatisticsPageView(
                            vm = vm,
                            type = StatisticsUseCase.StatisticsType.Spending,
                            amountStatistics = spendingStatistics,
                            amountDayAverageStatistics = spendingDayAverageStatistics,
                            categoryStatisticsList = categorySpendingStatisticsList,
                            labelStatisticsList = labelSpendingStatisticsList,
                            tendencyChatVo = tendencyChatSpendingVo,
                            categoryPieChartVo = categorySpendingPieChart,
                            labelPieChartVo = labelSpendingPieChart,
                        )
                        1 -> StatisticsPageView(
                            vm = vm,
                            type = StatisticsUseCase.StatisticsType.Income,
                            amountStatistics = incomeStatistics,
                            amountDayAverageStatistics = incomeDayAverageStatistics,
                            categoryStatisticsList = categoryIncomeStatisticsList,
                            labelStatisticsList = labelIncomeStatisticsList,
                            tendencyChatVo = tendencyChatIncomeVo,
                            categoryPieChartVo = categoryIncomePieChart,
                            labelPieChartVo = labelIncomePieChart,
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
fun StatisticsViewWrap() {
    PortTheme { StatisticsView() }
}

@Preview
@Composable
private fun StatisticsViewPreview() {
    PortTheme { StatisticsView(needInit = false) }
}
