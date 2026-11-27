package com.xiaojinzi.tally.module.widget

/** Keep layout decisions shared by the live widget and its RemoteViews preview. */
internal data class WidgetLayout(
    val compact: Boolean,
    val wide: Boolean,
    val tall: Boolean,
    val largeText: Boolean,
) {
    companion object {
        fun select(width: Float, height: Float, fontScale: Float): WidgetLayout = WidgetLayout(
            compact = height < 160f || (fontScale >= 1.8f && height < 250f),
            wide = width >= 300f,
            tall = height >= 250f,
            largeText = fontScale > 1.2f,
        )
    }
}

/** An overfull legacy size may ellipsize, but must never shrink text to unreadable sizes. */
internal fun fitWidgetTextSize(preferredSize: Int, fits: (Int) -> Boolean): Int {
    val minimumSize = minOf(preferredSize, 11)
    return (preferredSize downTo minimumSize).firstOrNull(fits) ?: minimumSize
}
