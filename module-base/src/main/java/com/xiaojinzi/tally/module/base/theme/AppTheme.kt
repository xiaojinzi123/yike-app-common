package com.xiaojinzi.tally.module.base.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_CHINA_RED
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_CUSTOM1
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_FOLLOW_SYSTEM
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_KLEIN_BLUE
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_OLIVE_GREEN
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_SCHENBRUNN_YELLOW
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_TITIAN_RED
import com.xiaojinzi.tally.lib.res.ui.theme.custom1.Custom1AppTheme
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRedAppTheme
import com.xiaojinzi.tally.lib.res.ui.theme.four.SchenbrunnYellowAppTheme
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRedAppTheme
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_dark_background
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_light_background
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreenAppTheme
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreenDynamicAppTheme
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlueAppTheme
import com.xiaojinzi.tally.module.base.support.AppServices

@Composable
fun AppTheme(
    useDarkThemeForSystem: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val themeIndex by AppServices.appInfoSpi.themeIndexStateOb.collectAsState(initial = null)
    val useDarkThemeAdapter = themeIndex?.run {
        when (this) {
            1 -> false
            2 -> true
            else -> useDarkThemeForSystem
        }
    }
    val themeName by AppServices.appInfoSpi.themeNameState.collectAsState(initial = null)
    val isShowContent = themeIndex != null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .run {
                if (isShowContent) {
                    this
                } else {
                    this.background(
                        color = if (useDarkThemeForSystem) {
                            ChinaRed_md_theme_dark_background
                        } else {
                            ChinaRed_md_theme_light_background
                        },
                    )
                }
            }
            .nothing(),
        contentAlignment = Alignment.TopCenter,
    ) {
        useDarkThemeAdapter?.let {
            val themedContent: @Composable () -> Unit = {
                PortTheme(useDarkTheme = useDarkThemeAdapter) {
                    AppWallpaperHost(content = content)
                }
            }
            when (themeName) {

                THEME_NAME_CHINA_RED -> ChinaRedAppTheme(
                    useDarkTheme = useDarkThemeAdapter,
                    content = themedContent,
                )

                THEME_NAME_KLEIN_BLUE -> KleinBlueAppTheme(
                    useDarkTheme = useDarkThemeAdapter,
                    content = themedContent,
                )

                THEME_NAME_SCHENBRUNN_YELLOW -> SchenbrunnYellowAppTheme(
                    useDarkTheme = useDarkThemeAdapter,
                    content = themedContent,
                )

                THEME_NAME_TITIAN_RED -> TitianRedAppTheme(
                    useDarkTheme = useDarkThemeAdapter,
                    content = themedContent,
                )

                THEME_NAME_CUSTOM1 -> Custom1AppTheme(
                    useDarkTheme = useDarkThemeAdapter,
                    content = themedContent,
                )

                THEME_NAME_FOLLOW_SYSTEM -> OliveGreenDynamicAppTheme(
                    useDarkTheme = useDarkThemeAdapter,
                    content = themedContent,
                )

                THEME_NAME_OLIVE_GREEN -> OliveGreenAppTheme(
                    useDarkTheme = useDarkThemeAdapter,
                    content = themedContent,
                )

                else -> OliveGreenAppTheme(
                    useDarkTheme = useDarkThemeAdapter,
                    content = themedContent,
                )

            }
        }
    }
}