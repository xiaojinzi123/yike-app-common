package com.xiaojinzi.tally.module.base.module.common_bill_list.view

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.R
import com.xiaojinzi.tally.lib.res.model.support.rememberPainter
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_LARGE
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.bottomShape
import com.xiaojinzi.tally.lib.res.ui.rectShape
import com.xiaojinzi.tally.lib.res.ui.topShape
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillListIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val TAG = "CommonBillListViews"

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun CommonBillListHeaderItemView(
    modifier: Modifier = Modifier,
    headerItem: CommonBillListHeaderItemVo? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = headerItem
                ?.timeStr
                ?.contentWithComposable().orEmpty(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = contentColorFor(backgroundColor = MaterialTheme.colorScheme.background)
            ),
            textAlign = TextAlign.Start,
        )
        Spacer(
            modifier = Modifier
                .width(width = APP_PADDING_NORMAL.dp)
                .nothing()
        )
        Text(
            text = headerItem?.dayOfWeekStr?.contentWithComposable().orEmpty(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
        Text(
            text = "收",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )
        Spacer(
            modifier = Modifier
                .width(width = 4.dp)
                .nothing()
        )
        Text(
            text = (headerItem?.income?.value ?: 0f).format2f(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
            textAlign = TextAlign.Start,
        )
        Spacer(
            modifier = Modifier
                .width(width = 16.dp)
                .nothing()
        )
        Text(
            text = "支",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.Start,
        )
        Spacer(
            modifier = Modifier
                .width(width = 4.dp)
                .nothing()
        )
        Text(
            text = (headerItem?.spending?.value ?: 0f).format2f(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
            textAlign = TextAlign.Start,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun CommonBillListNormalItemView(
    modifier: Modifier = Modifier,
    normalItem: CommonBillListNormalItemVo? = null,
    showBookInfo: Boolean = false,
) {
    ConstraintLayout(
        modifier = modifier,
    ) {
        val (
            iconCategoryOrTransfer,
            startContainerView,
            endContainerView,
        ) = createRefs()

        Icon(
            modifier = Modifier
                .constrainAs(ref = iconCategoryOrTransfer) {
                    this.top.linkTo(anchor = parent.top, margin = 0.dp)
                    this.bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                    this.start.linkTo(anchor = parent.start, margin = 0.dp)
                }
                .size(size = 24.dp)
                .nothing(),
            painter = when (normalItem?.type) {

                TallyBillDto.Type.NORMAL -> {
                    normalItem
                        .categoryIcon
                        ?.rememberPainter()
                        ?: painterResource(id = R.drawable.res_help1)
                }

                TallyBillDto.Type.TRANSFER -> painterResource(id = R.drawable.res_transfer2)

                TallyBillDto.Type.REFUND -> {
                    normalItem
                        .categoryIcon
                        ?.rememberPainter()
                        ?: painterResource(id = R.drawable.res_help1)
                }

                else -> {
                    painterResource(id = R.drawable.res_help1)
                }

            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )

        Column(
            modifier = Modifier
                .constrainAs(ref = startContainerView) {
                    this.width = Dimension.fillToConstraints
                    this.height = Dimension.wrapContent
                    this.start.linkTo(
                        anchor = iconCategoryOrTransfer.end,
                        margin = APP_PADDING_NORMAL.dp,
                    )
                    this.end.linkTo(
                        anchor = endContainerView.start,
                        margin = APP_PADDING_NORMAL.dp,
                    )
                    this.top.linkTo(anchor = parent.top, margin = 0.dp)
                    this.bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                }
                .nothing(),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .nothing(),
                    text = when (normalItem?.type) {
                        TallyBillDto.Type.NORMAL,
                        TallyBillDto.Type.REFUND -> {
                            normalItem
                                .categoryName
                                ?.contentWithComposable()
                                .orNull() ?: "无类别"
                        }

                        TallyBillDto.Type.TRANSFER -> {
                            "转账"
                        }

                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                if (normalItem?.type == TallyBillDto.Type.REFUND) {
                    Text(
                        modifier = Modifier
                            .padding(start = APP_PADDING_SMALL.dp)
                            .nothing(),
                        text = "(退)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.error,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
                if (normalItem?.isNotCalculate == true) {
                    Text(
                        modifier = Modifier
                            .padding(start = APP_PADDING_SMALL.dp)
                            .nothing(),
                        text = "(不计入收支)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.error,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            normalItem?.userName?.let { userName ->
                Spacer(
                    modifier = Modifier
                        .height(height = 4.dp)
                        .nothing()
                )
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(size = 12.dp)
                            .nothing(),
                        painter = painterResource(id = R.drawable.res_people1),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.5f,
                        ),
                    )
                    Spacer(
                        modifier = Modifier
                            .width(width = 4.dp)
                            .nothing()
                    )
                    Text(
                        modifier = Modifier
                            .nothing(),
                        text = userName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.5f,
                            ),
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            normalItem?.labelNameList?.let { labelNameList ->
                if (labelNameList.isNotEmpty()) {
                    Spacer(
                        modifier = Modifier
                            .height(height = 4.dp)
                            .nothing()
                    )
                    FlowRow(
                        modifier = Modifier
                            .wrapContentSize()
                            .nothing(),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 4.dp,
                        ),
                    ) {
                        labelNameList.forEach { item ->
                            Text(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .circleClip()
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                    .nothing(),
                                text = item,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .constrainAs(ref = endContainerView) {
                    this.end.linkTo(anchor = parent.end, margin = 0.dp)
                    this.top.linkTo(anchor = parent.top, margin = 0.dp)
                    this.bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                }
                .wrapContentSize()
                .nothing(),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                modifier = Modifier
                    .nothing(),
                text = (normalItem?.amount ?: 0f).format2f(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.error.copy(
                        alpha = 0.5f,
                    )
                ),
                textAlign = TextAlign.Start,
            )
            if (showBookInfo) {
                normalItem?.bookName?.let { bookName ->
                    Spacer(
                        modifier = Modifier
                            .height(height = 4.dp)
                            .nothing()
                    )
                    Text(
                        modifier = Modifier
                            .nothing(),
                        text = "账本：${bookName.contentWithComposable()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.inversePrimary,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            val accountStr = when (normalItem?.type) {
                TallyBillDto.Type.NORMAL,
                TallyBillDto.Type.REFUND -> normalItem
                    .accountName
                    ?.contentWithComposable()
                    .orEmpty()

                TallyBillDto.Type.TRANSFER -> "${
                    normalItem
                        .accountName
                        ?.contentWithComposable() ?: "空"
                }→${
                    normalItem
                        .transferTargetAccountName
                        ?.contentWithComposable() ?: "空"
                }"

                else -> ""
            }
            if (accountStr.isNotEmpty()) {
                Spacer(
                    modifier = Modifier
                        .height(height = 4.dp)
                        .nothing()
                )
                Text(
                    modifier = Modifier
                        .nothing(),
                    text = accountStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.inversePrimary,
                    ),
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun CommonBillListView(
    modifier: Modifier = Modifier
        .fillMaxSize()
        .nothing(),
    headerContent: (@Composable () -> Unit)? = null,
    commonBillListViewUseCase: CommonBillListViewUseCase,
    showBookInfo: Boolean = false,
    noMoreText: StringItemDto = "没有更多了~".toStringItemDto(),
    logKey: String? = null,
) {
    val context = LocalContext.current
    val billList by commonBillListViewUseCase.billListStateObVo.collectAsState(initial = emptyList())
    Box(
        modifier = modifier
            .nothing(),
    ) {
        if (billList.isEmpty()) {
            VerticalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .nothing(),
                state = rememberPagerState {
                    1
                },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nothing(),
                ) {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.res_bill_empty1)
                    )
                    LottieAnimation(
                        modifier = Modifier
                            .align(alignment = Alignment.Center)
                            .size(size = 240.dp)
                            .nothing(),
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                    )
                }
            }
        } else {
            val lazyListState: LazyListState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .nothing(),
                state = lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                headerContent?.let {
                    item(
                        key = "headerContent",
                    ) {
                        headerContent()
                    }
                }
                itemsIndexed(
                    items = billList,
                ) { index, item ->
                    when (item) {
                        is CommonBillListHeaderItemVo -> {
                            CommonBillListHeaderItemView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(
                                        start = APP_PADDING_NORMAL.dp,
                                        end = APP_PADDING_NORMAL.dp,
                                        top = APP_PADDING_NORMAL.dp,
                                        bottom = APP_PADDING_SMALL.dp,
                                    )
                                    .nothing(),
                                headerItem = item,
                            )
                        }

                        is CommonBillListNormalItemVo -> {
                            CommonBillListNormalItemView(
                                modifier = Modifier
                                    .zIndex(6f)
                                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clip(
                                        shape = when (item.clipType) {
                                            CommonBillListItemClipType.TopAndBottom -> MaterialTheme.shapes.small
                                            CommonBillListItemClipType.Top -> MaterialTheme.shapes.small.topShape()
                                            CommonBillListItemClipType.Bottom -> MaterialTheme.shapes.small.bottomShape()
                                            CommonBillListItemClipType.None -> MaterialTheme.shapes.small.rectShape()
                                        },
                                    )
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                    )
                                    .combinedClickable(
                                        role = Role.Button,
                                        onLongClick = {
                                            commonBillListViewUseCase.commonBillListUseCase.addIntent(
                                                intent = CommonBillListIntent.PopMenu(
                                                    context = context,
                                                    billId = item.billId,
                                                )
                                            )
                                        },
                                        onClick = {
                                            commonBillListViewUseCase.commonBillListUseCase.addIntent(
                                                intent = CommonBillListIntent.ToBillDetail(
                                                    context = context,
                                                    billId = item.billId,
                                                )
                                            )
                                        },
                                    )
                                    .padding(
                                        horizontal = APP_PADDING_NORMAL.dp,
                                        vertical = APP_PADDING_NORMAL.dp
                                    )
                                    .nothing(),
                                normalItem = item,
                                showBookInfo = showBookInfo,
                            )
                        }
                    }
                }
                item(
                    key = "footerContent",
                ) {
                    Text(
                        modifier = Modifier
                            .padding(bottom = APP_PADDING_SMALL.dp)
                            .padding(horizontal = 0.dp, vertical = APP_PADDING_LARGE.dp)
                            .nothing(),
                        text = noMoreText.contentWithComposable(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            LaunchedEffect(key1 = lazyListState) {
                snapshotFlow { billList.size }
                    .filter { it > 0 }
                    .flatMapLatest {
                        snapshotFlow { lazyListState.canScrollForward }
                    }
                    .filter { !it }
                    .onEach {
                        LogSupport.d(
                            tag = TAG,
                            content = "logKey = $logKey, 调用了加载更多",
                        )
                        commonBillListViewUseCase.commonBillListUseCase.addIntent(
                            intent = CommonBillListIntent.LoadMore,
                        )
                    }
                    .launchIn(scope = this)
            }
        }
    }

}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun CommonBillListNormalItemViewPreview() {
    CommonBillListNormalItemView(
        modifier = Modifier
            .zIndex(6f)
            .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(
                shape = MaterialTheme.shapes.small.rectShape(),
            )
            .background(
                /*color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    elevation = 1.dp
                ),*/
                color = MaterialTheme.colorScheme.background,
            )
            .combinedClickable(
                role = Role.Button,
                onLongClick = {
                },
                onClick = {
                },
            )
            .padding(
                horizontal = APP_PADDING_NORMAL.dp,
                vertical = APP_PADDING_NORMAL.dp,
            )
            .nothing(),
        normalItem = CommonBillListNormalItemVo(
            billId = "xxx",
            type = TallyBillDto.Type.REFUND,
            userName = "用户1",
            clipType = CommonBillListItemClipType.TopAndBottom,
            bookName = "账本1".toStringItemDto(),
            // categoryIcon = R.drawable.res_alipay1.toLocalImageItemDto(),
            categoryIcon = null,
            // categoryName = "类别1".toStringItemDto(),
            categoryName = null,
            accountName = "支付宝".toStringItemDto(),
            transferTargetAccountName = "微信".toStringItemDto(),
            labelNameList = listOf("测试1", "测试2"),
            amount = 100f,
            isNotCalculate = true,
        ),
    )
}