package com.xiaojinzi.tally.module.qianji

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortCardExpand
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton

/** 预算、导入和旧工具入口共用的进度与反馈，颜色/间距均使用共享主题。 */
@Composable
internal fun TransferStatus(
    title: String,
    message: String? = null,
    busy: Boolean = false,
    error: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    PortCard(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PortDesign.ItemGap),
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
            PortCardExpand(visible = !message.isNullOrBlank() || (actionText != null && onAction != null)) {
                Column(
                    modifier = Modifier.padding(top = PortDesign.ItemGap),
                    verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap),
                ) {
                    if (!message.isNullOrBlank()) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (actionText != null && onAction != null) {
                        PortSecondaryButton(text = actionText, onClick = onAction)
                    }
                }
            }
        }
    }
}
