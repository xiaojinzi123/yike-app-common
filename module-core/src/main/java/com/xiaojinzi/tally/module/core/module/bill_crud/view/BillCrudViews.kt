package com.xiaojinzi.tally.module.core.module.bill_crud.view

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Visibility
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.GridView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.compose.util.collectStateFlowAsState
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.launchIgnoreError
import com.xiaojinzi.support.ktx.notSupportError
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.shake
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.support.ktx.tryToggle
import com.xiaojinzi.tally.lib.res.R
import com.xiaojinzi.tally.lib.res.model.support.rememberPainter
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.lib.res.ui.NoInteractionSource
import com.xiaojinzi.tally.module.base.ktx.LayoutCustomWidthMode
import com.xiaojinzi.tally.module.base.ktx.layoutCustom
import com.xiaojinzi.tally.module.base.ktx.syncWith
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.CostEmptyState
import com.xiaojinzi.tally.module.base.theme.PortCardExpand
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.core.module.bill_crud.domain.BillCrudIntent
import com.xiaojinzi.tally.module.core.module.bill_crud.domain.BillCrudTab
import com.xiaojinzi.tally.module.core.module.category_select.domain.CategorySelectIntent
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillCrudKeyboardItemDecorateView(
    modifier: Modifier = Modifier,
    enableClick: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .then(other = modifier)
            .padding(all = 3.dp)
            .heightIn(min = PortDesign.TouchTarget)
            .clip(shape = CircleShape)
            .combinedClickable(
                enabled = enableClick,
                onLongClick = onLongClick,
            ) {
                onClick?.invoke()
            }
            .background(
                color = containerColor,
            )
            .nothing(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun BillCrudKeyboardTextOrIconItemView(
    modifier: Modifier = Modifier,
    text: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
    ),
    @DrawableRes
    iconRsd: Int? = null,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    enableClick: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    BillCrudKeyboardItemDecorateView(
        modifier = modifier,
        enableClick = enableClick,
        containerColor = containerColor,
        onLongClick = onLongClick,
        onClick = onClick,
    ) {
        text?.let { targetText ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .nothing(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = 16.dp)
                        .nothing(),
                    text = targetText,
                    style = textStyle,
                    textAlign = TextAlign.Center,
                )
            }
        }
        iconRsd?.let { targetIconRsd ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .nothing(),
                contentAlignment = Alignment.Center,
            ) {
                val sizeDp = with(receiver = LocalDensity.current) {
                    MaterialTheme.typography.bodyMedium.fontSize.toDp() + 4.dp
                }
                Icon(
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .size(size = sizeDp)
                        .nothing(),
                    painter = painterResource(id = targetIconRsd),
                    contentDescription = null,
                    tint = iconColor,
                )
            }
        }
    }
}

@Composable
private fun BillCrudKeyboardCompleteOrUpdateItemView(
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val vm: BillCrudViewModel = viewModel()
    val isEdit by vm.isEditStateOb.collectAsState(initial = false)
    val costState by vm.costUseCase.costStrStateOb.collectAsState(initial = CostEmptyState())
    val costIsCorrect by vm.costUseCase.costIsCorrectFormatStateOb.collectAsState(initial = false)
    val canClick = (costState is CostEmptyState) || costIsCorrect
    BillCrudKeyboardTextOrIconItemView(
        modifier = modifier,
        text = if (isEdit) "更新" else "完成",
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (canClick) 1f else 0.38f),
        ),
        containerColor = MaterialTheme.colorScheme.primary,
        enableClick = canClick,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

/**
 * 编辑的时候才会出现
 */
@Composable
private fun BillCrudKeyboardDeleteItemView(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    BillCrudKeyboardTextOrIconItemView(
        modifier = modifier,
        text = "删除",
        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        ),
        onClick = onClick,
    )
}

@Composable
private fun BillCrudKeyboardNumberItemView(
    modifier: Modifier = Modifier,
    number: Int,
    onClick: (() -> Unit)? = null
) {
    BillCrudKeyboardTextOrIconItemView(
        modifier = modifier,
        text = number.toString(),
        onClick = onClick
    )
}

@Composable
private fun BillCrudBigCategoryItemView(
    modifier: Modifier = Modifier,
    item: BillCrudCategoryVo?,
    isSelected: Boolean,
    isShowSubIcon: Boolean = false,
) {
    ConstraintLayout(
        modifier = modifier,
    ) {

        val (
            icon, name, iconMore,
        ) = createRefs()

        Icon(
            modifier = Modifier
                .constrainAs(ref = icon) {
                    this.top.linkTo(anchor = parent.top, margin = 0.dp)
                    this.start.linkTo(anchor = parent.start, margin = 0.dp)
                    this.end.linkTo(anchor = parent.end, margin = 0.dp)
                }
                .circleClip()
                .background(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceColorAtElevation(
                            elevation = 1.dp,
                        )
                    },
                )
                .padding(
                    horizontal = 6.dp,
                    vertical = 6.dp
                )
                .size(size = 24.dp)
                .nothing(),
            painter = if (item == null) {
                painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_forbid1)
            } else {
                item.icon?.rememberPainter()
                    ?: ColorPainter(color = Color.Transparent)
            },
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )

        Icon(
            modifier = Modifier
                .constrainAs(ref = iconMore) {
                    this.bottom.linkTo(anchor = icon.bottom, margin = 0.dp)
                    this.end.linkTo(anchor = icon.end, margin = 0.dp)
                    this.visibility =
                        if (isShowSubIcon) {
                            Visibility.Visible
                        } else {
                            Visibility.Gone
                        }
                }
                .circleClip()
                .background(
                    color = MaterialTheme.colorScheme.tertiary,
                )
                .size(size = 10.dp)
                .nothing(),
            painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_more3),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiary,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = name) {
                    this.top.linkTo(
                        anchor = icon.bottom,
                        margin = APP_PADDING_SMALL.dp
                    )
                    this.start.linkTo(anchor = parent.start, margin = 0.dp)
                    this.end.linkTo(anchor = parent.end, margin = 0.dp)
                }
                .wrapContentSize()
                .nothing(),
            text = if (item == null) {
                "无类别"
            } else {
                item.name?.contentWithComposable() ?: ""
            },
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ),
            textAlign = TextAlign.Start,
        )

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillCrudCoreView(
    vm: BillCrudViewModel,
    tabList: List<BillCrudTab>,
) {
    val scope = rememberCoroutineScope()
    val horizontalPadding = if (LocalConfiguration.current.screenWidthDp >= 600) 20.dp else PortDesign.PagePadding
    val context = LocalContext.current
    val isVibrate by AppServices.appConfigSpi.isVibrateStateOb.collectAsState(initial = true)
    val currentSelectTab by vm.tabStateOb.collectAsState(initial = null)
    val bookInfo by vm.bookInfoStateOb.collectAsState(initial = null)
    val isEdit by vm.isEditStateOb.collectAsState(initial = false)
    val refundBillDetail by vm.refundBillDetailStateOb.collectAsState(initial = null)
    val isForRefund by vm.isForRefundState.collectAsState(initial = false)
    val canSwitchBook by vm.canSwitchBookState.collectAsState(initial = false)
    val costState by vm.costUseCase.costStrStateOb.collectAsState(
        initial = CostEmptyState(),
    )
    val parsedCost by vm.costUseCase.parsedCostStateOb.collectAsState(
        initial = null,
    )
    val categorySpendingMap by vm.categorySelectViewModel.categorySpendingMapStateObVo.collectAsState(
        initial = emptyMap(),
    )
    val categoryIncomeMap by vm.categorySelectViewModel.categoryIncomeMapStateObVo.collectAsState(
        initial = emptyMap(),
    )
    val categorySpendingGroupList by vm.categorySelectViewModel.spendingCategoryGroupListStateObVo.collectAsState(
        initial = emptyList(),
    )
    val categoryIncomeGroupList by vm.categorySelectViewModel.incomeCategoryGroupListStateObVo.collectAsState(
        initial = emptyList(),
    )
    val categorySpendingGroupHasSublistMap by vm.categorySelectViewModel.spendingCategoryGroupHasSublistMapStateVo.collectAsState(
        initial = emptyMap(),
    )
    val categoryIncomeGroupHasSublistMap by vm.categorySelectViewModel.incomeCategoryGroupHasSublistMapStateVo.collectAsState(
        initial = emptyMap(),
    )
    val categoryGroupSelected by vm.categorySelectViewModel.categoryGroupSelectedStateObVo.collectAsState(
        initial = null,
    )
    val categoryItemSelected by vm.categorySelectViewModel.categoryItemSelectedStateObVo.collectAsState(
        initial = null
    )
    val labelList by vm.labelInfoListStateOb.collectAsState(initial = emptyList())
    val timeStampStr by vm.timeStampStrState.collectAsState(initial = null)
    val account by vm.accountStateOb.collectAsState(initial = null)
    val transferAccount by vm.transferAccountStateOb.collectAsState(initial = null)
    val transferTargetAccount by vm.transferTargetAccountStateOb.collectAsState(initial = null)
    val imageUrlList by vm.imageUrlListState.collectAsState(initial = emptyList())
    val isNotCalculate by vm.isNotCalculateState.collectAsState(initial = false)
    val isShowNotCalculateView by vm.isShowNotCalculateViewState.collectAsState(initial = false)

    val subSpendingCategoryList = categorySpendingMap[categoryGroupSelected]
    val subIncomeCategoryList = categoryIncomeMap[categoryGroupSelected]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nothing(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        if (isForRefund) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .statusBarsPadding()
                    .padding(
                        horizontal = horizontalPadding,
                        vertical = APP_PADDING_SMALL.dp
                    )
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    modifier = Modifier
                        .nothing(),
                    onClick = {
                        context.tryFinishActivity()
                    },
                ) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .wrapContentHeight()
                        .nothing(),
                    text = "(退款) ${
                        refundBillDetail?.run {
                            this.category?.name.orEmpty()
                        } ?: categoryItemSelected?.run {
                            this.name?.contentWithComposable()
                        } ?: categoryGroupSelected?.name?.contentWithComposable().orEmpty()
                    }",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    modifier = Modifier
                        .nothing(),
                    onClick = {
                    },
                    enabled = !isEdit,
                ) {
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true)
                    .nothing(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = parsedCost?.run {
                        when (this) {
                            in 0f..Float.MAX_VALUE -> "${
                                if (isEdit) {
                                    "修改为"
                                } else {
                                    "您将"
                                }
                            }获取退款"

                            else -> "${
                                if (isEdit) {
                                    "修改为"
                                } else {
                                    "您将"
                                }
                            }支付退款"
                        }
                    } ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Start,
                )
                Text(
                    text = parsedCost?.absoluteValue?.format2f().orEmpty(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    textAlign = TextAlign.Start,
                )
            }

        } else // 占位
        {

            val categoryPageState = rememberPagerState {
                tabList.size
            }
            categoryPageState.syncWith(
                targetFlow = vm.tabStateOb,
                toIndex = { tabType ->
                    when (tabType) {
                        BillCrudTab.Spending -> 0
                        BillCrudTab.Income -> 1
                        BillCrudTab.Transfer -> 2
                    }
                },
                toValue = { index ->
                    when (index) {
                        0 -> BillCrudTab.Spending
                        1 -> BillCrudTab.Income
                        2 -> BillCrudTab.Transfer
                        else -> notSupportError()
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .statusBarsPadding()
                    .padding(
                        horizontal = horizontalPadding,
                        vertical = APP_PADDING_NORMAL.dp
                    )
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    modifier = Modifier
                        .nothing(),
                    onClick = {
                        context.tryFinishActivity()
                    },
                ) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                TabRow(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .wrapContentHeight()
                        .nothing(),
                    selectedTabIndex = categoryPageState.currentPage,
                    containerColor = Color.Transparent,
                    indicator = {},
                    divider = {
                    },
                ) {
                    tabList.forEach { item ->
                        val isSelected = currentSelectTab == item
                        Tab(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent,
                                ),
                            selected = isSelected,
                            interactionSource = NoInteractionSource,
                            onClick = {
                                scope.launchIgnoreError {
                                    categoryPageState.animateScrollToPage(
                                        page = tabList.indexOf(element = item).coerceIn(
                                            minimumValue = 0,
                                            maximumValue = tabList.size - 1,
                                        ),
                                    )
                                }
                            },
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .nothing(),
                                text = when (item) {
                                    BillCrudTab.Spending -> "支出"
                                    BillCrudTab.Income -> "收入"
                                    BillCrudTab.Transfer -> "转账"
                                    else -> notSupportError()
                                },
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                ),
                            )
                        }
                    }
                }
                if (canSwitchBook) {
                    PortSecondaryButton(
                        modifier = Modifier.padding(start = 8.dp),
                        text = bookInfo?.name.orEmpty(),
                        onClick = {
                            vm.addIntent(
                                intent = BillCrudIntent.BookSwitch(
                                    context = context,
                                )
                            )
                        },
                        enabled = !isEdit,
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true)
                    .nothing(),
                state = categoryPageState,
                beyondViewportPageCount = 1,
                verticalAlignment = Alignment.Top,
            ) { pageIndex ->
                when (pageIndex) {
                    0, 1 -> {
                        val targetCategoryGroupList = when (pageIndex) {
                            0 -> categorySpendingGroupList
                            1 -> categoryIncomeGroupList
                            else -> notSupportError()
                        }
                        val targetSubCategoryGroupList = when (pageIndex) {
                            0 -> subSpendingCategoryList
                            1 -> subIncomeCategoryList
                            else -> notSupportError()
                        }
                        val targetCategoryGroupHasSublistMap = when (pageIndex) {
                            0 -> categorySpendingGroupHasSublistMap
                            1 -> categoryIncomeGroupHasSublistMap
                            else -> notSupportError()
                        }
                        BillCrudCategorySelectView(
                            modifier = Modifier
                                .padding(horizontal = horizontalPadding, vertical = 0.dp)
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .verticalScroll(state = rememberScrollState())
                                .clip(PortDesign.CardShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = PortDesign.SectionGap),
                            targetCategoryGroupList = targetCategoryGroupList,
                            categoryGroupSelected = categoryGroupSelected,
                            targetSubCategoryGroupList = targetSubCategoryGroupList,
                            categoryItemSelected = categoryItemSelected,
                            categoryType = when (pageIndex) {
                                0 -> TallyCategoryDto.Companion.TallyCategoryType.SPENDING
                                1 -> TallyCategoryDto.Companion.TallyCategoryType.INCOME
                                else -> null
                            },
                            targetCategoryGroupHasSublistMap = targetCategoryGroupHasSublistMap,
                            itemNullClick = {
                                vm.categorySelectUseCase.addIntent(
                                    intent = CategorySelectIntent.CategorySetNull,
                                )
                            },
                            itemGroupClick = { categoryId ->
                                vm.categorySelectUseCase.addIntent(
                                    intent = CategorySelectIntent.CategoryGroupSelect(
                                        id = categoryId,
                                    )
                                )
                            },
                            itemClick = { categoryId ->
                                vm.categorySelectUseCase.addIntent(
                                    intent = CategorySelectIntent.CategoryItemSelect(
                                        id = categoryId,
                                    )
                                )
                            },
                        )
                    }

                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .nothing(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(
                                        horizontal = horizontalPadding,
                                        vertical = 0.dp
                                    )
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clip(shape = PortDesign.CardShape)
                                    .border(
                                        width = 0.5.dp,
                                        shape = PortDesign.CardShape,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    .clickable {
                                        vm.addIntent(
                                            intent = BillCrudIntent.TransferAccountSelect(
                                                context = context,
                                            )
                                        )
                                    }
                                    .padding(
                                        horizontal = horizontalPadding,
                                        vertical = APP_PADDING_NORMAL.dp
                                    )
                                    .nothing(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(size = 24.dp)
                                        .nothing(),
                                    painter = painterResource(
                                        id = AppServices.iconMappingSpi[transferAccount?.iconName]
                                            ?: R.drawable.res_bank_card_no1,
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(
                                    modifier = Modifier
                                        .width(width = 16.dp)
                                        .nothing()
                                )
                                Spacer(
                                    modifier = Modifier
                                        .width(0.5.dp)
                                        .height(height = 20.dp)
                                        .circleClip()
                                        .background(
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                        .nothing()
                                )
                                Spacer(
                                    modifier = Modifier
                                        .width(width = 16.dp)
                                        .nothing()
                                )
                                if (transferAccount == null) {
                                    Text(
                                        text = "转出账户",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.outline,
                                        ),
                                        textAlign = TextAlign.Start,
                                    )
                                } else {
                                    Text(
                                        text = transferAccount?.name.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        textAlign = TextAlign.Start,
                                    )
                                    Spacer(
                                        modifier = Modifier.weight(
                                            weight = 1f,
                                            fill = true
                                        )
                                    )
                                    Text(
                                        text = parsedCost?.let { cost ->
                                            if (cost >= 0) {
                                                "-${cost.format2f()}"
                                            } else {
                                                "+${cost.absoluteValue.format2f()}"
                                            }
                                        }.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.secondary,
                                        ),
                                        textAlign = TextAlign.Start,
                                    )
                                }
                            }

                            Icon(
                                modifier = Modifier
                                    .rotate(
                                        degrees = 90f,
                                    )
                                    .circleClip()
                                    .clickable {
                                        vm.addIntent(
                                            intent = BillCrudIntent.TransferAccountSwitch,
                                        )
                                    }
                                    .padding(horizontal = 24.dp, vertical = 24.dp)
                                    .size(size = 24.dp)
                                    .nothing(),
                                painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_switch1),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )

                            Row(
                                modifier = Modifier
                                    .padding(
                                        horizontal = horizontalPadding,
                                        vertical = 0.dp
                                    )
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clip(shape = PortDesign.CardShape)
                                    .border(
                                        width = 0.5.dp,
                                        shape = PortDesign.CardShape,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    .clickable {
                                        vm.addIntent(
                                            intent = BillCrudIntent.TransferTargetAccountSelect(
                                                context = context,
                                            )
                                        )
                                    }
                                    .padding(
                                        horizontal = horizontalPadding,
                                        vertical = APP_PADDING_NORMAL.dp
                                    )
                                    .nothing(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(size = 24.dp)
                                        .nothing(),
                                    painter = painterResource(
                                        id = AppServices.iconMappingSpi[transferTargetAccount?.iconName]
                                            ?: R.drawable.res_bank_card_no1,
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(
                                    modifier = Modifier
                                        .width(width = 16.dp)
                                        .nothing()
                                )
                                Spacer(
                                    modifier = Modifier
                                        .width(0.5.dp)
                                        .height(height = 20.dp)
                                        .circleClip()
                                        .background(
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                        .nothing()
                                )
                                Spacer(
                                    modifier = Modifier
                                        .width(width = 16.dp)
                                        .nothing()
                                )
                                if (transferTargetAccount == null) {
                                    Text(
                                        text = "转入账户",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.outline,
                                        ),
                                        textAlign = TextAlign.Start,
                                    )
                                } else {
                                    Text(
                                        text = transferTargetAccount?.name.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        textAlign = TextAlign.Start,
                                    )
                                    Spacer(
                                        modifier = Modifier.weight(
                                            weight = 1f,
                                            fill = true
                                        )
                                    )
                                    Text(
                                        text = parsedCost?.let { cost ->
                                            if (cost < 0) {
                                                "-${cost.absoluteValue.format2f()}"
                                            } else {
                                                "+${cost.format2f()}"
                                            }
                                        }.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.secondary,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    horizontal = horizontalPadding,
                    vertical = APP_PADDING_SMALL.dp
                )
                .horizontalScroll(
                    state = rememberScrollState(),
                )
                .nothing(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labelList.forEachIndexed { _, item ->
                Text(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 0.dp)
                        .wrapContentSize()
                        .circleClip()
                        .combinedClickable(
                            onLongClick = {
                                vm.addIntent(
                                    intent = BillCrudIntent.LabelDelete(
                                        labelId = item.id,
                                    )
                                )
                            },
                        ) {
                            // empty
                        }
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                        .nothing(),
                    text = item.name.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSecondary,
                    ),
                    textAlign = TextAlign.Start,
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 1.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        elevation = 1.dp,
                    )
                )
                .nothing()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = horizontalPadding, vertical = 0.dp)
                .nothing(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .clickableNoRipple {
                        vm.addIntent(
                            intent = BillCrudIntent.LabelSelect(
                                context = context,
                            )
                        )
                    }
                    .padding(horizontal = 5.dp, vertical = 4.dp)
                    .size(size = 18.dp)
                    .nothing(),
                painter = painterResource(id = R.drawable.res_label2),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            val note by vm.noteStateOb.collectAsState(initial = "")
            BasicTextField(
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .padding(horizontal = APP_PADDING_SMALL.dp, vertical = 0.dp)
                    .wrapContentHeight()
                    .nothing(),
                value = note,
                cursorBrush = SolidColor(
                    value = MaterialTheme.colorScheme.primary,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                onValueChange = {
                    vm.noteStateOb.value = it
                },
            ) {
                if (note.isEmpty()) {
                    Text(
                        text = "添加备注",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.5f,
                            )
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
                it.invoke()
            }
            Text(
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .wrapContentHeight()
                    .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                    .nothing(),
                text = costState.strValue.ifEmpty {
                    "0.00"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.error,
                ),
                textAlign = TextAlign.End,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .nothing(),
        ) {
            // 为了让备注的那个输入框能自适应
            Spacer(
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
                    .nothing()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 1.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                elevation = 1.dp,
                            )
                        )
                        .nothing()
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(state = rememberScrollState())
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = horizontalPadding, vertical = 0.dp)
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .clickableNoRipple {
                                vm.addIntent(
                                    intent = BillCrudIntent
                                        .DateTimeSelect(
                                            context = context,
                                        )
                                )
                            }
                            .padding(
                                horizontal = 0.dp,
                                vertical = APP_PADDING_NORMAL.dp
                            )
                            .nothing(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 4.dp,
                        ),
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(size = 16.dp)
                                .nothing(),
                            painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_calendar1),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = timeStampStr ?: "当前时间",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }

                    AnimatedVisibility(
                        visible = currentSelectTab != BillCrudTab.Transfer,
                    ) {
                        Row(
                            modifier = Modifier
                                .wrapContentSize()
                                .clickableNoRipple {
                                    vm.addIntent(
                                        intent = BillCrudIntent
                                            .AccountSelect(
                                                context = context,
                                            )
                                    )
                                }
                                .padding(
                                    horizontal = 0.dp,
                                    vertical = APP_PADDING_NORMAL.dp
                                )
                                .nothing(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 4.dp,
                            ),
                        ) {
                            AppWidthSpace()
                            Icon(
                                modifier = Modifier
                                    .size(size = 16.dp)
                                    .nothing(),
                                painter = painterResource(
                                    id = AppServices.iconMappingSpi[account?.iconName]
                                        ?: R.drawable.res_bank_card_no1,
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = account?.name.orNull() ?: "无账户",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }

                    AppWidthSpace()

                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .clickableNoRipple {
                                vm.addIntent(
                                    intent = BillCrudIntent.ImageSelect(
                                        context = context,
                                    )
                                )
                            }
                            .padding(
                                horizontal = 0.dp,
                                vertical = 0.dp
                            )
                            .nothing(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 4.dp,
                        ),
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(size = 16.dp)
                                .nothing(),
                            painter = painterResource(id = R.drawable.res_image1),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .nothing(),
                        ) {
                            Text(
                                text = "图片",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                textAlign = TextAlign.Start,
                            )
                            if (imageUrlList.isNotEmpty()) {
                                Text(
                                    modifier = Modifier
                                        .align(
                                            alignment = Alignment.TopEnd,
                                        )
                                        .offset(
                                            x = 6.dp,
                                            y = (-4).dp,
                                        )
                                        .circleClip()
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        .padding(all = 0.5.dp)
                                        // 自定义测量大小
                                        .layoutCustom(
                                            widthMode = LayoutCustomWidthMode.AtLastHeight,
                                        )
                                        .nothing(),
                                    text = "${imageUrlList.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize.times(
                                            other = 0.7f
                                        ),
                                        color = MaterialTheme.colorScheme.onSecondary,
                                    ),
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isShowNotCalculateView) {
                        // 不计入预算
                        Row(
                            modifier = Modifier
                                .offset(x = (-4).dp)
                                .wrapContentSize()
                                .clickableNoRipple {
                                    vm.isNotCalculateState.tryToggle()
                                }
                                .nothing(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .scale(0.8f)
                                    .nothing(),
                                checked = isNotCalculate,
                                onCheckedChange = {
                                    vm.isNotCalculateState.tryToggle()
                                },
                            )
                            Text(
                                modifier = Modifier
                                    .offset(x = (-8).dp)
                                    .wrapContentSize()
                                    .nothing(),
                                text = "不计入收支",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }

                }
                GridView(
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                elevation = 1.dp,
                            ),
                        )
                        .nothing(),
                    items = (0..15).toList(),
                    columnNumber = 4,
                ) { item ->
                    when (item) {
                        in 0..2 -> BillCrudKeyboardNumberItemView(number = item + 7) {
                            vm.costUseCase.appendNumber(value = item + 7)
                            if (isVibrate) {
                                shake()
                            }
                        }

                        in 4..6 -> BillCrudKeyboardNumberItemView(number = item) {
                            vm.costUseCase.appendNumber(value = item)
                            if (isVibrate) {
                                shake()
                            }
                        }

                        in 8..10 -> BillCrudKeyboardNumberItemView(number = item - 7) {
                            vm.costUseCase.appendNumber(value = item - 7)
                            if (isVibrate) {
                                shake()
                            }
                        }

                        3 -> BillCrudKeyboardTextOrIconItemView(
                            iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_delete1,
                        ) {
                            vm.costUseCase.costDeleteLast()
                            if (isVibrate) {
                                shake()
                            }
                        }

                        7 -> BillCrudKeyboardTextOrIconItemView(text = "+") {
                            vm.costUseCase.appendAddSymbol()
                            if (isVibrate) {
                                shake()
                            }
                        }

                        11 -> BillCrudKeyboardTextOrIconItemView(text = "-") {
                            vm.costUseCase.appendMinusSymbol()
                            if (isVibrate) {
                                shake()
                            }
                        }

                        12 -> BillCrudKeyboardNumberItemView(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .nothing(),
                            number = 0,
                        ) {
                            vm.costUseCase.appendNumber(value = 0)
                            if (isVibrate) {
                                shake()
                            }
                        }

                        13 -> BillCrudKeyboardTextOrIconItemView(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .nothing(),
                            text = ".",
                        ) {
                            vm.costUseCase.appendPoint()
                            if (isVibrate) {
                                shake()
                            }
                        }

                        14 -> if (isEdit) {
                            BillCrudKeyboardDeleteItemView(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .nothing(),
                            ) {
                                if (isVibrate) {
                                    shake()
                                }
                                vm.addIntent(
                                    intent = BillCrudIntent.Delete(
                                        context = context,
                                    )
                                )
                            }
                        } else {
                            if (!isForRefund) {
                                BillCrudKeyboardTextOrIconItemView(
                                    modifier = Modifier
                                        .navigationBarsPadding()
                                        .nothing(),
                                    text = "再记",
                                    onLongClick = {
                                        if (isVibrate) {
                                            shake()
                                        }
                                        vm.addIntent(
                                            intent = BillCrudIntent.SubmitAndNewOne(
                                                isForce = true,
                                            ),
                                        )
                                    },
                                ) {
                                    if (isVibrate) {
                                        shake()
                                    }
                                    vm.addIntent(
                                        intent = BillCrudIntent.SubmitAndNewOne(),
                                    )
                                }
                            }
                        }

                        15 -> BillCrudKeyboardCompleteOrUpdateItemView(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .nothing(),
                            onLongClick = {
                                if (isVibrate) {
                                    shake()
                                }
                                vm.addIntent(
                                    intent = BillCrudIntent.Submit(
                                        context = context,
                                        isForce = true,
                                    )
                                )
                            },
                        ) {
                            if (isVibrate) {
                                shake()
                            }
                            vm.addIntent(
                                intent = BillCrudIntent.Submit(
                                    context = context,
                                )
                            )
                        }

                        else -> {
                            error("Not support")
                        }
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalLayoutApi::class)
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BillCrudView(
    needInit: Boolean? = false,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    if (!WindowInsets.isImeVisible) {
        LaunchedEffect(Unit) {
            focusManager.clearFocus(force = true)
        }
    }
    BusinessContentView<BillCrudViewModel>(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.background,
            )
            .clickableNoRipple {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
            .nothing(),
        needInit = needInit,
    ) { vm ->
        val isShowNotSupportBillView by vm.isShowNotSupportBillViewOb.collectStateFlowAsState()
        if (isShowNotSupportBillView) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nothing(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "不支持的账单",
                    textAlign = TextAlign.Start,
                )
            }
        } else {
            val tabList by vm.tabListOb.collectAsState(initial = emptyList())
            AnimatedVisibility(
                visible = tabList.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                BillCrudCoreView(
                    vm = vm,
                    tabList = tabList,
                )
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun BillCrudCategorySelectView(
    modifier: Modifier = Modifier,
    targetCategoryGroupList: List<BillCrudCategoryVo>,
    categoryGroupSelected: BillCrudCategoryVo?,
    targetSubCategoryGroupList: List<BillCrudCategoryVo>?,
    categoryItemSelected: BillCrudCategoryVo?,
    categoryType: TallyCategoryDto.Companion.TallyCategoryType?,
    targetCategoryGroupHasSublistMap: Map<String, Boolean>,
    itemNullClick: () -> Unit = {},
    itemGroupClick: (categoryId: String) -> Unit = {},
    itemClick: (categoryId: String) -> Unit = {},
) {
    val context = LocalContext.current
    GridView(
        modifier = modifier,
        items = targetCategoryGroupList,
        afterRowContent = { rowIndex ->
            val selectIndex =
                targetCategoryGroupList.indexOfFirst { categoryGroupSelected?.id == it.id }
            val isShow =
                categoryGroupSelected != null && selectIndex in (((rowIndex + 0) * 5 - 1) until ((rowIndex + 1) * 5 - 1))
            PortCardExpand(visible = isShow) {
                GridView(
                    modifier = Modifier
                        .padding(
                            horizontal = APP_PADDING_SMALL.dp,
                            vertical = APP_PADDING_SMALL.dp,
                        )
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                elevation = 1.dp,
                            ),
                        )
                        .padding(
                            horizontal = APP_PADDING_SMALL.dp,
                            vertical = APP_PADDING_SMALL.dp,
                        )
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    items = targetSubCategoryGroupList ?: emptyList(),
                    columnNumber = 5,
                    verticalSpace = APP_PADDING_SMALL.dp,
                    lastItemContent = if (targetSubCategoryGroupList.isNullOrEmpty()) {
                        null
                    } else {
                        {
                            Column(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .clickableNoRipple {
                                        categoryGroupSelected?.let {
                                            AppRouterCoreApi::class
                                                .routeApi()
                                                .toCategoryCrudView(
                                                    context = context,
                                                    parentId = it.id,
                                                )
                                        }
                                    }
                                    .nothing(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(
                                            horizontal = 6.dp,
                                            vertical = 6.dp
                                        )
                                        .size(size = 16.dp)
                                        .nothing(),
                                    painter = painterResource(id = R.drawable.res_add1),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(
                                    modifier = Modifier
                                        .height(height = 2.dp)
                                        .nothing()
                                )
                                Text(
                                    text = "新增",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize.times(
                                            other = 0.7f
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                    },
                ) { item ->
                    val isSelected =
                        item.id == categoryItemSelected?.id
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .clickableNoRipple {
                                itemClick.invoke(
                                    item.id
                                )
                            }
                            .nothing(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            modifier = Modifier
                                .circleClip()
                                .run {
                                    if (isSelected) {
                                        this.background(color = MaterialTheme.colorScheme.secondary)
                                    } else {
                                        this
                                    }
                                }
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 6.dp
                                )
                                .size(size = 16.dp)
                                .nothing(),
                            painter = item.icon?.rememberPainter()
                                ?: painterResource(id = R.drawable.res_airpods1),
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Spacer(
                            modifier = Modifier
                                .height(height = 2.dp)
                                .nothing()
                        )
                        Text(
                            modifier = Modifier
                                .wrapContentSize()
                                .nothing(),
                            text = item.name?.contentWithComposable() ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = MaterialTheme.typography.bodySmall.fontSize.times(
                                    other = 0.7f
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        },
        columnNumber = 5,
        verticalSpace = APP_PADDING_SMALL.dp,
        firstItemContent = {
            BillCrudBigCategoryItemView(
                modifier = Modifier
                    .wrapContentSize()
                    .clickableNoRipple {
                        itemNullClick.invoke()
                    }
                    .nothing(),
                item = null,
                isSelected = categoryGroupSelected == null,
            )
        },
        lastItemContent = if (targetCategoryGroupList.isEmpty()) {
            null
        } else // 占位
        {
            {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .clickableNoRipple {
                            AppRouterCoreApi::class
                                .routeApi()
                                .toCategoryCrudView(
                                    context = context,
                                    categoryType = categoryType,
                                )
                        }
                        .nothing(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(size = 28.dp)
                            .nothing(),
                        painter = painterResource(id = R.drawable.res_add1),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(
                        modifier = Modifier
                            .height(height = 6.dp)
                            .nothing()
                    )
                    Text(
                        text = "新增",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
        },
    ) { item ->

        val isSelected = item.id == categoryGroupSelected?.id

        BillCrudBigCategoryItemView(
            modifier = Modifier
                .wrapContentSize()
                .combinedClickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onLongClick = {
                        AppRouterCoreApi::class
                            .routeApi()
                            .toCategorySubInfoView(
                                context = context,
                                parentId = item.id,
                            )
                    },
                ) {
                    itemGroupClick.invoke(
                        item.id,
                    )
                }
                .nothing(),
            item = item,
            isSelected = isSelected,
            isShowSubIcon = targetCategoryGroupHasSublistMap.getOrElse(
                key = item.id,
                defaultValue = { false },
            )
        )
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun BillCrudViewWrap() {
    PortTheme {
        BillCrudView()
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BillCrudViewPreview() {
    PortTheme {
        BillCrudView(
            needInit = false,
        )
    }
}