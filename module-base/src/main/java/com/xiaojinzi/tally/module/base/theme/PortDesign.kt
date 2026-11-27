package com.xiaojinzi.tally.module.base.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.nexioui.basiccomponent.BasicComponent
import com.haooz.nexioui.button.Button
import com.haooz.nexioui.button.ButtonDefaults
import com.haooz.nexioui.button.FlatTextButton
import com.haooz.nexioui.card.Card
import com.haooz.nexioui.indication.NexioIndication
import com.haooz.nexioui.theme.NexioDarkColorScheme
import com.haooz.nexioui.theme.NexioLightColorScheme
import com.haooz.nexioui.theme.NexioTheme

/** 主流程与钱迹页面共用 NexioUI 的尺寸和组件，旧页面通过 MaterialTheme 适配同一配色。 */
object PortDesign {
    val PagePadding = 16.dp
    val CardPadding = 16.dp
    val SectionGap = 12.dp
    val ItemGap = 12.dp
    val SmallGap = 8.dp
    val TinyGap = 4.dp
    val TouchTarget = 48.dp
    val ButtonHeight = 48.dp
    val ActionRowHeight = 56.dp
    val IconSize = 22.dp
    val IconContainer = 36.dp
    val NavigationHeight = 72.dp
    val ProgressHeight = 6.dp
    val MaxContentWidth = 840.dp
    val CardShape = RoundedCornerShape(20.dp)
    val ControlShape = RoundedCornerShape(16.dp)
    val SmallShape = RoundedCornerShape(12.dp)
}

/** 继承 AppTheme 已解析的明暗模式，包括用户手动选择的外观。 */
@Composable
fun PortTheme(
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable () -> Unit,
) {
    val outerColors = MaterialTheme.colorScheme
    val scheme = remember(useDarkTheme, outerColors) {
        (if (useDarkTheme) NexioDarkColorScheme else NexioLightColorScheme).copy(
            primary = outerColors.primary,
            onPrimary = outerColors.onPrimary,
            primaryContainer = outerColors.primaryContainer,
            onPrimaryContainer = outerColors.onPrimaryContainer,
            secondary = outerColors.secondary,
            onSecondary = outerColors.onSecondary,
            secondaryContainer = outerColors.secondaryContainer,
            onSecondaryContainer = outerColors.onSecondaryContainer,
            tertiaryContainer = outerColors.tertiaryContainer,
            onTertiaryContainer = outerColors.onTertiaryContainer,
        )
    }
    val materialColors = remember(scheme, outerColors) {
        outerColors.copy(
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            primaryContainer = scheme.primaryContainer,
            onPrimaryContainer = scheme.onPrimaryContainer,
            secondary = scheme.secondary,
            onSecondary = scheme.onSecondary,
            secondaryContainer = scheme.secondaryContainer,
            onSecondaryContainer = scheme.onSecondaryContainer,
            background = scheme.surface,
            onBackground = scheme.onSurface,
            surface = scheme.surfaceContainer,
            onSurface = scheme.onSurfaceContainer,
            surfaceVariant = scheme.surfaceVariant,
            onSurfaceVariant = scheme.onSurfaceVariantSummary,
            surfaceContainerLowest = scheme.surface,
            surfaceContainerLow = scheme.surfaceContainer,
            surfaceContainer = scheme.surfaceContainer,
            surfaceContainerHigh = scheme.surfaceContainerHigh,
            surfaceContainerHighest = scheme.surfaceContainerHighest,
            outline = scheme.outline,
            outlineVariant = scheme.onSurfaceVariantActions.copy(alpha = 0.1f),
            error = scheme.error,
            surfaceTint = Color.Transparent,
        )
    }
    val typography = MaterialTheme.typography
    NexioTheme(isDark = useDarkTheme, colorScheme = scheme) {
        MaterialTheme(
            colorScheme = materialColors,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = PortDesign.SmallShape,
                medium = PortDesign.ControlShape,
                large = PortDesign.CardShape,
                extraLarge = RoundedCornerShape(28.dp),
            ),
            typography = typography.copy(
                headlineLarge = typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    lineHeight = 38.sp,
                ),
                headlineMedium = typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                ),
                titleLarge = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                titleMedium = typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium),
                titleSmall = typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                bodyLarge = typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                bodyMedium = typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                bodySmall = typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            ),
        ) {
            val indication = remember(scheme.onSurface) { NexioIndication(color = scheme.onSurface) }
            CompositionLocalProvider(
                LocalContentColor provides scheme.onSurface,
                LocalIndication provides indication,
            ) {
                content()
            }
        }
    }
}

/** 独立页面容器。嵌套 LazyColumn 时传 scrollable = false，并在内容中使用 weight。 */
@Composable
fun PortPage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val horizontalPadding = if (LocalConfiguration.current.screenWidthDp >= 600) 20.dp else PortDesign.PagePadding
    Surface(
        modifier = modifier.fillMaxSize(),
        color = appPageBackgroundColor(),
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .widthIn(max = PortDesign.MaxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = PortDesign.SmallGap)
                        .heightIn(min = PortDesign.TouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        modifier = Modifier.size(PortDesign.TouchTarget),
                        onClick = onBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    actions()
                }
            }
            Column(
                modifier = Modifier
                    .widthIn(max = PortDesign.MaxContentWidth)
                    .fillMaxWidth()
                    .weight(1f)
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = horizontalPadding, vertical = PortDesign.ItemGap),
                verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PortDesign.SmallGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = Modifier.semantics { heading() },
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                modifier = Modifier.padding(top = PortDesign.SmallGap),
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (onBack == null) actions()
                }
                content()
            }
        }
    }
}

@Composable
fun PortCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(PortDesign.CardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 20.dp,
        insideMargin = contentPadding,
        content = content,
    )
}

@Composable
fun PortActionRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = NexioTheme.colorScheme
    BasicComponent(
        modifier = modifier.fillMaxWidth(),
        title = title,
        summary = subtitle,
        enabled = enabled,
        onClick = onClick,
        startAction = icon?.let { painter ->
            {
                Box(
                    modifier = Modifier
                        .size(PortDesign.IconContainer)
                        .clip(PortDesign.SmallShape)
                        .background(scheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(PortDesign.IconSize),
                        painter = painter,
                        contentDescription = null,
                        tint = scheme.primary.copy(alpha = if (enabled) 1f else 0.38f),
                    )
                }
            }
        },
        endActions = if (trailing != null || onClick != null) {
            {
                if (trailing != null) {
                    trailing()
                } else {
                    Icon(
                        modifier = Modifier.size(PortDesign.IconSize),
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariantActions.copy(alpha = if (enabled) 1f else 0.38f),
                    )
                }
            }
        } else null,
    )
}

@Composable
fun PortSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier.padding(start = PortDesign.CardPadding, top = PortDesign.SmallGap),
        verticalArrangement = Arrangement.spacedBy(PortDesign.TinyGap),
    ) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NexioTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NexioTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
fun PortPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
) {
    if (icon == null) {
        FlatTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier.heightIn(min = PortDesign.ButtonHeight),
            enabled = enabled,
            minHeight = PortDesign.ButtonHeight,
            cornerRadius = 100.dp,
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.titleSmall,
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    } else {
        val colors = ButtonDefaults.buttonColorsPrimary()
        val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = PortDesign.ButtonHeight),
            enabled = enabled,
            minHeight = PortDesign.ButtonHeight,
            cornerRadius = 100.dp,
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            colors = colors,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(PortDesign.IconSize),
                tint = contentColor,
            )
            Spacer(Modifier.width(PortDesign.SmallGap))
            Text(text = text, style = MaterialTheme.typography.titleSmall, color = contentColor)
        }
    }
}
