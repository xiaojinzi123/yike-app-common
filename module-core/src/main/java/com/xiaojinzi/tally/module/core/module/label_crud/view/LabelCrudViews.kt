package com.xiaojinzi.tally.module.core.module.label_crud.view

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.util.clickableNoRipple
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_LARGE
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.AppHeightSpace
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.view.compose.AppCommonActionButton
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import com.xiaojinzi.tally.module.core.module.label_crud.domain.LabelCrudIntent
import kotlinx.coroutines.InternalCoroutinesApi

@OptIn(ExperimentalComposeUiApi::class)
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun LabelCrudView(
    needInit: Boolean? = false,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    BusinessContentView<LabelCrudViewModel>(
        needInit = needInit,
    ) { vm ->
        val canDelete by vm.canDeleteStateOb.collectAsState(initial = false)
        val labelName by vm.labelNameStateOb.collectAsState(initial = TextFieldValue())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val focusRequesterForAccountName = remember { FocusRequester() }
            LaunchedEffect(key1 = focusRequesterForAccountName) {
                focusRequesterForAccountName.requestFocus()
                keyboardController?.show()
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(shape = MaterialTheme.shapes.small)
                    .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .clickableNoRipple {
                        focusRequesterForAccountName.requestFocus()
                        keyboardController?.show()
                    }
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .nothing(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "标签名称",
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
                    if (labelName.text.isEmpty()) {
                        Text(
                            text = "请输入标签名称",
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
                            .padding(horizontal = 0.dp, vertical = APP_PADDING_LARGE.dp)
                            .nothing(),
                        value = labelName,
                        enabled = true,
                        onValueChange = {
                            vm.labelNameStateOb.value = it.copy(
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
                    .height(height = 20.dp)
                    .nothing()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canDelete) {
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
                            keyboardController?.hide()
                            focusManager.clearFocus(
                                force = true,
                            )
                            vm.addIntent(
                                intent = LabelCrudIntent.Delete,
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
                    text = "提交".toStringItemDto(),
                ) {
                    vm.addIntent(
                        intent = LabelCrudIntent.Submit,
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
fun LabelCrudViewWrap() {
    val context = LocalContext.current
    val vm: LabelCrudViewModel = viewModel()
    val isEdit by vm.isEditStateOb.collectAsState(initial = false)
    val editLabelInfo by vm.editLabelInfoStateOb.collectAsState(initial = null)
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = if (isEdit) {
                    "更新标签"
                } else {
                    "新增标签"
                }.toStringItemDto(),
                menu1IconRsd = if (isEdit) {
                    com.xiaojinzi.tally.lib.res.R.drawable.res_search1
                } else {
                    null
                },
                menu1ClickListener = {
                    editLabelInfo?.id.orNull()?.let { labelId ->
                        AppRouterCoreApi::class
                            .routeApi()
                            .toBillSearchView(
                                context = context,
                                labelIdList = arrayListOf(
                                    labelId
                                ),
                            )
                    }
                },
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            LabelCrudView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun LabelCrudViewPreview() {
    LabelCrudView(
        needInit = false,
    )
}