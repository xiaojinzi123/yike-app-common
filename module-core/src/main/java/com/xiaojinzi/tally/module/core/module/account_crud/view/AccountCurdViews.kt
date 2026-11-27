package com.xiaojinzi.tally.module.core.module.account_crud.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.module.base.view.compose.AppCommonActionButton
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import com.xiaojinzi.tally.module.core.module.account_crud.domain.AccountCrudIntent
import kotlinx.coroutines.InternalCoroutinesApi

@OptIn(ExperimentalComposeUiApi::class)
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun AccountCrudView(
    needInit: Boolean? = false,
    previewDefault: AccountCrudPreviewDefault? = null,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    BusinessContentView<AccountCrudViewModel>(
        needInit = needInit,
    ) { vm ->
        val canEdit by vm.canEditStateOb.collectAsState(initial = false)
        val isEdit by vm.isEditStateOb.collectAsState(initial = false)
        val iconRsd by vm.iconRsdStateOb.collectAsState(initial = null)
        val name by vm.nameStateOb.collectAsState(initial = TextFieldValue())
        val initialBalanceStr by vm.initialBalanceStrStateOb.collectAsState(initial = "")
        val currentBalance by vm.currentBalanceStrStateOb.collectAsState(initial = 0f)
        val isExcludedAssets by vm.isExcludedAssetsStateOb.collectAsState(initial = false)
        val isDefault by vm.isDefaultStateOb.collectAsState(initial = false)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickableNoRipple {
                    softwareKeyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                modifier = Modifier
                    .clickableNoRipple {
                        vm.addIntent(
                            intent = AccountCrudIntent.IconSelect(
                                context = context,
                            ),
                        )
                    }
                    .padding(horizontal = 0.dp, vertical = 24.dp)
                    .circleClip()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                            elevation = 1.dp,
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .size(size = 36.dp)
                    .nothing(),
                painter = iconRsd?.run {
                    painterResource(id = this)
                } ?: ColorPainter(color = Color.Transparent),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )

            val focusRequesterForAccountName = remember { FocusRequester() }
            Row(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(shape = MaterialTheme.shapes.small)
                    .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .clickableNoRipple {
                        focusRequesterForAccountName.requestFocus()
                    }
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .nothing(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "账户名称",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (name.text.isEmpty()) {
                        Text(
                            text = "请输入账户名称",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.secondary,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                    BasicTextField(
                        modifier = Modifier
                            .focusRequester(
                                focusRequester = focusRequesterForAccountName,
                            )
                            .width(intrinsicSize = IntrinsicSize.Min)
                            .widthIn(min = 2.dp)
                            .wrapContentHeight()
                            .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                            .nothing(),
                        enabled = canEdit,
                        value = name,
                        onValueChange = {
                            vm.nameStateOb.value = it.copy(
                                text = it.text.trim().take(n = 8),
                            )
                        },
                        cursorBrush = SolidColor(
                            value = MaterialTheme.colorScheme.primary,
                        ),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(height = 12.dp)
                    .nothing()
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(shape = MaterialTheme.shapes.small)
                    .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .clickableNoRipple {
                        softwareKeyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        if (canEdit) {
                            vm.addIntent(
                                intent = AccountCrudIntent.EditBalance(
                                    context = context,
                                ),
                            )
                        }
                    }
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .nothing(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "初始余额",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start,
                )
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .nothing(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (initialBalanceStr.isEmpty()) {
                        Text(
                            text = "默认：0.00",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.secondary,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                    Text(
                        modifier = Modifier
                            .width(intrinsicSize = IntrinsicSize.Min)
                            .widthIn(min = 2.dp)
                            .wrapContentHeight()
                            .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                            .nothing(),
                        text = initialBalanceStr,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.secondary,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }

            if (isEdit) {
                Spacer(
                    modifier = Modifier
                        .height(height = 12.dp)
                        .nothing()
                )

                Row(
                    modifier = Modifier
                        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(shape = MaterialTheme.shapes.small)
                        .background(
                            color = MaterialTheme.colorScheme
                                .surfaceColorAtElevation(1.dp)
                                .copy(
                                    alpha = 0.5f,
                                )
                        )
                        .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                        .nothing(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "当前余额",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Start,
                    )
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight()
                            .nothing(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            modifier = Modifier
                                .width(intrinsicSize = IntrinsicSize.Min)
                                .widthIn(min = 2.dp)
                                .wrapContentHeight()
                                .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                                .nothing(),
                            text = currentBalance.format2f(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.secondary,
                            ),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .height(height = 12.dp)
                    .nothing()
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(shape = MaterialTheme.shapes.small)
                    .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .nothing(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "不计入总资产",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start,
                )
                Checkbox(
                    modifier = Modifier
                        .scale(scale = 0.8f)
                        .wrapContentSize()
                        .nothing(),
                    enabled = canEdit,
                    checked = isExcludedAssets,
                    onCheckedChange = {
                        vm.isExcludedAssetsStateOb.value = it
                    },
                )
            }

            Spacer(
                modifier = Modifier
                    .height(height = 12.dp)
                    .nothing()
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(shape = MaterialTheme.shapes.small)
                    .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .nothing(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "是否默认账户",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start,
                )
                Checkbox(
                    modifier = Modifier
                        .scale(scale = 0.8f)
                        .wrapContentSize()
                        .nothing(),
                    enabled = canEdit,
                    checked = isDefault,
                    onCheckedChange = {
                        vm.isDefaultStateOb.value = it
                    },
                )
            }

            if (canEdit) {

                Spacer(
                    modifier = Modifier
                        .height(height = 16.dp)
                        .nothing()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nothing(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isEdit) {
                        TextButton(
                            modifier = Modifier
                                .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                                .weight(weight = 1f, fill = true)
                                .wrapContentHeight()
                                .nothing(),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                            onClick = {
                                vm.addIntent(
                                    intent = AccountCrudIntent.Delete,
                                )
                            },
                        ) {
                            Text(
                                text = "删除",
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    AppCommonActionButton(
                        modifier = Modifier
                            .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                            .weight(weight = 1f, fill = true)
                            .wrapContentHeight()
                            .nothing(),
                        onClick = {
                            vm.addIntent(
                                intent = AccountCrudIntent.Submit,
                            )
                        },
                        text = "提交".toStringItemDto()
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
fun AccountCrudViewWrap() {
    val vm: AccountCrudViewModel = viewModel()
    val isEdit by vm.isEditStateOb.collectAsState(initial = false)
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = if (isEdit) {
                    "编辑账户"
                } else {
                    "创建账户"
                }.toStringItemDto(),
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            AccountCrudView()
        }
    }
}

private data class AccountCrudPreviewDefault(
    val str: String,
)

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun AccountCrudViewPreview() {
    AccountCrudView(
        needInit = false,
        previewDefault = AccountCrudPreviewDefault(
            str = "测试数据",
        )
    )
}