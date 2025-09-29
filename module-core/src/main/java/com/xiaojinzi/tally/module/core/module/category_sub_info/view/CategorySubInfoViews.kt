package com.xiaojinzi.tally.module.core.module.category_sub_info.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.BottomView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.support.ktx.tryToggle
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppBackgroundColor
import com.xiaojinzi.tally.lib.res.ui.AppDivider
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.lib.res.ui.topShape
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.view.compose.AppCommonEmptyDataView
import com.xiaojinzi.tally.module.core.module.category_sub_info.domain.CategorySubInfoIntent
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun CategorySubInfoView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    BusinessContentView<CategorySubInfoViewModel>(
        modifier = Modifier
            .animateContentSize()
            .fillMaxSize()
            .clip(
                shape = MaterialTheme.shapes.medium.topShape(),
            )
            .background(
                color = AppBackgroundColor,
            )
            .nothing(),
        needInit = needInit,
    ) { vm ->
        val parentCategory by vm.parentCategoryInfoStateOb.collectAsState(initial = null)
        val isSort by vm.isSortStateOb.collectAsState(initial = false)
        if (parentCategory == null) {
            AppCommonEmptyDataView(
                text = "一级类别已删除".toStringItemDto(),
            )
        } else {
            val subCategoryList by vm.subCategoryListStateOb.collectAsState(initial = emptyList())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nothing(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(
                    modifier = Modifier
                        .height(height = 24.dp)
                        .nothing()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .circleClip()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .size(size = 24.dp)
                            .nothing(),
                        painter = AppServices.iconMappingSpi[parentCategory?.iconName]?.let {
                            painterResource(
                                id = it,
                            )
                        } ?: ColorPainter(
                            color = Color.Transparent,
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    AppWidthSpace()
                    Column {
                        Text(
                            text = parentCategory?.name.orEmpty(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            textAlign = TextAlign.Start,
                        )
                        parentCategory?.userInfoCache?.let { userCacheInfo ->
                            Spacer(
                                modifier = Modifier
                                    .height(height = 4.dp)
                                    .nothing()
                            )
                            Row(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .nothing(),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(size = 12.dp)
                                        .nothing(),
                                    painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_people1),
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
                                    text = userCacheInfo.name.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.5f,
                                        ),
                                    ),
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                    if (isSort) {
                        IconButton(
                            modifier = Modifier
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
                    } else {
                        if (parentCategory?.canDelete == true) {
                            IconButton(
                                onClick = {
                                    parentCategory?.let { parentCategory1 ->
                                        vm.addIntent(
                                            intent = CategorySubInfoIntent.ItemDelete(
                                                categoryId = parentCategory1.id,
                                            )
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(size = 18.dp)
                                        .nothing(),
                                    painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_delete2),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                parentCategory?.let { parentCategory1 ->
                                    AppRouterCoreApi::class
                                        .routeApi()
                                        .toCategoryCrudView(
                                            context = context,
                                            id = parentCategory1.id,
                                        )
                                }
                            }
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(size = 18.dp)
                                    .nothing(),
                                painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_edit1),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                }

                AppDivider(
                    modifier = Modifier
                        .padding(
                            horizontal = APP_PADDING_NORMAL.dp,
                            vertical = APP_PADDING_NORMAL.dp
                        )
                        .fillMaxWidth()
                        .height(height = 0.5.dp)
                        .nothing(),
                )

                if (subCategoryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(weight = 1f, fill = true)
                            .nothing(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppCommonEmptyDataView(
                            text = "暂无二级类别".toStringItemDto(),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(weight = 1f, fill = true)
                            .nothing(),
                    ) {
                        itemsIndexed(
                            items = subCategoryList,
                            key = { _, item ->
                                item.id
                            },
                        ) { index, item ->
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .background(
                                        color = AppBackgroundColor,
                                    )
                                    .combinedClickable(
                                        onLongClick = {
                                            vm.isSortStateOb.tryToggle()
                                        },
                                    ) {

                                    }
                                    .padding(
                                        horizontal = APP_PADDING_NORMAL.dp,
                                        vertical = 0.dp
                                    )
                                    .nothing(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .circleClip()
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        )
                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                        .size(size = 20.dp)
                                        .nothing(),
                                    painter = AppServices.iconMappingSpi[item.iconName]?.let {
                                        painterResource(
                                            id = it,
                                        )
                                    } ?: ColorPainter(
                                        color = Color.Transparent,
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                AppWidthSpace()
                                Column {
                                    Text(
                                        text = item.name.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        textAlign = TextAlign.Start,
                                    )
                                    item.userInfoCache?.let { userCacheInfo ->
                                        Spacer(
                                            modifier = Modifier
                                                .height(height = 4.dp)
                                                .nothing()
                                        )
                                        Row(
                                            modifier = Modifier
                                                .wrapContentSize()
                                                .nothing(),
                                            verticalAlignment = Alignment.Bottom,
                                        ) {
                                            Icon(
                                                modifier = Modifier
                                                    .size(size = 12.dp)
                                                    .nothing(),
                                                painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_people1),
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
                                                text = userCacheInfo.name.orEmpty(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.5f,
                                                    ),
                                                ),
                                                textAlign = TextAlign.Start,
                                            )
                                        }
                                    }
                                }
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
                                                    intent = CategorySubInfoIntent.ChangeSort(
                                                        cateId = item.id,
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
                                    if (index == subCategoryList.lastIndex) {
                                        IconButton(
                                            onClick = {
                                            },
                                        ) {
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                vm.addIntent(
                                                    intent = CategorySubInfoIntent.ChangeSort(
                                                        cateId = item.id,
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
                                    if (item.canDelete) {
                                        IconButton(
                                            onClick = {
                                                vm.addIntent(
                                                    intent = CategorySubInfoIntent.ItemDelete(
                                                        categoryId = item.id,
                                                    )
                                                )
                                            },
                                        ) {
                                            Icon(
                                                modifier = Modifier
                                                    .size(size = 15.dp)
                                                    .nothing(),
                                                painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_delete2),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            AppRouterCoreApi::class
                                                .routeApi()
                                                .toCategoryCrudView(
                                                    context = context,
                                                    id = item.id,
                                                )
                                        }
                                    ) {
                                        Icon(
                                            modifier = Modifier
                                                .size(size = 15.dp)
                                                .nothing(),
                                            painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_edit1),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = APP_PADDING_NORMAL.dp)
                        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    onClick = {
                        parentCategory?.let {
                            AppRouterCoreApi::class
                                .routeApi()
                                .toCategoryCrudView(
                                    context = context,
                                    parentId = it.id,
                                )
                        }
                    },
                ) {
                    Text(
                        text = "新建二级分类",
                        textAlign = TextAlign.Start,
                    )
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
fun CategorySubInfoViewWrap() {
    BottomView(
        maxFraction = 0.6f,
    ) {
        CategorySubInfoView()
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun CategorySubInfoViewPreview() {
    CategorySubInfoView(
        needInit = false,
    )
}