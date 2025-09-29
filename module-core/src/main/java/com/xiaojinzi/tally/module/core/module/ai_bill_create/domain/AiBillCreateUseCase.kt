package com.xiaojinzi.tally.module.core.module.ai_bill_create.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

sealed class AiBillCreateIntent {

    data object Submit : AiBillCreateIntent()

}

@ViewModelLayer
interface AiBillCreateUseCase : BusinessMVIUseCase {

    @StateHotObservable
    val contentStateOb: MutableSharedStateFlow<String>

}

@ViewModelLayer
class AiBillCreateUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AiBillCreateUseCase {

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: AiBillCreateIntent.Submit) {
        val currentUserInfo = AppServices.userSpi.requiredUserInfo()
        val content = contentStateOb.firstOrNull()
        if (content.isNullOrEmpty()) {
            tip(content = "请输入内容".toStringItemDto())
        } else {
            val tallyDataSourceSpi = AppServices
                .tallyDataSourceSpi
            tallyDataSourceSpi
                .selectedBookStateOb
                .firstOrNull()?.let { currentBookInfo ->
                    val categoryList =
                        tallyDataSourceSpi.getCategoryByBookId(bookId = currentBookInfo.id)
                    val spendingCategoryList = categoryList.filter {
                        it.type == TallyCategoryDto.Companion.TallyCategoryType.SPENDING
                    }
                    val incomeCategoryList = categoryList.filter {
                        it.type == TallyCategoryDto.Companion.TallyCategoryType.INCOME
                    }
                    val analyzeResult = AppServices
                        .appNetworkSpi
                        .aiBillAnalyze(
                            spendingCategoryNameList = spendingCategoryList.mapNotNull { it.name.orNull() },
                            incomeCategoryNameList = incomeCategoryList.mapNotNull { it.name.orNull() },
                            content = content,
                        )
                    val amount = runCatching {
                        content
                            // 过滤出数字和.
                            .filter {
                                it.isDigit() || it == '.'
                            }.toFloatOrNull()
                    }.getOrNull()
                    if (amount == null) {
                        tip(content = "无法识别金额".toStringItemDto())
                    } else {
                        val targetCategoryItem = if (analyzeResult.isSpending == null) {
                            spendingCategoryList
                                .find { it.name == analyzeResult.categoryName }
                                ?: incomeCategoryList
                                    .find { it.name == analyzeResult.categoryName }
                        } else if(analyzeResult.isSpending == false) {
                            incomeCategoryList
                                .find { it.name == analyzeResult.categoryName }
                        } else {
                            spendingCategoryList
                                .find { it.name == analyzeResult.categoryName }
                        }
                        val isSpending = when (targetCategoryItem?.type) {
                            TallyCategoryDto.Companion.TallyCategoryType.INCOME -> false
                            else -> {
                                analyzeResult.isSpending != false
                            }
                        }
                        val absAmount = amount.times(100f).roundToLong().absoluteValue
                        tallyDataSourceSpi.insertBill(
                            target = TallyBillInsertDto(
                                userId = currentUserInfo.id,
                                bookId = currentBookInfo.id,
                                type = TallyBillDto.Type.NORMAL.value,
                                time = System.currentTimeMillis(),
                                categoryId = targetCategoryItem?.id,
                                amount = MoneyFen(
                                    value = if (!isSpending) {
                                        absAmount
                                    } else {
                                        -absAmount
                                    },
                                ),
                                note = content,
                            )
                        )
                        tip(content = "添加成功".toStringItemDto())
                        postActivityFinishEvent()
                    }

                }

        }

    }

    override val contentStateOb = MutableSharedStateFlow(
        initValue = "",
    )

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}