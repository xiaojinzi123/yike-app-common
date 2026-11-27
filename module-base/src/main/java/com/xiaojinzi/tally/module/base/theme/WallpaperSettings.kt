package com.xiaojinzi.tally.module.base.theme

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.haooz.nexioui.theme.NexioTheme

/** 设置与主题页共用入口：系统照片选择器提供临时读取权限，导入后无需相册权限。 */
@Composable
fun WallpaperSettingsRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by AppWallpaperStore.state.collectAsState()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var pickerError by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(context) { AppWallpaperStore.ensureLoaded(context) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pickerOpen = false
        if (uri != null) AppWallpaperStore.select(context, uri)
    }
    val choose: () -> Unit = {
        if (!state.busy && !pickerOpen) {
            pickerError = null
            try {
                pickerOpen = true
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (_: ActivityNotFoundException) {
                pickerOpen = false
                pickerError = "系统图片选择器暂时不可用，请稍后重试"
            } catch (_: SecurityException) {
                pickerOpen = false
                pickerError = "系统暂时无法打开图片，请稍后重试"
            }
        }
    }
    val subtitle = when {
        state.busy -> "正在保存壁纸…"
        !state.loaded -> "正在读取壁纸…"
        state.bitmap != null -> "已自定义 · 点击预览、更换或清除"
        state.error != null -> "壁纸读取失败 · 点击重新选择或清除"
        else -> "选择本机图片作为应用背景"
    }
    PortActionRow(
        modifier = modifier,
        title = "应用壁纸",
        subtitle = subtitle,
        icon = rememberVectorPainter(Icons.Rounded.Wallpaper),
        onClick = { showDialog = true },
        trailing = {
            PortSecondaryButton(
                text = if (state.bitmap != null) "管理" else "设置",
                onClick = { showDialog = true },
            )
        },
    )
    if (showDialog) {
        val scheme = NexioTheme.colorScheme
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = scheme.surfaceContainer,
            title = { Text("应用壁纸", color = scheme.onSurface) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(PortDesign.ControlShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        val bitmap = state.bitmap
                        if (bitmap != null) {
                            Image(
                                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                                contentDescription = "当前应用壁纸预览",
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Box(Modifier.matchParentSize().background(wallpaperScrimColor()))
                        }
                        Text(
                            text = if (bitmap != null) "文字清晰，记账更专注" else "当前使用主题背景",
                            modifier = Modifier.padding(PortDesign.CardPadding),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        text = "从本机选择一张图片，自动裁剪铺满页面，并随浅色、深色外观添加柔和遮罩。壁纸仅保存在本机，重启后仍然有效。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = "仅更改应用背景；取消选择会保留当前壁纸。支持 32 MB 以内的图片，超大尺寸图片请缩小后再选。",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariantSummary,
                    )
                    if (state.busy) {
                        Text(
                            text = "正在处理图片，请稍候…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.primary,
                        )
                    }
                    (pickerError ?: state.error)?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.error,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
                    ) {
                        PortSecondaryButton(
                            text = if (state.bitmap != null) "更换图片" else "选择图片",
                            onClick = choose,
                            enabled = state.loaded && !state.busy && !pickerOpen,
                        )
                        PortSecondaryButton(
                            text = "清除壁纸",
                            onClick = {
                                pickerError = null
                                AppWallpaperStore.clear(context)
                            },
                            enabled = state.loaded && !state.busy && !pickerOpen &&
                                (state.bitmap != null || state.error != null),
                            destructive = true,
                        )
                    }
                }
            },
            confirmButton = {
                PortSecondaryButton(text = "完成", onClick = { showDialog = false })
            },
        )
    }
}
