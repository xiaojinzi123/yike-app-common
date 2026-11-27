package com.xiaojinzi.tally.module.main.module.main.view

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.nexioui.button.Button
import com.haooz.nexioui.button.ButtonDefaults
import com.haooz.nexioui.card.Card
import com.haooz.nexioui.theme.NexioTheme
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.finishAppAllTask
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.base.theme.appPageBackgroundColor
import com.xiaojinzi.tally.module.main.module.main.bill.view.BillView
import com.xiaojinzi.tally.module.main.module.main.calendar.view.CalendarViewWrap
import com.xiaojinzi.tally.module.main.module.main.domain.MainIntent
import com.xiaojinzi.tally.module.main.module.main.domain.MainTabDto
import com.xiaojinzi.tally.module.main.module.main.statistics.view.StatisticsViewWrap
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun MainView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    val isShowedGuide1 by AppServices.appConfigSpi.isShowedGuide1StateOb.collectAsState(initial = true)
    BusinessContentView<MainViewModel>(needInit = needInit) { vm ->
        val tabList by vm.tabListStateOb.collectAsState(initial = emptyList())
        val tabSelected by vm.tabSelectedStateOb.collectAsState(initial = MainTabDto.Bill)
        // 自动记账直接打开应用内专页；日历保留为账单的次级页面。
        val pageList = remember(tabList) {
            tabList.filter { it != MainTabDto.AutoRecord } + MainTabDto.Calendar
        }
        val pagerState = rememberPagerState { pageList.size }
        LaunchedEffect(tabSelected, pageList) {
            if (tabList.isNotEmpty()) {
                val selectedIndex = pageList.indexOf(tabSelected)
                if (selectedIndex >= 0) {
                    pagerState.scrollToPage(selectedIndex)
                } else {
                    vm.addIntent(MainIntent.TabChanged(MainTabDto.Bill))
                }
            }
        }
        BackHandler(enabled = tabSelected == MainTabDto.Calendar) {
            vm.addIntent(MainIntent.TabChanged(MainTabDto.Bill))
        }
        val horizontalPadding = if (LocalConfiguration.current.screenWidthDp >= 600) 20.dp else 16.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(appPageBackgroundColor()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    // 底部安全区由导航栏占用，内容页仅消费，避免重复留白。
                    .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            ) {
                if (tabList.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = pagerState,
                        key = { pageList[it].name },
                        beyondViewportPageCount = 1,
                        userScrollEnabled = false,
                    ) { pageIndex ->
                        when (pageList[pageIndex]) {
                            MainTabDto.Bill -> BillView(
                                onOpenCalendar = {
                                    vm.addIntent(MainIntent.TabChanged(MainTabDto.Calendar))
                                },
                            )
                            MainTabDto.Assets -> AppServices.coreSpi?.AssetsViewShared()
                            MainTabDto.Statistics -> StatisticsViewWrap()
                            MainTabDto.Tools -> ToolsView()
                            MainTabDto.Calendar -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = PortDesign.SmallGap),
                                        horizontalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconButton(
                                            modifier = Modifier.size(PortDesign.TouchTarget),
                                            onClick = {
                                                vm.addIntent(MainIntent.TabChanged(MainTabDto.Bill))
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = "返回账单",
                                            )
                                        }
                                        Text("账单日历", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                                    ) {
                                        CalendarViewWrap()
                                    }
                                }
                            }
                            MainTabDto.AutoRecord -> Unit
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 独立占位的实底 Nexio 胶囊，列表与底部手势区始终在它之外。
                Card(
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                    cornerRadius = 100.dp,
                    insideMargin = PaddingValues(6.dp),
                ) {
                    BoxWithConstraints {
                        val compact = maxWidth / tabList.size.coerceAtLeast(1) < 76.dp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            tabList.forEach { tab ->
                                val label = tab.nameStringItem.contentWithComposable()
                                val isSelected = tabSelected == tab ||
                                    (tab == MainTabDto.Bill && tabSelected == MainTabDto.Calendar)
                                val scheme = NexioTheme.colorScheme
                                val tint = if (isSelected) scheme.primary else scheme.onSurfaceVariantSummary
                                Button(
                                    modifier = Modifier.weight(1f).semantics { selected = isSelected },
                                    onClick = {
                                        if (tab == MainTabDto.AutoRecord) {
                                            Router.with(context)
                                                .hostAndPath(AppRouterConfig.QIANJI_AUTO_RECORD)
                                                .forward()
                                        } else {
                                            vm.addIntent(MainIntent.TabChanged(tab))
                                        }
                                    },
                                    cornerRadius = 100.dp,
                                    minWidth = 0.dp,
                                    minHeight = 52.dp,
                                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        color = if (isSelected) scheme.primary.copy(alpha = 0.1f)
                                        else scheme.surfaceContainer,
                                        contentColor = tint,
                                    ),
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(PortDesign.IconSize),
                                            painter = painterResource(tab.iconRsd),
                                            contentDescription = if (compact) label else null,
                                            tint = tint,
                                        )
                                        if (!compact) {
                                            Text(
                                                text = label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 13.sp,
                                                color = tint,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!isShowedGuide1) {
            val dismissGuide = { AppServices.appConfigSpi.switchShowedGuide1(b = true) }
            AlertDialog(
                onDismissRequest = dismissGuide,
                title = { Text("从这一笔开始") },
                text = {
                    Text(
                        "首页查看月度收支和预算，点击月份可切换账期。\n\n" +
                            "「记一笔」记录收支；自动记账、导入账单和日历都有独立入口。账本与资产账户在「工具」中管理，数据保存在本机。",
                    )
                },
                confirmButton = {
                    TextButton(
                        modifier = Modifier.heightIn(min = PortDesign.TouchTarget),
                        onClick = dismissGuide,
                    ) {
                        Text("开始使用")
                    }
                },
            )
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun MainViewWrap() {
    val context = LocalContext.current
    var lastPressBackTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressBackTime < 1500) {
            finishAppAllTask()
        } else {
            lastPressBackTime = currentTime
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }
    PortTheme { MainView() }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun MainViewPreview() {
    PortTheme { MainView(needInit = false) }
}
