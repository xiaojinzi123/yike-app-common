package com.xiaojinzi.tally.module.core.module.book_select.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.BottomView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.tally.lib.res.model.support.rememberPainter
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppBackgroundColor
import com.xiaojinzi.tally.lib.res.ui.copyNew
import com.xiaojinzi.tally.lib.res.ui.topShape
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.core.module.book_select.domain.BookSelectIntent
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BookSelectView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    BusinessContentView<BookSwitchViewModel>(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .nothing(),
        needInit = needInit,
    ) { vm ->
        val bookList by vm
            .allBookStateObVo
            .collectAsState(
                initial = emptyList()
            )
        val bookSelected by AppServices
            .tallyDataSourceSpi
            .selectedBookStateOb
            .collectAsState(
                initial = null
            )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(
                    shape = MaterialTheme.shapes.medium.topShape(),
                )
                .background(
                    color = AppBackgroundColor,
                )
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 0.dp, vertical = APP_PADDING_NORMAL.dp)
                    .nothing(),
                text = "选择账本",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Start,
            )
            Column(
                modifier = Modifier
                    .animateContentSize()
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(
                        state = rememberScrollState(),
                    )
                    .nothing(),
            ) {
                bookList.forEach { item ->
                    val isSelected = item.bookId == bookSelected?.id
                    ConstraintLayout(
                        modifier = Modifier
                            .padding(
                                horizontal = APP_PADDING_NORMAL.dp,
                                vertical = APP_PADDING_SMALL.dp,
                            )
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(
                                shape = MaterialTheme.shapes.small,
                            )
                            .run {
                                if (isSelected) {
                                    this.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            0.5f,
                                        ),
                                        shape = MaterialTheme.shapes.small,
                                    )
                                } else {
                                    this
                                }
                            }
                            .clickable {
                                vm.addIntent(
                                    intent = BookSelectIntent.SwitchBook(
                                        bookId = item.bookId,
                                    )
                                )
                            }
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                            )
                            .nothing(),
                    ) {
                        val (
                            icon,
                            name, textCurrentUse,
                        ) = createRefs()

                        if (isSelected) {
                            Text(
                                modifier = Modifier
                                    .constrainAs(ref = textCurrentUse) {
                                        this.top.linkTo(anchor = parent.top, margin = 0.dp)
                                        this.end.linkTo(anchor = parent.end, margin = 0.dp)
                                    }
                                    .clip(
                                        shape = MaterialTheme.shapes.small.copyNew(
                                            topStart = 0.dp,
                                            bottomEnd = 0.dp,
                                        )
                                    )
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            0.5f,
                                        ),
                                    )
                                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 4.dp)
                                    .nothing(),
                                text = "当前使用",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                ),
                                textAlign = TextAlign.Start,
                            )
                        }

                        Icon(
                            modifier = Modifier
                                .constrainAs(ref = icon) {
                                    this.top.linkTo(
                                        anchor = parent.top,
                                        margin = APP_PADDING_NORMAL.dp,
                                    )
                                    this.bottom.linkTo(
                                        anchor = parent.bottom,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                    this.start.linkTo(
                                        anchor = parent.start,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                }
                                .circleClip()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                        elevation = 1.dp,
                                    )
                                )
                                .padding(all = 8.dp)
                                .size(size = 20.dp)
                                .nothing(),
                            painter = item.bookIcon?.rememberPainter() ?: ColorPainter(
                                color = Color.Transparent,
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )

                        Text(
                            modifier = Modifier
                                .constrainAs(ref = name) {
                                    this.start.linkTo(
                                        anchor = icon.end,
                                        margin = APP_PADDING_NORMAL.dp
                                    )
                                    this.centerVerticallyTo(other = icon)
                                }
                                .nothing(),
                            text = item.bookName?.contentWithComposable().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            textAlign = TextAlign.Center,
                        )

                    }
                }
            }
            Button(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(top = APP_PADDING_SMALL.dp, bottom = APP_PADDING_NORMAL.dp)
                    .padding(horizontal = APP_PADDING_NORMAL.dp, vertical = 0.dp)
                    .fillMaxWidth()
                    .nothing(),
                onClick = {
                    AppRouterCoreApi::class
                        .routeApi()
                        .toBookCrudView(
                            context = context,
                        )
                },
            ) {
                Text(
                    modifier = Modifier
                        .nothing(),
                    text = "创建账本",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun BookSelectViewWrap() {
    BottomView(
        maxFraction = 0.7f,
    ) {
        BookSelectView()
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BookSelectViewPreview() {
    BookSelectView(
        needInit = false,
    )
}