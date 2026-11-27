package com.xiaojinzi.tally.module.main.module.main.bill.view

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.haooz.nexioui.theme.NexioTheme
import com.xiaojinzi.tally.module.base.theme.PortCardExpand
import com.xiaojinzi.tally.module.base.theme.PortDivider
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortStatusPill
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.tally.lib.res.model.support.DateTimeType
import com.xiaojinzi.tally.module.base.module.common_bill_list.view.CommonBillListView
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortPrimaryButton
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.base.theme.appPageBackgroundColor
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCase
import com.xiaojinzi.tally.module.main.module.main.bill.domain.BillIntent
import com.xiaojinzi.tally.module.main.module.main.calendar.view.CalendarAct
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.Locale

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BillView(
    needInit: Boolean? = null,
    onOpenCalendar: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val horizontalPadding = if (LocalConfiguration.current.screenWidthDp >= 600) 20.dp else 16.dp
    val amountsVisible by AppServices.appConfigSpi.isHomeBillStatVisibleStateOb.collectAsState(
        initial = false,
    )
    val bookSelected by AppServices.tallyDataSourceSpi.selectedBookStateOb.collectAsState(initial = null)
    BusinessContentView<BillViewModel>(needInit = needInit) { vm ->
        val selectedYear by vm.timeSelectUseCase.selectedYearStateOb.collectAsState(initial = null)
        val selectedMonth by vm.timeSelectUseCase.selectedMonthStateOb.collectAsState(initial = null)
        val spending by vm.currentMonthSpendingStateOb.collectAsState(initial = null)
        val income by vm.currentMonthIncomeStateOb.collectAsState(initial = null)
        val balance by vm.currentMonthBalanceStateOb.collectAsState(initial = null)
        val budget by vm.currentMonthBudgetStateOb.collectAsState(initial = null)
        val categoryProgress by vm.currentMonthCategoryBudgetStateOb.collectAsState(initial = null)
        val bills by vm.commonBillListViewUseCase.billListStateObVo.collectAsState(initial = null)
        val budgetSummary = remember(amountsVisible, budget, spending, categoryProgress) {
            BillBudgetSummaryVo.create(
                amountsVisible = amountsVisible,
                budgetFen = budget?.value,
                spendingFen = spending?.value,
                categoryProgress = categoryProgress,
            )
        }
        val monthLabel = "${selectedYear ?: "—"} 年 ${(selectedMonth?.plus(1)) ?: "—"} 月"
        val canOpenBudget = bookSelected != null && selectedYear != null && selectedMonth != null
        val onAddBill = {
            AppRouterCoreApi::class.routeApi().toBillCrudView(context = context)
        }
        val onSearch = {
            AppRouterCoreApi::class.routeApi().toBillSearchView(context = context)
        }
        val openCalendar = onOpenCalendar ?: {
            context.startActivity(Intent(context, CalendarAct::class.java))
        }
        val header: @Composable () -> Unit = {
            BillHomeHeader(
                bookName = bookSelected?.name ?: "选择账本",
                monthLabel = monthLabel,
                amountsVisible = amountsVisible,
                spendingFen = spending?.value,
                incomeFen = income?.value,
                balanceFen = balance?.value,
                budgetSummary = budgetSummary,
                canOpenBudget = canOpenBudget,
                onSelectBook = { vm.addIntent(BillIntent.BookSelect(context = context)) },
                onSearch = onSearch,
                onSelectMonth = {
                    vm.timeSelectUseCase.addIntent(
                        TimeSelectUseCase.Intent.DateTimeSelect(
                            context = context,
                            dateTimeType = DateTimeType.Month,
                        ),
                    )
                },
                onToggleAmounts = {
                    AppServices.appConfigSpi.switchHomeBillStatVisible(b = !amountsVisible)
                },
                onOpenBudget = {
                    val year = selectedYear
                    val month = selectedMonth
                    if (bookSelected != null && year != null && month != null) {
                        Router.with(context = context)
                            .hostAndPath(hostAndPath = AppRouterConfig.QIANJI_BUDGET)
                            .putString("bookId", bookSelected?.id)
                            .putString("month", String.format(Locale.CHINA, "%04d-%02d", year, month + 1))
                            .forward()
                    }
                },
                onAddBill = onAddBill,
                onAutoRecord = {
                    Router.with(context).hostAndPath(AppRouterConfig.QIANJI_AUTO_RECORD).forward()
                },
                onImport = {
                    Router.with(context).hostAndPath(AppRouterConfig.QIANJI_IMPORT).forward()
                },
                onOpenCalendar = openCalendar,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appPageBackgroundColor())
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = PortDesign.MaxContentWidth)
                    .fillMaxSize(),
            ) {
                if (bills.isNullOrEmpty()) {
                    // CommonBillListView 的空状态不渲染 headerContent；在这里保留完整首页。
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        header()
                        PortCard(
                            modifier = Modifier.padding(
                                horizontal = horizontalPadding,
                                vertical = PortDesign.SectionGap,
                            ),
                        ) {
                            if (bills == null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(PortDesign.ItemGap),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(PortDesign.IconSize),
                                        strokeWidth = 2.dp,
                                    )
                                    Text("正在读取账单", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                Icon(
                                    modifier = Modifier.size(36.dp),
                                    painter = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_book3),
                                    contentDescription = null,
                                    tint = NexioTheme.colorScheme.primary,
                                )
                                Text(
                                    modifier = Modifier.padding(top = 12.dp),
                                    text = "这个月还没有账单",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NexioTheme.colorScheme.onSurface,
                                )
                                Text(
                                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                                    text = "从第一笔收支开始，慢慢看清每个月的生活。",
                                    fontSize = 14.sp,
                                    color = NexioTheme.colorScheme.onSurfaceVariantSummary,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PortSecondaryButton(text = "记第一笔", onClick = onAddBill)
                                    PortSecondaryButton(
                                        text = "导入账单",
                                        onClick = {
                                            Router.with(context).hostAndPath(AppRouterConfig.QIANJI_IMPORT).forward()
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(PortDesign.SectionGap))
                    }
                } else {
                    CommonBillListView(
                        modifier = Modifier.fillMaxSize(),
                        headerContent = header,
                        commonBillListViewUseCase = vm.commonBillListViewUseCase,
                    )
                }
            }
        }
    }
}

@Composable
private fun BillHomeHeader(
    bookName: String,
    monthLabel: String,
    amountsVisible: Boolean,
    spendingFen: Long?,
    incomeFen: Long?,
    balanceFen: Long?,
    budgetSummary: BillBudgetSummaryVo,
    canOpenBudget: Boolean,
    onSelectBook: () -> Unit,
    onSearch: () -> Unit,
    onSelectMonth: () -> Unit,
    onToggleAmounts: () -> Unit,
    onOpenBudget: () -> Unit,
    onAddBill: () -> Unit,
    onAutoRecord: () -> Unit,
    onImport: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val scheme = NexioTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = if (isTablet) 24.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = "账单",
                    style = MaterialTheme.typography.headlineLarge,
                    color = scheme.onSurface,
                )
                Text(
                    text = "每一笔，都有迹可循",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
            IconButton(
                modifier = Modifier.size(PortDesign.TouchTarget),
                onClick = onSearch,
            ) {
                Icon(
                    modifier = Modifier.size(PortDesign.IconSize),
                    painter = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_search1),
                    contentDescription = "搜索账单",
                )
            }
        }
        PortCard(contentPadding = PaddingValues(0.dp)) {
            PortActionRow(
                title = bookName,
                subtitle = "当前账本 · 点击切换",
                icon = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_book3),
                onClick = onSelectBook,
            )
        }
        PortCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PortSecondaryButton(
                    text = "$monthLabel ▾",
                    onClick = onSelectMonth,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    modifier = Modifier.size(PortDesign.TouchTarget),
                    onClick = onToggleAmounts,
                ) {
                    Icon(
                        modifier = Modifier.size(PortDesign.IconSize),
                        painter = painterResource(
                            if (amountsVisible) com.xiaojinzi.tally.lib.res.R.drawable.res_eye1
                            else com.xiaojinzi.tally.lib.res.R.drawable.res_eye1_closed,
                        ),
                        contentDescription = if (amountsVisible) "隐藏概览与预算金额" else "显示概览与预算金额",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                modifier = Modifier.padding(top = PortDesign.TinyGap, bottom = PortDesign.SmallGap),
                text = "月度支出",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val spendingText = if (amountsVisible) "¥ ${formatBillAmount(spendingFen)}" else "••••"
            Text(
                text = spendingText,
                style = when {
                    spendingText.length > 20 -> MaterialTheme.typography.titleLarge
                    spendingText.length > 15 -> MaterialTheme.typography.headlineMedium
                    else -> MaterialTheme.typography.headlineLarge
                },
                color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = PortDesign.CardPadding),
                thickness = 0.5.dp,
                color = scheme.onSurfaceVariantActions.copy(alpha = 0.1f),
            )
            BoxWithConstraints {
                if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f) {
                    Column(verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap)) {
                        BillAmount("月度收入", incomeFen, amountsVisible)
                        BillAmount("月度结余", balanceFen, amountsVisible)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(PortDesign.ItemGap)) {
                        BillAmount("月度收入", incomeFen, amountsVisible, Modifier.weight(1f))
                        BillAmount("月度结余", balanceFen, amountsVisible, Modifier.weight(1f))
                    }
                }
            }
        }
        PortSectionTitle(title = "本月预算")
        BudgetSummaryCard(
            summary = budgetSummary,
            enabled = canOpenBudget,
            onClick = onOpenBudget,
        )
        PortPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "记一笔",
            icon = rememberVectorPainter(Icons.Rounded.Add),
            onClick = onAddBill,
        )
        PortSectionTitle(title = "快捷记录")
        PortCard(contentPadding = PaddingValues(0.dp)) {
            PortActionRow(
                title = "自动记账",
                subtitle = "管理识别方式与待确认账单",
                icon = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_robot1),
                onClick = onAutoRecord,
            )
            PortDivider()
            PortActionRow(
                title = "导入账单",
                subtitle = "把已有收支记录整理到当前账本",
                icon = rememberVectorPainter(Icons.Rounded.FileDownload),
                onClick = onImport,
            )
            PortDivider()
            PortActionRow(
                title = "账单日历",
                subtitle = "按日期回看，长按日期记一笔",
                icon = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_calendar1),
                onClick = onOpenCalendar,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PortSectionTitle(
                modifier = Modifier.weight(1f),
                title = "最近账单",
                subtitle = "所选月份的收支记录",
            )
            PortSecondaryButton(text = "查看全部", onClick = onSearch)
        }
    }
}

@Composable
private fun BillAmount(
    label: String,
    amountFen: Long?,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (visible) "¥ ${formatBillAmount(amountFen)}" else "••••",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BudgetSummaryCard(
    summary: BillBudgetSummaryVo,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PortCard(
        onClick = if (enabled) onClick else null,
        contentPadding = PaddingValues(
            horizontal = PortDesign.CardPadding,
            vertical = PortDesign.ItemGap,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PortDesign.TinyGap),
            ) {
                Text(
                    text = summary.amountText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = NexioTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary.detailText,
                    fontSize = 14.sp,
                    color = if (summary.isOverBudget) NexioTheme.colorScheme.error
                    else NexioTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Icon(
                modifier = Modifier.size(PortDesign.IconSize),
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "查看与设置月度预算",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PortCardExpand(visible = summary.progress != null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(top = PortDesign.ItemGap)
                    .fillMaxWidth()
                    .height(PortDesign.ProgressHeight)
                    .clip(PortDesign.SmallShape),
                progress = { summary.progress ?: 0f },
                color = if (summary.isOverBudget) NexioTheme.colorScheme.error
                else NexioTheme.colorScheme.primary,
                trackColor = NexioTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.08f),
            )
        }
        PortCardExpand(visible = summary.categoryStatus != null) {
            PortStatusPill(
                modifier = Modifier.padding(top = PortDesign.SmallGap),
                text = summary.categoryStatus.orEmpty(),
                isError = true,
            )
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BillViewWrap() {
    PortTheme { BillView() }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BillViewPreview() {
    PortTheme { BillView(needInit = false) }
}
