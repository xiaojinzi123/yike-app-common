package com.xiaojinzi.tally.module.base.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.nexioui.button.ButtonDefaults
import com.haooz.nexioui.button.FlatTextButton
import com.haooz.nexioui.card.CardDefaults
import com.haooz.nexioui.theme.NexioTheme

@Composable
fun PortSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    color: Color = Color.Unspecified,
) {
    val scheme = NexioTheme.colorScheme
    val tint = when {
        color != Color.Unspecified -> color
        destructive -> scheme.error
        else -> scheme.onSurface
    }
    val dimBackground = if (scheme.surface.luminance() < 0.5f) {
        Color.White
    } else {
        Color.Black
    }
    FlatTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minWidth = 64.dp,
        minHeight = 36.dp,
        cornerRadius = 100.dp,
        insideMargin = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        colors = ButtonDefaults.textButtonColors(
            color = dimBackground,
            disabledColor = dimBackground,
            textColor = tint,
            disabledTextColor = tint.copy(alpha = 0.38f),
            alpha = 0.06f,
        ),
    )
}

@Composable
fun PortStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    isError: Boolean = false,
) {
    val scheme = NexioTheme.colorScheme
    val tint = when {
        isError -> scheme.error
        emphasized -> scheme.primary
        else -> scheme.onSurfaceVariantSummary
    }
    Surface(
        modifier = modifier.widthIn(max = 180.dp),
        shape = CircleShape,
        color = tint.copy(alpha = 0.08f),
        contentColor = tint,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PortDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = PortDesign.CardPadding),
        thickness = 0.5.dp,
        color = NexioTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.1f),
    )
}

@Composable
fun PortInlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    hint: String = "",
) {
    val scheme = NexioTheme.colorScheme
    val contentColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.38f)
        isError -> scheme.error
        else -> scheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PortDesign.ActionRowHeight)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1.5f)
                .padding(start = 16.dp)
                .semantics {
                    contentDescription = label
                    if (isError) error("请检查输入内容")
                },
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = contentColor,
                textAlign = TextAlign.End,
            ),
            cursorBrush = SolidColor(scheme.primary),
            decorationBox = { innerTextField ->
                if (value.isEmpty() && hint.isNotEmpty()) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = hint,
                            color = scheme.onSurfaceVariantSummary.copy(alpha = if (enabled) 1f else 0.38f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                        )
                        innerTextField()
                    }
                } else {
                    innerTextField()
                }
            },
        )
    }
}

private object PortExpandSpec {
    const val FadeMillis = 180
    const val BlurDp = 12f
    const val BlurInMillis = 420
    const val BlurOutMillis = 160
}

/** 只在过渡期间挂绘制层，避免长卡片的触摸区域被常驻图层截断。 */
@Composable
fun PortCardExpand(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(animationSpec = CardDefaults.SizeAnimation, expandFrom = Alignment.Top),
        exit = shrinkVertically(animationSpec = CardDefaults.SizeAnimation, shrinkTowards = Alignment.Top),
    ) {
        val animating by remember { derivedStateOf { transition.currentState != transition.targetState } }
        val opacity = transition.animateFloat(
            transitionSpec = { tween(PortExpandSpec.FadeMillis) },
            label = "PortCardOpacity",
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
        val focus = transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState == EnterExitState.Visible) {
                        PortExpandSpec.BlurInMillis
                    } else {
                        PortExpandSpec.BlurOutMillis
                    },
                    easing = FastOutSlowInEasing,
                )
            },
            label = "PortCardFocus",
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
        Column(
            modifier = if (animating) {
                Modifier.graphicsLayer {
                    alpha = opacity.value
                    val radius = PortExpandSpec.BlurDp.dp.toPx() * (1f - focus.value)
                    renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0.01f) {
                        BlurEffect(radius, radius, TileMode.Decal)
                    } else null
                }
            } else Modifier,
            content = content,
        )
    }
}
