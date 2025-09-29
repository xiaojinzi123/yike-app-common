package com.xiaojinzi.tally.module.core.module.book_info.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.MenuItem
import com.xiaojinzi.tally.lib.res.model.support.MenuItemLevel
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.bottomMenuSelect
import org.json.JSONObject

sealed class BookInfoIntent {

    data object Submit : BookInfoIntent()

    data object ScanQrCodeAndJoinBook : BookInfoIntent()

    data class BookSwitch(val id: String) : BookInfoIntent()

    data class OnMoreClick(
        @UiContext val context: Context,
        val bookId: String,
    ) : BookInfoIntent()

    data class DeleteOrQuitBook(
        @UiContext val context: Context,
        val bookId: String,
    ) : BookInfoIntent()

}

@ViewModelLayer
interface BookInfoUseCase : BusinessMVIUseCase {
}

@ViewModelLayer
class BookInfoUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BookInfoUseCase {

    @IntentProcess
    private suspend fun scanQrCodeAndJoinBook(intent: BookInfoIntent.ScanQrCodeAndJoinBook) {
        AppServices.qrCodeSpi?.scanQrCode().orNull()?.let { scanStr ->
            val jb = JSONObject(scanStr)
            val code = jb.getString("code")
            val bookId = jb.getString("bookId")
            // 接受邀请分享
            val bookNecessaryInfo = AppServices.appNetworkSpi.acceptBookShare(
                code = code,
                bookId = bookId,
            )
            // 插入到数据库
            AppServices
                .tallyDataSourceSpi
                .insertBookNecessaryInfo(
                    bookNecessaryInfo = bookNecessaryInfo,
                )
            tip(
                content = "接受邀请成功".toStringItemDto(),
            )
        }
    }

    @IntentProcess
    private suspend fun onMoreClick(intent: BookInfoIntent.OnMoreClick) {
        val userId = AppServices
            .userSpi
            .requiredLastUserId()
        val bookInfo = AppServices.tallyDataSourceSpi.getBookById(
            id = intent.bookId,
        ) ?: return
        // 这个账本是不是自己的
        val isOwner = bookInfo.userId == userId
        val menuList = if (bookInfo.isSystem) {
            listOf(
                MenuItem(content = "查看账单".toStringItemDto()),
            )
        } else {
            if (isOwner) {
                listOf(
                    MenuItem(content = "查看账单".toStringItemDto()),
                    MenuItem(content = "查看成员和分享".toStringItemDto()),
                    MenuItem(
                        content = "删除".toStringItemDto(),
                        level = MenuItemLevel.Danger,
                    ),
                )
            } else {
                listOf(
                    MenuItem(content = "查看账单".toStringItemDto()),
                    MenuItem(content = "查看成员".toStringItemDto()),
                    MenuItem(content = "退出这个账本".toStringItemDto()),
                )
            }
        }
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelect(
                context = intent.context,
                items = ArrayList(menuList),
            )
        when (selectIndex) {
            0 -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBillListView(
                        context = intent.context,
                        title = bookInfo.name.orEmpty().toStringItemDto(),
                        question = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                            bookIdList = listOf(intent.bookId),
                        )
                    )
            }

            1 -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBookMemberView(
                        context = intent.context,
                        bookId = intent.bookId,
                    )
            }

            2 -> {
                addIntent(
                    intent = BookInfoIntent.DeleteOrQuitBook(
                        context = intent.context,
                        bookId = intent.bookId,
                    )
                )
            }
        }
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun deleteOrQuitBook(intent: BookInfoIntent.DeleteOrQuitBook) {
        val userId = AppServices
            .userSpi
            .requiredLastUserId()
        val bookInfo = AppServices.tallyDataSourceSpi.getBookById(
            id = intent.bookId,
        ) ?: return
        // 这个账本是不是自己的
        val isOwner = bookInfo.userId == userId
        if (isOwner) {
            confirmDialogOrError(
                content = "删除这个账本会删除这个账本下的所有数据\n是否确定删除".toStringItemDto(),
            )
            // 删除这个账本
            AppServices.appNetworkSpi.exitBook(
                bookId = bookInfo.id,
                targetUserId = userId,
            )
        } else {
            confirmDialogOrError(
                content = "退出这个账本会删除此账本未同步的数据\n是否确定退出".toStringItemDto(),
            )
            // 退出这个账本
            AppServices.appNetworkSpi.exitBook(
                bookId = bookInfo.id,
                targetUserId = userId,
            )
        }
        // 删除这个账本下的所有数据
        AppServices
            .tallyDataSourceSpi
            .clearAllDataByBookId(
                bookId = bookInfo.id,
            )
    }

    @IntentProcess
    private suspend fun bookSwitch(intent: BookInfoIntent.BookSwitch) {
        /*confirmDialogOrError(
            content = "账本之间的数据完全隔离\n是否确定切换".toStringItemDto(),
        )*/
        AppServices
            .tallyDataSourceSpi
            .switchBook(
                bookId = intent.id,
                isTipAfterSwitch = true,
            )
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}