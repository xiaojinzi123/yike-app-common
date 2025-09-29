package com.xiaojinzi.tally.module.core.module.bill_album.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDetailDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillImageDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.support.AppRouterImagePreviewApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

sealed class BillAlbumIntent {

    data object Submit : BillAlbumIntent()

    data class ToImagePreview(
        @UiContext val context: Context,
        val billImageId: String,
    ) : BillAlbumIntent()

}

@ViewModelLayer
interface BillAlbumUseCase : BusinessMVIUseCase {

    /**
     * 账单列表
     */
    @StateHotObservable
    val billImageListStateOb: Flow<List<Pair<TallyBillDetailDto, List<TallyBillImageDto>>>>

}

@ViewModelLayer
class BillAlbumUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BillAlbumUseCase {

    @StateHotObservable
    override val billImageListStateOb: Flow<List<Pair<TallyBillDetailDto, List<TallyBillImageDto>>>> =
        AppServices.tallyDataSourceSpi.let { tallyDataSourceSpi ->
            combine(
                tallyDataSourceSpi.subscribeDataBaseTableChangedOb(
                    TallyTable.BillImage, emitOneWhileSubscribe = true,
                ),
                tallyDataSourceSpi.selectedBookStateOb,
            ) { _, currentSelectedBook ->
                (currentSelectedBook?.let {
                    tallyDataSourceSpi.getBillImageList(
                        bookId = currentSelectedBook.id,
                    ).mapNotNull { it.getAdapter }
                } ?: emptyList())
                    .groupBy { it.billId }
                    .toList()
                    .mapNotNull { item ->
                        tallyDataSourceSpi.getBillDetailById(
                            id = item.first,
                        )?.getAdapter?.let { billDetailItem ->
                            billDetailItem to item.second
                        }
                    }
                    // 根据时间倒序
                    .sortedByDescending {
                        it.first.core.time
                    }
            }
        }.sharedStateIn(
            scope = scope,
        )

    @IntentProcess
    private suspend fun toImagePreview(intent: BillAlbumIntent.ToImagePreview) {
        billImageListStateOb
            .first()
            .find { itemPair ->
                itemPair.second.any {
                    it.id == intent.billImageId
                }
            }?.let { itemPair ->
                val billImageList = itemPair.second.filter { it.url.orNull() != null }
                AppRouterImagePreviewApi::class
                    .routeApi()
                    .toImagePreviewView(
                        context = intent.context,
                        urlList = ArrayList(
                            billImageList.mapNotNull { it.url },
                        ),
                        index = billImageList.indexOfFirst {
                            it.id == intent.billImageId
                        },
                    )
            }
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: BillAlbumIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}