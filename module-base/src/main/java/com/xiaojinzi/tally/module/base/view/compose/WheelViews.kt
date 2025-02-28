package com.xiaojinzi.tally.module.base.view.compose

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.nothing
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

enum class DragValue { Start, Center, End }

@Composable
fun WheelItem(
    content: StringItemDto,
    isSelected: Boolean,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .nothing(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = content.contentWithComposable(),
            style = textStyle.copy(
                color = if (isSelected) {
                    textStyle.color
                } else {
                    textStyle.color.copy(
                        alpha = 0.5f,
                    )
                }
            ),
        )
    }
}

@Composable
private fun WheelColumnView(
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {

    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = { measurables, constraints ->
            val size = itemHeight.toPx().toInt()
            val placeables = measurables.map {
                it.measure(constraints)
            }
            val totalHeight =
                placeables.map { it.height }.reduceOrNull { acc, item -> acc + item } ?: 0
            layout(constraints.maxWidth, totalHeight) {
                var currentY = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(x = 0, y = currentY)
                    currentY += placeable.height
                }
            }
        }
    )

}

@Composable
fun <T> WheelView(
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    lineThickness: Dp = 1.dp,
    itemHeight: Dp = 48.dp,
    items: List<T>,
    wheelState: WheelState = rememberWheelState(initIndex = 0),
    itemContent: @Composable (item: T) -> Unit,
) {
    WheelViewWithIndex(
        modifier = modifier,
        visibleCount = visibleCount,
        lineThickness = lineThickness,
        itemHeight = itemHeight,
        items = items,
        wheelState = wheelState,
    ) { _, item ->
        itemContent(item)
    }
}

@Composable
fun <T> WheelViewWithIndex(
    modifier: Modifier = Modifier,
    visibleCount: Int = 3,
    lineThickness: Dp = 1.dp,
    itemHeight: Dp = 48.dp,
    lineWidthPercent: Float = 1f,
    items: List<T>,
    wheelState: WheelState = rememberWheelState(initIndex = 0),
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    WheelViewRaw(
        modifier = modifier,
        visibleCount = visibleCount,
        lineThickness = lineThickness,
        lineWidthPercent = lineWidthPercent,
        itemHeight = itemHeight,
        items = items,
        wheelState = wheelState,
    ) { index, item, _ ->
        itemContent(index, item)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelViewRaw(
    modifier: Modifier = Modifier,
    visibleCount: Int = 3,
    lineThickness: Dp = 1.dp,
    lineWidthPercent: Float = 1f,
    itemHeight: Dp = 48.dp,
    items: List<T>,
    wheelState: WheelState = rememberWheelState(initIndex = 0),
    itemContent: @Composable (index: Int, item: T, isSelected: Boolean) -> Unit,
) {

    val itemCount = items.size
    // 必须至少有一个
    check(value = itemCount > 0, lazyMessage = { "items is empty" })
    // 选中的下标不可以超过 items 的长度
    check(
        value = wheelState.currentIndex > -1 && wheelState.currentIndex < itemCount,
        lazyMessage = {
            "wheelState.currentIndex = ${wheelState.currentIndex}, itemCount = $itemCount"
        },
    )
    val targetSelectIndex: Int = (visibleCount) / 2
    println("wheelState.currentIndex = ${wheelState.currentIndex}, targetSelectIndex = $targetSelectIndex")
    val containerHeight: Dp = itemHeight * visibleCount
    val itemHeightPx: Float = with(LocalDensity.current) { itemHeight.toPx() }
    // 因为容器内的内容是居中的, 为了贴合 top, 所以计算需要的偏移
    val offsetY: Float = (itemCount - visibleCount) * itemHeightPx / 2f

    // 当第一项 index = 0 作为被选择项目的时候, 测试数据如下
    // itemCount    anchorStart     anchorEnd       ...
    // 1            0               0
    // 2            -1              0
    // 3            -2              0

    // 当考虑 visibleCount 的中间的 targetSelectIndex 作为被选择项目的时候,
    // 只需要将 [anchorStart, anchorEnd] 整体移动一个偏移量 anchorOffset
    val anchorOffset = targetSelectIndex - 0

    val anchorList by remember(key1 = itemCount) {
        mutableStateOf(
            value = ((1 - itemCount)..(0))
                .map {
                    it + anchorOffset
                }
                .map {
                    it to (itemHeightPx * it).roundToInt()
                }
        )
    }

    val firstAnchorIndex = anchorList.last().first

    val anchoredDraggableState = remember(key1 = itemCount) {
        AnchoredDraggableState(
            initialValue = firstAnchorIndex - wheelState.currentIndex,
            anchors = DraggableAnchors {
                anchorList
                    .forEach { entity ->
                        entity.first at entity.second.toFloat()
                    }
            },
            // 3
            positionalThreshold = { distance: Float -> distance},
            velocityThreshold = { Float.MAX_VALUE },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    /*val swipeableState =
        androidx.compose.material.rememberSwipeableState(firstAnchorIndex - wheelState.currentIndex)*/

    val swipeableStateTargetValue = firstAnchorIndex - wheelState.currentIndex
    if (anchoredDraggableState.currentValue != swipeableStateTargetValue) {
        LaunchedEffect(key1 = null) {
            anchoredDraggableState.animateTo(targetValue = swipeableStateTargetValue)
        }
    }
    LaunchedEffect(key1 = anchoredDraggableState) {
        snapshotFlow { anchoredDraggableState.currentValue }
            .onEach {
                val itemIndex = firstAnchorIndex - it
                if (wheelState.currentIndex != itemIndex) {
                    wheelState.currentIndex = itemIndex
                }
            }
            .launchIn(scope = this)
    }

    // 计算当前选中的 item 的下标

    val targetLineThickness = with(receiver = LocalDensity.current) {
        lineThickness.toPx()
    }
    val surfaceColorAtElevationColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
        elevation = 1.dp,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .height(containerHeight)
            .anchoredDraggable(
                state = anchoredDraggableState,
                orientation = Orientation.Vertical,
            )
            /*.swipeable(
                state = swipeableState,
                anchors = anchorMap,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Vertical
            )*/
            .drawWithContent {
                val lineWidth = drawContext.size.width * lineWidthPercent
                val lineStart = (drawContext.size.width - lineWidth) / 2f
                this.drawContent()
                if (targetLineThickness > 0f) {
                    this.drawLine(
                        color = surfaceColorAtElevationColor,
                        start = Offset(
                            x = lineStart,
                            y = targetSelectIndex * itemHeightPx,
                        ),
                        end = Offset(
                            x = lineStart + lineWidth,
                            y = targetSelectIndex * itemHeightPx,
                        ),
                        strokeWidth = targetLineThickness,
                    )
                }
                if (targetLineThickness > 0f) {
                    this.drawLine(
                        color = surfaceColorAtElevationColor,
                        start = Offset(
                            x = lineStart,
                            y = (targetSelectIndex + 1) * itemHeightPx,
                        ),
                        end = Offset(
                            x = lineStart + lineWidth,
                            y = (targetSelectIndex + 1) * itemHeightPx,
                        ),
                        strokeWidth = targetLineThickness,
                    )
                }
            }
            .graphicsLayer { clip = true }
            // .padding(horizontal = 16.dp, vertical = 0.dp)
            .nothing(),
        contentAlignment = Alignment.Center,
    ) {
        WheelColumnView(
            itemHeight = itemHeight,
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        x = 0,
                        y = offsetY.roundToInt(),
                    )
                }
                // 滚动的吸附的偏移
                .offset {
                    IntOffset(
                        x = 0,
                        y = if (anchoredDraggableState.offset.isNaN()) {
                            0
                        } else {
                            anchoredDraggableState.offset.roundToInt()
                        }
                    )
                }
                .nothing(),
        ) {
            items.forEachIndexed { index, t ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .nothing(),
                    contentAlignment = Alignment.Center,
                ) {
                    itemContent(index, t, index == wheelState.currentIndex)
                }
            }
        }
    }

}

class WheelState(currentIndex: Int) {

    var currentIndex by mutableStateOf(currentIndex)

    companion object {
        val Saver = object : Saver<WheelState, Int> {
            override fun restore(value: Int): WheelState {
                return WheelState(currentIndex = value)
            }

            override fun SaverScope.save(value: WheelState): Int {
                return value.currentIndex
            }
        }
    }

}

@Composable
fun rememberWheelState(
    initIndex: Int = 0,
) = rememberSaveable(saver = WheelState.Saver) {
    WheelState(currentIndex = initIndex)
}