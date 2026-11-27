package com.xiaojinzi.tally.module.main.module.setting.domain

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

/** 通过 activity-alias 切换桌面图标；只影响图标，不迁移或清除本地数据。 */
object AppIconSwitcher {

    data class IconOption(
        val id: String,
        val label: String,
        val description: String,
        val alias: String,
    )

    // 与 opensource/app 清单中 activity-alias 的类全名一致（基于 namespace，不是 applicationId）。
    private const val ALIAS_PREFIX = "com.xiaojinzi.tally.opensource.Launcher"

    val options: List<IconOption> = listOf(
        IconOption("default", "小亚 · 墨蓝", "墨蓝底与暖白几何亚字", ALIAS_PREFIX + "Default"),
        IconOption("blue", "小亚 · 海蓝", "海蓝底与白色亚字账本", ALIAS_PREFIX + "Blue"),
        IconOption("green", "小亚 · 松绿", "松绿底与浅绿亚字记账符号", ALIAS_PREFIX + "Green"),
        IconOption("amber", "小亚 · 暖杏", "暖杏底与深棕亚字账页", ALIAS_PREFIX + "Amber"),
    )

    fun current(context: Context): IconOption {
        val manager = context.packageManager
        return options.firstOrNull {
            manager.getComponentEnabledSetting(ComponentName(context.packageName, it.alias)) !=
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } ?: options.first()
    }

    /** 返回是否已启用目标别名；不会停止应用进程，也不会改动账本数据。 */
    fun switchTo(context: Context, option: IconOption): Boolean {
        if (current(context).id == option.id) return true
        val manager = context.packageManager
        val packageName = context.packageName
        val target = ComponentName(packageName, option.alias)
        val changed = runCatching {
            // 先启用新图标，再关闭其他别名，避免桌面短暂没有入口。
            manager.setComponentEnabledSetting(
                target,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            options.filter { it.id != option.id }.forEach {
                manager.setComponentEnabledSetting(
                    ComponentName(packageName, it.alias),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }.isSuccess
        return changed && manager.getComponentEnabledSetting(target) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    fun iconBitmap(context: Context, option: IconOption, sizePx: Int): Bitmap? = runCatching {
        context.packageManager
            .getActivityIcon(ComponentName(context.packageName, option.alias))
            .toBitmap(sizePx)
    }.getOrNull()

    private fun Drawable.toBitmap(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, size, size)
        draw(Canvas(bitmap))
        return bitmap
    }
}
