package com.xiaojinzi.tally.module.core.module.category_info.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.GridView
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.launchIgnoreError
import com.xiaojinzi.support.ktx.notSupportError
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.support.ktx.tryToggle
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppBackgroundColor
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.core.module.category_info.domain.CategoryInfoIntent
import kotlinx.coroutines.InternalCoroutinesApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryInfoPageView(
    modifier: Modifier = Modifier,
    vm: CategoryInfoViewModel,
    isSort: Boolean,
    pageIndex: Int,
    dataList: List<Pair<TallyCategoryDto, List<TallyCategoryDto>>>,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier,
    ) {
        itemsIndexed(
            items = dataList,
            key = { _, item ->
                item.first.id
            },
        ) { index, item ->
            Column(
                modifier = Modifier
                    .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
                    .nothing(),
            ) {
                var isExpanded by remember {
                    mutableStateOf(
                        value = false,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .combinedClickable(
                            onLongClick = {
                                vm.isSortStateOb.tryToggle()
                            },
                        ) {
                            isExpanded = !isExpanded
                            /*AppRouterCoreApi::class
                                .routeApi()
                                .toCategorySubInfoView(
                                    context = context,
                                    parentId = item.first.id,
                                )*/
                        }
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            isExpanded = !isExpanded
                        },
                    ) {
                        Icon(
                            modifier = Modifier
                                .rotate(
                                    degrees = if (isExpanded) 90f else 0f,
                                )
                                .size(size = 16.dp)
                                .nothing(),
                            painter = rememberVectorPainter(image = Icons.Filled.PlayArrow),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        modifier = Modifier
                            .size(size = 20.dp)
                            .nothing(),
                        painter = AppServices.iconMappingSpi[item.first.iconName]?.let {
                            painterResource(
                                id = it,
                            )
                        } ?: ColorPainter(
                            color = Color.Transparent,
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(
                        modifier = Modifier
                            .width(width = 4.dp)
                            .nothing()
                    )
                    Text(
                        text = item.first.name.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        textAlign = TextAlign.Start,
                    )
                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                    if (isSort) {
                        if (index == 0) {
                            IconButton(
                                onClick = {
                                },
                            ) {
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    vm.addIntent(
                                        intent = CategoryInfoIntent.ChangeSort(
                                            pageIndex = pageIndex,
                                            cateId = item.first.id,
                                            isUp = true,
                                        )
                                    )
                                },
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(size = 16.dp)
                                        .nothing(),
                                    painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_arrow_up1),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        if (index == dataList.lastIndex) {
                            IconButton(
                                onClick = {
                                },
                            ) {
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    vm.addIntent(
                                        intent = CategoryInfoIntent.ChangeSort(
                                            pageIndex = pageIndex,
                                            cateId = item.first.id,
                                            isUp = false,
                                        )
                                    )
                                },
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .rotate(degrees = 180f)
                                        .size(size = 16.dp)
                                        .nothing(),
                                    painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_arrow_up1),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        AppWidthSpace()
                    } else {
                        IconButton(
                            onClick = {
                                AppRouterCoreApi::class
                                    .routeApi()
                                    .toCategorySubInfoView(
                                        context = context,
                                        parentId = item.first.id,
                                    )
                            },
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(size = 16.dp)
                                    .nothing(),
                                painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_edit1),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                if (isExpanded) {
                    GridView(
                        modifier = Modifier
                            .padding(
                                horizontal = APP_PADDING_NORMAL.dp,
                                vertical = APP_PADDING_SMALL.dp
                            )
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .nothing(),
                        items = item.second,
                        columnNumber = 5,
                        verticalSpace = APP_PADDING_NORMAL.dp,
                        lastItemContent = {
                            IconButton(onClick = {
                                AppRouterCoreApi::class
                                    .routeApi()
                                    .toCategoryCrudView(
                                        context = context,
                                        parentId = item.first.id,
                                    )
                            }) {
                                Icon(
                                    modifier = Modifier
                                        .size(size = 24.dp)
                                        .nothing(),
                                    painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_add1),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    ) { item1 ->
                        Column(
                            modifier = Modifier
                                .wrapContentSize()
                                .clickableNoRipple {
                                    AppRouterCoreApi::class
                                        .routeApi()
                                        .toCategoryCrudView(
                                            context = context,
                                            id = item1.id,
                                        )
                                }
                                .nothing(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(size = 12.dp)
                                    .nothing(),
                                painter = AppServices.iconMappingSpi[item1.iconName]?.let {
                                    painterResource(
                                        id = it,
                                    )
                                } ?: ColorPainter(
                                    color = Color.Transparent,
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(height = 4.dp)
                                    .nothing()
                            )
                            Text(
                                text = item1.name.orEmpty(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                textAlign = TextAlign.Start,
                            )
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
private fun CategoryInfoView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BusinessContentView<CategoryInfoViewModel>(
        needInit = needInit,
    ) { vm ->
        val isSort by vm.isSortStateOb.collectAsState(initial = false)
        val spendingCategoryList by vm.spendingCategoryListStateOb.collectAsState(initial = emptyList())
        val incomeCategoryList by vm.incomeCategoryListStateOb.collectAsState(initial = emptyList())
        val pageState = rememberPagerState {
            2
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = AppBackgroundColor,
                )
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .statusBarsPadding()
                    .padding(horizontal = 0.dp, vertical = 5.dp)
                    .nothing(),
            ) {
                IconButton(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterStart)
                        .nothing(),
                    onClick = {
                        context.tryFinishActivity()
                    },
                ) {
                    Icon(
                        modifier = Modifier
                            .nothing(),
                        painter = rememberVectorPainter(image = Icons.Filled.ArrowBack),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Row(
                    modifier = Modifier
                        .align(alignment = Alignment.Center)
                        .wrapContentSize()
                        .nothing(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    (0..1).forEach { index ->
                        val isSelected = index == pageState.currentPage
                        Text(
                            modifier = Modifier
                                .wrapContentSize()
                                .clickableNoRipple {
                                    scope.launchIgnoreError {
                                        pageState.animateScrollToPage(
                                            page = index,
                                        )
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .nothing(),
                            text = when (index) {
                                0 -> "支出"
                                1 -> "收入"
                                else -> notSupportError()
                            },
                            style = if (isSelected) {
                                MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            } else {
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            },
                            textAlign = TextAlign.Start,
                        )
                    }
                }
                if (isSort) {
                    IconButton(
                        modifier = Modifier
                            .align(alignment = Alignment.CenterEnd)
                            .nothing(),
                        onClick = {
                            vm.isSortStateOb.tryToggle()
                        },
                    ) {
                        Icon(
                            modifier = Modifier
                                .nothing(),
                            painter = rememberVectorPainter(image = Icons.Rounded.Check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true)
                    .nothing(),
                state = pageState,
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> CategoryInfoPageView(
                        modifier = Modifier
                            .fillMaxSize()
                            .nothing(),
                        vm = vm,
                        isSort = isSort,
                        pageIndex = pageIndex,
                        dataList = spendingCategoryList,
                    )

                    1 -> CategoryInfoPageView(
                        modifier = Modifier
                            .fillMaxSize()
                            .nothing(),
                        vm = vm,
                        isSort = isSort,
                        pageIndex = pageIndex,
                        dataList = incomeCategoryList,
                    )
                }
            }

            Button(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = APP_PADDING_NORMAL.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                onClick = {
                    AppRouterCoreApi::class
                        .routeApi()
                        .toCategoryCrudView(
                            context = context,
                            categoryType = when (pageState.currentPage) {
                                0 -> TallyCategoryDto.Companion.TallyCategoryType.SPENDING
                                1 -> TallyCategoryDto.Companion.TallyCategoryType.INCOME
                                else -> notSupportError()
                            }
                        )
                },
            ) {
                Text(
                    text = "添加一级类别",
                    textAlign = TextAlign.Start,
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
fun CategoryInfoViewWrap() {
    CategoryInfoView()
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun CategoryInfoViewPreview() {
    CategoryInfoView(
        needInit = false,
    )
}