package com.xiaojinzi.tally.module.core.module.label_info.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.R
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppBackgroundColor
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.view.compose.AppCommonEmptyDataView
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import com.xiaojinzi.tally.module.core.module.label_info.domain.LabelDetailItemUseCaseDto
import com.xiaojinzi.tally.module.core.module.label_info.domain.LabelInfoIntent
import kotlinx.coroutines.InternalCoroutinesApi

@Composable
private fun LabelInfoItemView(
    modifier: Modifier = Modifier,
    vm: LabelInfoViewModel,
    item: LabelDetailItemUseCaseDto?,
) {
    val context = LocalContext.current
    ConstraintLayout(
        modifier = modifier,
    ) {
        val (
            icon, name,
            amount, billCount,
            menuMore,
        ) = createRefs()

        Icon(
            modifier = Modifier
                .constrainAs(ref = icon) {
                    this.start.linkTo(anchor = parent.start, margin = APP_PADDING_NORMAL.dp)
                    this.top.linkTo(anchor = parent.top, margin = 0.dp)
                    this.bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                }
                .size(size = 18.dp)
                .nothing(),
            painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_label1),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = name) {
                    this.start.linkTo(anchor = icon.end, margin = APP_PADDING_NORMAL.dp)
                    this.top.linkTo(anchor = parent.top, margin = 0.dp)
                }
                .nothing(),
            text = item?.labelName.orEmpty(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Start,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = amount) {
                    this.start.linkTo(anchor = name.start, margin = 0.dp)
                    this.top.linkTo(anchor = name.bottom, margin = 0.dp)
                    this.bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                }
                .nothing(),
            text = item?.amount?.value?.format2f().orEmpty(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Start,
        )

        Text(
            modifier = Modifier
                .constrainAs(ref = billCount) {
                    this.start.linkTo(anchor = amount.end, margin = 2.dp)
                    this.centerVerticallyTo(other = amount)
                }
                .nothing(),
            text = "(共${item?.billCount ?: 0} 笔账单)",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Start,
        )

        IconButton(
            modifier = Modifier
                .constrainAs(ref = menuMore) {
                    this.end.linkTo(anchor = parent.end, margin = 0.dp)
                    this.centerVerticallyTo(other = parent)
                }
                .nothing(),
            onClick = {
                item?.let {
                    vm.addIntent(
                        intent = LabelInfoIntent.MenuMore(
                            context = context,
                            labelId = it.labelId,
                            labelName = it.labelName,
                            billIdList = it.billIdList,
                        )
                    )
                }
            },
        ) {
            Icon(
                modifier = Modifier
                    .size(size = 24.dp)
                    .nothing(),
                painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_more3),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun LabelInfoView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    BusinessContentView<LabelInfoViewModel>(
        needInit = needInit,
    ) { vm ->
        val labelDetailList by vm.labelDetailListStateOb.collectAsState(initial = emptyList())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = AppBackgroundColor,
                )
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (labelDetailList.isEmpty()) {
                AppCommonEmptyDataView(
                    modifier = Modifier
                        .fillMaxSize()
                        .nothing(),
                )
            } else {
                LazyColumn {
                    labelDetailList.forEach { item ->
                        item {
                            LabelInfoItemView(
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
                                    .clickable {
                                        AppRouterCoreApi::class
                                            .routeApi()
                                            .toLabelCrudView(
                                                context = context,
                                                labelId = item.labelId,
                                            )
                                    }
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                    )
                                    .padding(
                                        horizontal = APP_PADDING_NORMAL.dp,
                                        vertical = APP_PADDING_SMALL.dp
                                    )
                                    .nothing(),
                                vm = vm,
                                item = item,
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
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun LabelInfoViewWrap() {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = "标签管理".toStringItemDto(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    AppRouterCoreApi::class
                        .routeApi()
                        .toLabelCrudView(
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
            LabelInfoView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun LabelInfoViewPreview() {
    LabelInfoView(
        needInit = false,
    )
}