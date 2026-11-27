package com.xiaojinzi.tally.module.core.module.bill_search.view

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.commonTimeFormat1
import com.xiaojinzi.support.ktx.commonTimeFormat3
import com.xiaojinzi.support.ktx.launchIgnoreError
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_LARGE
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppBackgroundColor
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.lib.res.ui.startShape
import com.xiaojinzi.tally.module.base.module.common_bill_list.view.CommonBillListView
import com.xiaojinzi.tally.module.base.support.DevelopHelper
import com.xiaojinzi.tally.module.core.module.bill_search.domain.BillSearchIntent
import kotlinx.coroutines.InternalCoroutinesApi

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BillSearchConditionView(
    vm: BillSearchViewModel,
    drawerState: DrawerState,
    hideKeyboardAndClearFocusAction: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bookInfoList by vm.bookInfoListStateOb.collectAsState(initial = emptyList())
    val categoryInfoList by vm.categoryInfoListStateOb.collectAsState(initial = emptyList())
    val accountInfoList by vm.accountInfoListStateOb.collectAsState(initial = emptyList())
    val labelInfoList by vm.labelInfoListStateOb.collectAsState(initial = emptyList())
    val hasImage by vm.hasImageStateOb.collectAsState(initial = false)
    Column(
        modifier = Modifier
            .fillMaxWidth(
                fraction = 0.8f,
            )
            .fillMaxHeight()
            .background(
                color = MaterialTheme.colorScheme.surface,
            )
            .clickableNoRipple {
                hideKeyboardAndClearFocusAction.invoke()
            }
            .statusBarsPadding()
            .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
            .nothing(),
    ) {
        Spacer(
            modifier = Modifier
                .height(height = 24.dp)
                .nothing()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = true)
                .verticalScroll(
                    state = rememberScrollState(),
                )
                .nothing(),
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val yearSelect by vm.yearSelectStateOb.collectAsState(initial = null)
                Text(
                    text = "日期",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                AppWidthSpace()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickableNoRipple {
                            vm.addIntent(
                                intent = BillSearchIntent.YearSelect(
                                    context = context,
                                )
                            )
                        }
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${yearSelect?.toString().orEmpty()}年",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        textAlign = TextAlign.Start,
                    )
                    Icon(
                        modifier = Modifier
                            .size(size = 24.dp)
                            .nothing(),
                        painter = rememberVectorPainter(image = Icons.Default.ArrowDropDown),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.8f,
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_SMALL.dp, vertical = APP_PADDING_LARGE.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val timeStart by vm.timeStartStateOb.collectAsState(initial = null)
                val timeEnd by vm.timeEndStateOb.collectAsState(initial = null)
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .wrapContentHeight()
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small,
                        )
                        .clickableNoRipple {
                            vm.addIntent(
                                intent = BillSearchIntent.TimeStartSelect(
                                    context = context,
                                )
                            )
                        }
                        .padding(horizontal = APP_PADDING_SMALL.dp, vertical = APP_PADDING_SMALL.dp)
                        .nothing(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (timeStart == null) {
                        Text(
                            modifier = Modifier
                                .nothing(),
                            text = "起始日期",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                    Text(
                        modifier = Modifier
                            .nothing(),
                        text = if (DevelopHelper.isDevelop) {
                            timeStart?.commonTimeFormat1().orEmpty()
                        } else {
                            timeStart?.commonTimeFormat3().orEmpty()
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = APP_PADDING_SMALL.dp, vertical = 0.dp)
                        .nothing(),
                    text = "-",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.outline,
                    ),
                    textAlign = TextAlign.Start,
                )
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .wrapContentHeight()
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small,
                        )
                        .clickableNoRipple {
                            vm.addIntent(
                                intent = BillSearchIntent.TimeEndSelect(
                                    context = context,
                                )
                            )
                        }
                        .padding(horizontal = APP_PADDING_SMALL.dp, vertical = APP_PADDING_SMALL.dp)
                        .nothing(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (timeEnd == null) {
                        Text(
                            modifier = Modifier
                                .nothing(),
                            text = "截止日期",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                    Text(
                        modifier = Modifier
                            .nothing(),
                        text = if (DevelopHelper.isDevelop) {
                            timeEnd?.commonTimeFormat1().orEmpty()
                        } else {
                            timeEnd?.commonTimeFormat3().orEmpty()
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Text(
                text = "金额",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Start,
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_SMALL.dp, vertical = APP_PADDING_LARGE.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val amountMin by vm.amountMinStateOb.collectAsState(initial = "")
                val amountMax by vm.amountMaxStateOb.collectAsState(initial = "")
                val focusRequesterForMinAmount = remember { FocusRequester() }
                val focusRequesterForMaxAmount = remember { FocusRequester() }
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .wrapContentHeight()
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small,
                        )
                        .clickableNoRipple {
                            focusRequesterForMinAmount.requestFocus()
                        }
                        .padding(horizontal = APP_PADDING_SMALL.dp, vertical = APP_PADDING_SMALL.dp)
                        .nothing(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (amountMin.isEmpty()) {
                        Text(
                            modifier = Modifier
                                .nothing(),
                            text = "最低金额",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                    BasicTextField(
                        modifier = Modifier
                            .focusRequester(
                                focusRequester = focusRequesterForMinAmount,
                            )
                            .width(intrinsicSize = IntrinsicSize.Min)
                            .widthIn(min = 2.dp)
                            .wrapContentHeight()
                            .nothing(),
                        value = amountMin,
                        onValueChange = {
                            vm.amountMinStateOb.value = it.filter { symbol ->
                                symbol.isDigit()
                            }.take(n = 9)
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                scope.launchIgnoreError {
                                    hideKeyboardAndClearFocusAction.invoke()
                                }
                            },
                        ),
                        cursorBrush = SolidColor(
                            value = MaterialTheme.colorScheme.primary,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
                Text(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = APP_PADDING_SMALL.dp, vertical = 0.dp)
                        .nothing(),
                    text = "-",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.outline,
                    ),
                    textAlign = TextAlign.Start,
                )
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .wrapContentHeight()
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small,
                        )
                        .clickableNoRipple {
                            focusRequesterForMaxAmount.requestFocus()
                        }
                        .padding(horizontal = APP_PADDING_SMALL.dp, vertical = APP_PADDING_SMALL.dp)
                        .nothing(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (amountMax.isEmpty()) {
                        Text(
                            modifier = Modifier
                                .nothing(),
                            text = "最高金额",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                    BasicTextField(
                        modifier = Modifier
                            .focusRequester(
                                focusRequester = focusRequesterForMaxAmount,
                            )
                            .width(intrinsicSize = IntrinsicSize.Min)
                            .widthIn(min = 2.dp)
                            .wrapContentHeight()
                            .nothing(),
                        value = amountMax,
                        onValueChange = {
                            vm.amountMaxStateOb.value = it.filter { symbol ->
                                symbol.isDigit()
                            }.take(n = 9)
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                scope.launchIgnoreError {
                                    hideKeyboardAndClearFocusAction.invoke()
                                }
                            },
                        ),
                        cursorBrush = SolidColor(
                            value = MaterialTheme.colorScheme.primary,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "账本",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                AppWidthSpace()
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .clickableNoRipple {
                            hideKeyboardAndClearFocusAction.invoke()
                            vm.addIntent(
                                intent = BillSearchIntent.BookSelect(
                                    context = context,
                                )
                            )
                        }
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(size = 16.dp)
                            .nothing(),
                        painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_add1),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(
                        modifier = Modifier
                            .width(width = 4.dp)
                            .nothing()
                    )
                    Text(
                        text = "添加账本",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outline,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            if (bookInfoList.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = APP_PADDING_SMALL.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        space = APP_PADDING_SMALL.dp,
                    ),
                ) {
                    bookInfoList.forEach { bookItem ->
                        Text(
                            modifier = Modifier
                                .wrapContentSize()
                                .clip(
                                    shape = MaterialTheme.shapes.small,
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                )
                                .clickable {
                                    vm.addIntent(
                                        intent = BillSearchIntent.BookDelete(
                                            id = bookItem.id,
                                        )
                                    )
                                }
                                .padding(
                                    horizontal = APP_PADDING_SMALL.dp,
                                    vertical = APP_PADDING_SMALL.dp
                                )
                                .nothing(),
                            text = bookItem.name.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            } else {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .wrapContentSize()
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                elevation = 1.dp,
                            )
                        )
                        .padding(
                            horizontal = APP_PADDING_SMALL.dp,
                            vertical = APP_PADDING_SMALL.dp
                        )
                        .nothing(),
                    text = "暂无选择",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
            }
            // 类别是账本下面的, 所以只有选择了一个账本的时候, 才可以选择类别
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "类别",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                AppWidthSpace()
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .clickableNoRipple {
                            hideKeyboardAndClearFocusAction.invoke()
                            vm.addIntent(
                                intent = BillSearchIntent.CategorySelect(
                                    context = context,
                                )
                            )
                        }
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(size = 16.dp)
                            .nothing(),
                        painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_add1),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(
                        modifier = Modifier
                            .width(width = 4.dp)
                            .nothing()
                    )
                    Text(
                        text = "添加类别",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outline,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            if (categoryInfoList.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = APP_PADDING_SMALL.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        space = APP_PADDING_SMALL.dp,
                    ),
                ) {
                    categoryInfoList.forEach { categoryItem ->
                        Text(
                            modifier = Modifier
                                .wrapContentSize()
                                .clip(
                                    shape = MaterialTheme.shapes.small,
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                )
                                .clickable {
                                    vm.addIntent(
                                        intent = BillSearchIntent.CategoryDelete(
                                            id = categoryItem.id,
                                        )
                                    )
                                }
                                .padding(
                                    horizontal = APP_PADDING_SMALL.dp,
                                    vertical = APP_PADDING_SMALL.dp
                                )
                                .nothing(),
                            text = categoryItem.name.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            } else {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .wrapContentSize()
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                elevation = 1.dp,
                            )
                        )
                        .padding(
                            horizontal = APP_PADDING_SMALL.dp,
                            vertical = APP_PADDING_SMALL.dp
                        )
                        .nothing(),
                    text = "暂无选择",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
            }
            // 账户也是账本下面的, 所以只有选择了一个账本的时候, 才可以选择账户
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "账户",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                AppWidthSpace()
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .clickableNoRipple {
                            hideKeyboardAndClearFocusAction.invoke()
                            vm.addIntent(
                                intent = BillSearchIntent.AccountSelect(
                                    context = context,
                                )
                            )
                        }
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(size = 16.dp)
                            .nothing(),
                        painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_add1),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(
                        modifier = Modifier
                            .width(width = 4.dp)
                            .nothing()
                    )
                    Text(
                        text = "添加账户",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outline,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            if (accountInfoList.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = APP_PADDING_SMALL.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        space = APP_PADDING_SMALL.dp,
                    ),
                ) {
                    accountInfoList.forEach { accountItem ->
                        Text(
                            modifier = Modifier
                                .wrapContentSize()
                                .clip(
                                    shape = MaterialTheme.shapes.small,
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                )
                                .clickable {
                                    vm.addIntent(
                                        intent = BillSearchIntent.AccountDelete(
                                            id = accountItem.id,
                                        )
                                    )
                                }
                                .padding(
                                    horizontal = APP_PADDING_SMALL.dp,
                                    vertical = APP_PADDING_SMALL.dp
                                )
                                .nothing(),
                            text = accountItem.name.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            } else {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .wrapContentSize()
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                elevation = 1.dp,
                            )
                        )
                        .padding(
                            horizontal = APP_PADDING_SMALL.dp,
                            vertical = APP_PADDING_SMALL.dp
                        )
                        .nothing(),
                    text = "暂无选择",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
            }
            // 标签也是账本下面的, 所以只有选择了一个账本的时候, 才可以选择标签
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "标签",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                AppWidthSpace()
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .clickableNoRipple {
                            hideKeyboardAndClearFocusAction.invoke()
                            vm.addIntent(
                                intent = BillSearchIntent.LabelSelect(
                                    context = context,
                                )
                            )
                        }
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(size = 16.dp)
                            .nothing(),
                        painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_add1),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(
                        modifier = Modifier
                            .width(width = 4.dp)
                            .nothing()
                    )
                    Text(
                        text = "添加标签",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outline,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            if (labelInfoList.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = APP_PADDING_SMALL.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        space = APP_PADDING_SMALL.dp,
                    ),
                ) {
                    labelInfoList.forEach { labelItem ->
                        Text(
                            modifier = Modifier
                                .wrapContentSize()
                                .clip(
                                    shape = MaterialTheme.shapes.small,
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                )
                                .clickable {
                                    vm.addIntent(
                                        intent = BillSearchIntent.LabelDelete(
                                            id = labelItem.id,
                                        )
                                    )
                                }
                                .padding(
                                    horizontal = APP_PADDING_SMALL.dp,
                                    vertical = APP_PADDING_SMALL.dp
                                )
                                .nothing(),
                            text = labelItem.name.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            } else {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                        .wrapContentSize()
                        .clip(
                            shape = MaterialTheme.shapes.small,
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                elevation = 1.dp,
                            )
                        )
                        .padding(
                            horizontal = APP_PADDING_SMALL.dp,
                            vertical = APP_PADDING_SMALL.dp
                        )
                        .nothing(),
                    text = "暂无选择",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
            }
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "是否有图片",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                AppWidthSpace()
                Checkbox(
                    modifier = Modifier
                        .scale(scale = 0.8f)
                        .nothing(),
                    checked = hasImage,
                    onCheckedChange = {
                        vm.hasImageStateOb.value = it
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .navigationBarsPadding()
                .nothing(),
        ) {
            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_SMALL.dp, vertical = 0.dp)
                    .weight(weight = 1f, fill = true)
                    .wrapContentHeight()
                    .nothing(),
                onClick = {
                    scope.launchIgnoreError {
                        hideKeyboardAndClearFocusAction.invoke()
                        vm.addIntent(
                            intent = BillSearchIntent.ResetSearch,
                        )
                    }
                },
            ) {
                Text(
                    text = "重置",
                    textAlign = TextAlign.Start,
                )
            }
            Button(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_SMALL.dp, vertical = 0.dp)
                    .weight(weight = 1f, fill = true)
                    .wrapContentHeight()
                    .nothing(),
                onClick = {
                    scope.launchIgnoreError {
                        hideKeyboardAndClearFocusAction.invoke()
                        drawerState.close()
                        vm.addIntent(
                            intent = BillSearchIntent.DoSearch(
                                context = context,
                            ),
                        )
                    }
                },
            ) {
                Text(
                    text = "确认",
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
private fun BillSearchView(
    needInit: Boolean? = false,
    hideKeyboardAndClearFocusAction: () -> Unit = {},
) {
    val context = LocalContext.current
    BusinessContentView<BillSearchViewModel>(
        needInit = needInit,
    ) { vm ->
        val yearSelect by vm.yearSelectStateOb.collectAsState(initial = null)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = AppBackgroundColor,
                )
                .clickableNoRipple {
                    hideKeyboardAndClearFocusAction.invoke()
                }
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .align(
                        alignment = Alignment.End,
                    )
                    .padding(horizontal = 0.dp, vertical = APP_PADDING_SMALL.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .clip(
                        shape = CircleShape.startShape(),
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
                    .clickableNoRipple {
                        vm.addIntent(
                            intent = BillSearchIntent.YearSelect(
                                context = context,
                                isSearchAfterSelect = true,
                            )
                        )
                    }
                    .padding(horizontal = (2 * APP_PADDING_SMALL).dp, vertical = 4.dp)
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "${yearSelect?.toString().orEmpty()}年",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                Icon(
                    modifier = Modifier
                        .size(size = 24.dp)
                        .nothing(),
                    painter = rememberVectorPainter(image = Icons.Default.ArrowDropDown),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.8f,
                    ),
                )
            }
            CommonBillListView(
                modifier = Modifier
                    .fillMaxSize()
                    .nothing(),
                commonBillListViewUseCase = vm.commonBillListViewUseCase,
                showBookInfo = true,
                noMoreText = "最多只能查询一年时间的账单".toStringItemDto(),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun BillSearchViewWrap() {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hideKeyboardAndClearFocusAction = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vm: BillSearchViewModel = viewModel()
    val searchKey by vm.searchKeyStateOb.collectAsState(initial = TextFieldValue(text = ""))
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed,
        confirmStateChange = {
            hideKeyboardAndClearFocusAction.invoke()
            true
        },
    )
    BackHandler {
        if (drawerState.isOpen) {
            scope.launchIgnoreError {
                drawerState.close()
            }
        } else {
            context.tryFinishActivity()
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            BillSearchConditionView(
                vm = vm,
                drawerState = drawerState,
            ) {
                hideKeyboardAndClearFocusAction.invoke()
            }
        },
    ) {
        Scaffold(
            topBar = {
                ConstraintLayout(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                ) {
                    val (
                        iconBack, editText, searchButton,
                    ) = createRefs()
                    IconButton(
                        modifier = Modifier
                            .constrainAs(ref = iconBack) {
                                this.start.linkTo(anchor = parent.start)
                                this.centerVerticallyTo(other = parent)
                            }
                            .nothing(),
                        onClick = {
                            context.tryFinishActivity()
                        },
                    ) {
                        Icon(
                            painter = rememberVectorPainter(image = Icons.Filled.ArrowBack),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    val focusRequesterForSearchKey = remember { FocusRequester() }
                    Box(
                        modifier = Modifier
                            .constrainAs(ref = editText) {
                                this.width = Dimension.fillToConstraints
                                this.height = Dimension.wrapContent
                                this.start.linkTo(
                                    anchor = iconBack.end,
                                    margin = 0.dp,
                                )
                                this.end.linkTo(
                                    anchor = searchButton.start,
                                    margin = 0.dp
                                )
                                this.top.linkTo(anchor = parent.top, margin = 0.dp)
                                this.bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                            }
                            .circleClip()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                    elevation = 1.dp,
                                ),
                            )
                            .clickableNoRipple {
                                focusRequesterForSearchKey.requestFocus()
                            }
                            .padding(
                                horizontal = APP_PADDING_NORMAL.dp,
                                vertical = APP_PADDING_SMALL.dp,
                            )
                            .nothing(),
                    ) {
                        BasicTextField(
                            modifier = Modifier
                                .focusRequester(
                                    focusRequester = focusRequesterForSearchKey,
                                )
                                .fillMaxWidth()
                                .nothing(),
                            value = searchKey,
                            onValueChange = {
                                vm.searchKeyStateOb.value = it.copy(
                                    text = it.text.trim(),
                                )
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search,
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    hideKeyboardAndClearFocusAction.invoke()
                                    vm.addIntent(
                                        intent = BillSearchIntent.DoSearch(
                                            context = context,
                                        ),
                                    )
                                },
                            ),
                            cursorBrush = SolidColor(
                                value = MaterialTheme.colorScheme.primary,
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        if (searchKey.text.isEmpty()) {
                            Text(
                                text = "搜索备注、账户、标签、金额等等",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                ),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    TextButton(
                        modifier = Modifier
                            .constrainAs(ref = searchButton) {
                                this.end.linkTo(anchor = parent.end, margin = APP_PADDING_NORMAL.dp)
                                this.centerVerticallyTo(other = parent)
                            }
                            .wrapContentSize()
                            .nothing(),
                        onClick = {
                            hideKeyboardAndClearFocusAction.invoke()
                            vm.addIntent(
                                intent = BillSearchIntent.DoSearch(
                                    context = context,
                                ),
                            )
                        },
                    ) {
                        Text(
                            text = "搜索",
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        hideKeyboardAndClearFocusAction.invoke()
                        scope.launchIgnoreError {
                            drawerState.open()
                        }
                    }
                ) {
                    Icon(
                        modifier = Modifier
                            .size(size = 20.dp)
                            .nothing(),
                        painter = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_filter1),
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
                BillSearchView() {
                    hideKeyboardAndClearFocusAction.invoke()
                }
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
private fun BillSearchViewPreview() {
    BillSearchView(
        needInit = false,
    )
}