package com.xiaojinzi.tally.module.core.module.book_select1.view

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Visibility
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.compose.BottomView
import com.xiaojinzi.support.compose.util.circleClip
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.tally.lib.res.model.support.rememberPainter
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_NORMAL
import com.xiaojinzi.tally.lib.res.ui.APP_PADDING_SMALL
import com.xiaojinzi.tally.lib.res.ui.AppBackgroundColor
import com.xiaojinzi.tally.lib.res.ui.AppWidthSpace
import com.xiaojinzi.tally.module.core.module.book_select.domain.BookSelectIntent
import com.xiaojinzi.tally.module.core.module.book_select.view.BookSwitchViewModel
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BookSelect1View(
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
        val bookIdSelectList by vm.bookIdSelectListStateOb.collectAsState(initial = emptySet())
        val bookList by vm
            .allBookStateObVo
            .collectAsState(
                initial = emptyList()
            )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(
                    shape = MaterialTheme.shapes.medium,
                )
                .background(
                    color = AppBackgroundColor,
                )
                .navigationBarsPadding()
                .nothing(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppWidthSpace()
                Text(
                    text = "选择账本",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Start,
                )
                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                TextButton(onClick = {
                    vm.addIntent(
                        intent = BookSelectIntent.ReturnSelectList(
                            context = context,
                        ),
                    )
                }) {
                    Text(
                        text = "完成",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nothing(),
            ) {
                items(
                    items = bookList,
                ) { item ->
                    val isSelected = bookIdSelectList.contains(element = item.bookId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .nothing(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                                .clickable {
                                    vm.addIntent(
                                        intent = BookSelectIntent.SelectBook(
                                            bookId = item.bookId,
                                        ),
                                    )
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                )
                                .nothing(),
                        ) {
                            val (
                                icon, iconSelect,
                                name,
                            ) = createRefs()

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

                            Icon(
                                modifier = Modifier
                                    .constrainAs(ref = iconSelect) {
                                        this.end.linkTo(
                                            anchor = parent.end,
                                            margin = APP_PADDING_NORMAL.dp
                                        )
                                        this.centerVerticallyTo(other = parent)
                                        this.visibility = if (isSelected) {
                                            Visibility.Visible
                                        } else {
                                            Visibility.Gone
                                        }
                                    }
                                    .size(size = 16.dp)
                                    .nothing(),
                                painter = rememberVectorPainter(image = Icons.Default.Check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
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
fun BookSelect1ViewWrap() {
    BottomView(
        maxFraction = 0.7f,
    ) {
        BookSelect1View()
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BookSelect1ViewPreview() {
    BookSelect1View(
        needInit = false,
    )
}