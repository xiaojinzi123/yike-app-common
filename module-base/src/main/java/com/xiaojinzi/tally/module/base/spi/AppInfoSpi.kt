package com.xiaojinzi.tally.module.base.spi

import android.os.Build
import androidx.annotation.DrawableRes
import com.xiaojinzi.module.common.base.spi.spPersistence
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.HotStateFlow
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_CHINA_RED
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_CUSTOM1
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_FOLLOW_SYSTEM
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_KLEIN_BLUE
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_OLIVE_GREEN
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_SCHENBRUNN_YELLOW
import com.xiaojinzi.tally.lib.res.ui.THEME_NAME_TITIAN_RED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 对应不同的 App
 * 对于每一个 App 只会有一个实现类
 */
interface AppInfoSpi {

    companion object {
        const val THEME_FOLLOW_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    /** 当前安装变体的展示名称，与桌面名称保持一致。 */
    val appName: String
        get() = app.applicationInfo.loadLabel(app.packageManager).toString()

    /**
     * 类似 1.0.0 这种
     */
    val appVersionName: String

    /**
     * App 版本号
     */
    val appVersionCode: Long

    /**
     * App 包名
     */
    val appPackageName: String

    /**
     * 是否是开源版本
     */
    val forOpenSource: Boolean

    /**
     * App Foreground Icon 的资源 Id
     */
    @get:DrawableRes
    val appLauncherForegroundIconRsd: Int

    /**
     * App Icon 的资源 Id
     */
    @get:DrawableRes
    val appLauncherIconRsd: Int

    /**
     * App Icon 的资源 Id
     */
    @get:DrawableRes
    val appIconRsd: Int

    /**
     * 昼夜主题 Index
     * 1    亮色
     * 2    暗色
     * else 跟随系统
     */
    @StateHotObservable
    val themeIndexStateOb: Flow<Int>

    /**
     * 是否使用暗色主题
     */
    @StateHotObservable
    val isDarkThemeStateOb: Flow<Boolean>

    /**
     * 主题配色 Index
     */
    val themeNameState: HotStateFlow<String>

    /**
     * 切换主题, <= 0 是默认的主题
     * 1    亮色
     * 2    暗色
     * else 跟随系统
     */
    fun switchTheme(
        themeIndex: Int,
    )

    /**
     * 切换主题配色
     * [THEME_NAME_FOLLOW_SYSTEM]
     * [THEME_NAME_OLIVE_GREEN]
     * [THEME_NAME_CHINA_RED]
     * [THEME_NAME_KLEIN_BLUE]
     * [THEME_NAME_SCHENBRUNN_YELLOW]
     * [THEME_NAME_TITIAN_RED]
     * [THEME_NAME_CUSTOM1]
     */
    fun switchTheme(
        themeName: String,
    )

}

abstract class BaseAppInfoSpiImpl : AppInfoSpi {

    final override val appVersionName: String
        get() = try {
            checkNotNull(
                app
                    .packageManager
                    .getPackageInfo(app.packageName, 0)
                    .versionName
            )
        } catch (e: Exception) {
            "UnKnow"
        }

    final override val appVersionCode: Long
        get() = try {
            app
                .packageManager
                .getPackageInfo(app.packageName, 0).run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        this.longVersionCode
                    } else {
                        this.versionCode.toLong()
                    }
                }
        } catch (e: Exception) {
            0
        }

    final override val appPackageName: String
        get() = app.packageName

    override val forOpenSource: Boolean
        get() = false

    final override val themeIndexStateOb = MutableSharedStateFlow<Int>()
        .spPersistence(
            scope = AppScope,
            key = "themeIndex",
            def = 0,
        )

    final override val isDarkThemeStateOb = themeIndexStateOb
        .map {
            it == AppInfoSpi.THEME_DARK
        }

    final override val themeNameState = MutableSharedStateFlow<String>()
        .spPersistence(
            scope = AppScope,
            key = "themeName",
            def = "",
        )

    final override fun switchTheme(themeIndex: Int) {
        themeIndexStateOb.value = themeIndex
    }

    final override fun switchTheme(themeName: String) {
        themeNameState.value = themeName
    }

}