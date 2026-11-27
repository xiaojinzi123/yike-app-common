package com.xiaojinzi.tally.module.core.module.account_info.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortPrimaryButton
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.base.view.compose.AppCommonEmptyDataView
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun AccountInfoView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val horizontalPadding = if (isTablet) 20.dp else PortDesign.PagePadding
    val isAssetsVisible by AppServices.appConfigSpi.isAssetsVisibleStateOb.collectAsState(
        initial = false
    )
    BusinessContentView<AccountInfoViewModel>(
        needInit = needInit,
    ) { vm ->
        val allAccount by vm.allAccountStateOb.collectAsState(initial = emptyList())
        val assets by vm.assetsStateOb.collectAsState(initial = null)
        val dept by vm.deptStateOb.collectAsState(initial = null)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = if (isTablet) 12.dp else 0.dp,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(PortDesign.SectionGap),
        ) {
            item {
                PortSectionTitle(title = "资产概览")
            }
            item {
                PortCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "净资产",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(
                            onClick = {
                                AppServices
                                    .appConfigSpi
                                    .switchAssetsVisible(
                                        b = !isAssetsVisible,
                                    )
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(
                                    id = if (isAssetsVisible) {
                                        com.xiaojinzi.tally.lib.res.R.drawable.res_eye1
                                    } else {
                                        com.xiaojinzi.tally.lib.res.R.drawable.res_eye1_closed
                                    }
                                ),
                                contentDescription = if (isAssetsVisible) "隐藏资产" else "显示资产",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = if (isAssetsVisible) {
                            ((assets?.value ?: 0f) + (dept?.value ?: 0f)).format2f()
                        } else {
                            "*****"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(PortDesign.SectionGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PortDesign.SectionGap),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PortDesign.TinyGap),
                        ) {
                            Text(
                                text = "资产",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (isAssetsVisible) {
                                    (assets?.value ?: 0f).format2f()
                                } else {
                                    "*****"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PortDesign.TinyGap),
                        ) {
                            Text(
                                text = "负债",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (isAssetsVisible) {
                                    (dept?.value ?: 0f).format2f()
                                } else {
                                    "*****"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            item {
                PortSectionTitle(title = "资产账户")
            }
            if (allAccount.isEmpty()) {
                item {
                    PortCard {
                        AppCommonEmptyDataView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            text = "暂无资产账户".toStringItemDto(),
                        )
                    }
                }
            } else {
                items(
                    items = allAccount,
                    key = { it.id },
                ) { item ->
                    PortCard(contentPadding = PaddingValues(0.dp)) {
                        PortActionRow(
                            title = "${item.name.orEmpty()}${if (item.isDefault) " (默认)" else ""}",
                            icon = item.iconRsd?.let { painterResource(id = it) },
                            onClick = {
                                AppRouterCoreApi::class
                                    .routeApi()
                                    .toAccountDetailView(
                                        context = context,
                                        accountId = item.id,
                                    )
                            },
                            trailing = {
                                Text(
                                    text = if (isAssetsVisible) {
                                        item.balanceCurrent.value.format2f()
                                    } else {
                                        "*****"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.End,
                                )
                            },
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
fun AccountInfoViewWrap(
    isShowBackIcon: Boolean = true,
) {
    val context = LocalContext.current
    PortTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (isShowBackIcon) {
                    AppbarNormalM3(
                        title = "资产".toStringItemDto(),
                    )
                } else {
                    AppbarNormalM3(
                        backIcon = null,
                        title = "资产".toStringItemDto(),
                    )
                }
            },
            floatingActionButton = {
                PortPrimaryButton(
                    text = "添加账户",
                    icon = painterResource(id = com.xiaojinzi.tally.lib.res.R.drawable.res_add1),
                    onClick = {
                        AppRouterCoreApi::class
                            .routeApi()
                            .toAccountCrudView(
                                context = context,
                            )
                    },
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            ) {
                AccountInfoView()
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
private fun AccountInfoViewPreview() {
    PortTheme {
        AccountInfoView(
            needInit = false,
        )
    }
}
