package com.xiaojinzi.tally.module.main.module.theme_select.view

import android.os.Build
import com.xiaojinzi.reactive.view.BaseViewModel
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_CHINA_RED
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_FOLLOW_SYSTEM
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_KLEIN_BLUE
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_OLIVE_GREEN
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_SCHENBRUNN_YELLOW
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_TITIAN_RED
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_dark_primary
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_dark_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_dark_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_dark_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_dark_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_dark_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_light_primary
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_light_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_light_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_light_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_light_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.five.TitianRed_md_theme_light_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_dark_primary
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_dark_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_dark_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_dark_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_dark_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_dark_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_light_primary
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_light_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_light_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_light_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_light_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.four.Schenbrunn_Yellowmd_md_theme_light_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_dark_primary
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_dark_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_dark_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_dark_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_dark_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_dark_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_light_primary
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_light_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_light_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_light_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_light_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.one.ChinaRed_md_theme_light_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_dark_primary
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_dark_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_dark_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_dark_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_dark_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_dark_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_light_primary
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_light_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_light_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_light_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_light_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.three.OliveGreen_md_theme_light_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_dark_primary
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_dark_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_dark_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_dark_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_dark_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_dark_tertiaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_light_primary
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_light_primaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_light_secondary
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_light_secondaryContainer
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_light_tertiary
import com.xiaojinzi.tally.lib.res.ui.theme.two.KleinBlue_md_theme_light_tertiaryContainer
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.main.module.theme_select.domain.ThemeSelectUseCase
import com.xiaojinzi.tally.module.main.module.theme_select.domain.ThemeSelectUseCaseImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@ViewLayer
class ThemeSelectViewModel(
    private val useCase: ThemeSelectUseCase = ThemeSelectUseCaseImpl(),
) : BaseViewModel(),
    ThemeSelectUseCase by useCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    @StateHotObservable
    val themeListStateObVo: Flow<List<ThemeSelectItemVo>> = AppServices
        .appInfoSpi
        .themeNameState
        .flatMapLatest { themeName ->
            flowOf(
                arrayListOf(
                    ThemeSystemSelectItemVo(
                        name = "系统配色",
                        themeName = THEME_NAME_FOLLOW_SYSTEM,
                        isSelected = themeName == THEME_NAME_FOLLOW_SYSTEM,
                    ),
                    ThemeNormalSelectItemVo(
                        name = "中国红",
                        themeName = THEME_NAME_CHINA_RED,
                        isSelected = themeName == THEME_NAME_CHINA_RED,
                        lightPrimary = ChinaRed_md_theme_light_primary,
                        lightPrimaryContainer = ChinaRed_md_theme_light_primaryContainer,
                        darkPrimary = ChinaRed_md_theme_dark_primary,
                        darkPrimaryContainer = ChinaRed_md_theme_dark_primaryContainer,
                        lightSecondary = ChinaRed_md_theme_light_secondary,
                        lightSecondaryContainer = ChinaRed_md_theme_light_secondaryContainer,
                        darkSecondary = ChinaRed_md_theme_dark_secondary,
                        darkSecondaryContainer = ChinaRed_md_theme_dark_secondaryContainer,
                        lightTertiary = ChinaRed_md_theme_light_tertiary,
                        lightTertiaryContainer = ChinaRed_md_theme_light_tertiaryContainer,
                        darkTertiary = ChinaRed_md_theme_dark_tertiary,
                        darkTertiaryContainer = ChinaRed_md_theme_dark_tertiaryContainer,
                    ),
                    ThemeNormalSelectItemVo(
                        name = "克莱因蓝",
                        themeName = THEME_NAME_KLEIN_BLUE,
                        isSelected = themeName == THEME_NAME_KLEIN_BLUE,
                        lightPrimary = KleinBlue_md_theme_light_primary,
                        lightPrimaryContainer = KleinBlue_md_theme_light_primaryContainer,
                        darkPrimary = KleinBlue_md_theme_dark_primary,
                        darkPrimaryContainer = KleinBlue_md_theme_dark_primaryContainer,
                        lightSecondary = KleinBlue_md_theme_light_secondary,
                        lightSecondaryContainer = KleinBlue_md_theme_light_secondaryContainer,
                        darkSecondary = KleinBlue_md_theme_dark_secondary,
                        darkSecondaryContainer = KleinBlue_md_theme_dark_secondaryContainer,
                        lightTertiary = KleinBlue_md_theme_light_tertiary,
                        lightTertiaryContainer = KleinBlue_md_theme_light_tertiaryContainer,
                        darkTertiary = KleinBlue_md_theme_dark_tertiary,
                        darkTertiaryContainer = KleinBlue_md_theme_dark_tertiaryContainer,
                    ),
                    ThemeNormalSelectItemVo(
                        name = "橄榄绿",
                        themeName = THEME_NAME_OLIVE_GREEN,
                        isSelected = themeName.isEmpty() || themeName == THEME_NAME_OLIVE_GREEN,
                        lightPrimary = OliveGreen_md_theme_light_primary,
                        lightPrimaryContainer = OliveGreen_md_theme_light_primaryContainer,
                        darkPrimary = OliveGreen_md_theme_dark_primary,
                        darkPrimaryContainer = OliveGreen_md_theme_dark_primaryContainer,
                        lightSecondary = OliveGreen_md_theme_light_secondary,
                        lightSecondaryContainer = OliveGreen_md_theme_light_secondaryContainer,
                        darkSecondary = OliveGreen_md_theme_dark_secondary,
                        darkSecondaryContainer = OliveGreen_md_theme_dark_secondaryContainer,
                        lightTertiary = OliveGreen_md_theme_light_tertiary,
                        lightTertiaryContainer = OliveGreen_md_theme_light_tertiaryContainer,
                        darkTertiary = OliveGreen_md_theme_dark_tertiary,
                        darkTertiaryContainer = OliveGreen_md_theme_dark_tertiaryContainer,
                    ),
                    ThemeNormalSelectItemVo(
                        name = "申布伦黄",
                        themeName = THEME_NAME_SCHENBRUNN_YELLOW,
                        isSelected = themeName == THEME_NAME_SCHENBRUNN_YELLOW,
                        lightPrimary = Schenbrunn_Yellowmd_md_theme_light_primary,
                        lightPrimaryContainer = Schenbrunn_Yellowmd_md_theme_light_primaryContainer,
                        darkPrimary = Schenbrunn_Yellowmd_md_theme_dark_primary,
                        darkPrimaryContainer = Schenbrunn_Yellowmd_md_theme_dark_primaryContainer,
                        lightSecondary = Schenbrunn_Yellowmd_md_theme_light_secondary,
                        lightSecondaryContainer = Schenbrunn_Yellowmd_md_theme_light_secondaryContainer,
                        darkSecondary = Schenbrunn_Yellowmd_md_theme_dark_secondary,
                        darkSecondaryContainer = Schenbrunn_Yellowmd_md_theme_dark_secondaryContainer,
                        lightTertiary = Schenbrunn_Yellowmd_md_theme_light_tertiary,
                        lightTertiaryContainer = Schenbrunn_Yellowmd_md_theme_light_tertiaryContainer,
                        darkTertiary = Schenbrunn_Yellowmd_md_theme_dark_tertiary,
                        darkTertiaryContainer = Schenbrunn_Yellowmd_md_theme_dark_tertiaryContainer,
                    ),
                    ThemeNormalSelectItemVo(
                        name = "提香红",
                        themeName = THEME_NAME_TITIAN_RED,
                        isSelected = themeName == THEME_NAME_TITIAN_RED,
                        lightPrimary = TitianRed_md_theme_light_primary,
                        lightPrimaryContainer = TitianRed_md_theme_light_primaryContainer,
                        darkPrimary = TitianRed_md_theme_dark_primary,
                        darkPrimaryContainer = TitianRed_md_theme_dark_primaryContainer,
                        lightSecondary = TitianRed_md_theme_light_secondary,
                        lightSecondaryContainer = TitianRed_md_theme_light_secondaryContainer,
                        darkSecondary = TitianRed_md_theme_dark_secondary,
                        darkSecondaryContainer = TitianRed_md_theme_dark_secondaryContainer,
                        lightTertiary = TitianRed_md_theme_light_tertiary,
                        lightTertiaryContainer = TitianRed_md_theme_light_tertiaryContainer,
                        darkTertiary = TitianRed_md_theme_dark_tertiary,
                        darkTertiaryContainer = TitianRed_md_theme_dark_tertiaryContainer,
                    ),
                ).run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        this
                    } else {
                        this.filter {
                            it.themeName != THEME_NAME_FOLLOW_SYSTEM
                        }
                    }
                }
            )
        }

}