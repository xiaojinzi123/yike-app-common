package com.xiaojinzi.tally.module.core.module.account_crud.domain

import android.content.Context
import androidx.annotation.UiContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.notSupportError
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.exception.NoBookSelectException
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.lib.res.model.tally.TallyAccountDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyAccountInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlin.math.roundToLong

sealed class AccountCrudIntent {

    data object Submit : AccountCrudIntent()

    data object Delete : AccountCrudIntent()

    data class EditBalance(
        @UiContext val context: Context,
    ) : AccountCrudIntent()

    data class IconSelect(
        @UiContext val context: Context,
    ) : AccountCrudIntent()

    data class DataInit(
        @UiContext val accountId: String?,
    ) : AccountCrudIntent()

}

@ViewModelLayer
interface AccountCrudUseCase : BusinessMVIUseCase {

    companion object {
        const val FLAG = "AccountCrudUseCase"
    }

    /**
     * 账户的数量
     */
    @StateHotObservable
    val accountCountStateOb: Flow<Int?>

    /**
     * 编辑的 Id
     */
    @StateHotObservable
    val editIdStateOb: Flow<String?>

    /**
     * 编辑的对象
     */
    @StateHotObservable
    val editInfoStateOb: Flow<TallyAccountDto?>

    /**
     * 是否是编辑
     */
    @StateHotObservable
    val isEditStateOb: Flow<Boolean>

    /**
     * 是否可以编辑
     */
    @StateHotObservable
    val canEditStateOb: Flow<Boolean>

    /**
     * Icon 的名字
     */
    @StateHotObservable
    val iconNameStateOb: Flow<String?>

    /**
     * Icon 的资源 Id
     */
    @StateHotObservable
    val iconRsdStateOb: Flow<Int?>

    /**
     * 账户名
     */
    @StateHotObservable
    val nameStateOb: MutableStateFlow<TextFieldValue>

    /**
     * 初始余额
     */
    @StateHotObservable
    val initialBalanceStrStateOb: Flow<String>

    /**
     * 当前余额
     */
    @StateHotObservable
    val currentBalanceStrStateOb: Flow<Float>

    /**
     * 不计入资产
     */
    @StateHotObservable
    val isExcludedAssetsStateOb: MutableStateFlow<Boolean>

    /**
     * 是否是默认的
     */
    @StateHotObservable
    val isDefaultStateOb: MutableStateFlow<Boolean>

}

@ViewModelLayer
class AccountCrudUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AccountCrudUseCase {

    override val accountCountStateOb = combine(
        AppServices
            .tallyDataSourceSpi
            .subscribeDataBaseTableChangedOb(
                TallyTable.Account, emitOneWhileSubscribe = true,
            ),
        AppServices
            .tallyDataSourceSpi
            .selectedBookStateOb,
    ) { _, currentBookInfo ->
        currentBookInfo?.let {
            AppServices
                .tallyDataSourceSpi
                .getAccountByBookId(
                    bookId = currentBookInfo.id,
                    isExcludeDeleted = true,
                )
                .count()
        }
    }.sharedStateIn(
        scope = scope,
    )

    override val editIdStateOb = MutableStateFlow<String?>(value = null)

    override val editInfoStateOb = combine(
        AppServices.tallyDataSourceSpi.selectedBookStateOb,
        editIdStateOb,
    ) { bookInfo, editId ->
        bookInfo?.let {
            editId?.let {
                AppServices.tallyDataSourceSpi.getAccountByIdAndBookId(
                    id = editId,
                    bookId = bookInfo.id,
                )
            }
        }
    }.sharedStateIn(
        scope = scope,
    )

    override val isEditStateOb = editIdStateOb.map {
        it.orNull() != null
    }

    override val canEditStateOb = combine(
        AppServices.userSpi.latestUserIdStateOb,
        isEditStateOb,
        editInfoStateOb,
    ) { selfUserId, isEdit, accountInfo ->
        if (isEdit) {
            selfUserId != null && selfUserId == accountInfo?.userId
        } else {
            true
        }
    }

    override val iconNameStateOb = MutableStateFlow<String?>(value = null)

    override val iconRsdStateOb = iconNameStateOb.map { iconName ->
        AppServices.iconMappingSpi.get(
            iconName = iconName,
        )
    }

    override val nameStateOb = MutableStateFlow(value = TextFieldValue())

    override val initialBalanceStrStateOb = MutableStateFlow(value = "")

    override val currentBalanceStrStateOb = MutableStateFlow(value = 0f)

    override val isExcludedAssetsStateOb = MutableStateFlow(value = false)

    override val isDefaultStateOb = MutableStateFlow(value = false)

    @IntentProcess
    private suspend fun dataInit(
        intent: AccountCrudIntent.DataInit,
    ) {
        val bookInfo = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        // 编辑的账户
        intent.accountId?.let { id ->
            val editAccount = AppServices
                .tallyDataSourceSpi
                .getAccountByIdAndBookId(
                    id = id,
                    bookId = bookInfo.id,
                )
            if (editAccount == null) {
                tip(content = "账户不存在".toStringItemDto())
                postActivityFinishEvent()
            } else {
                editIdStateOb.emit(
                    value = editAccount.id,
                )
                iconNameStateOb.emit(
                    value = editAccount.iconName,
                )
                (editAccount.name ?: "").let {
                    nameStateOb.emit(
                        value = TextFieldValue(
                            text = it,
                            selection = TextRange(
                                index = it.length,
                            ),
                        ),
                    )
                }
                editAccount.balanceInit.toYuan().value.format2f().let {
                    initialBalanceStrStateOb.emit(
                        value = it,
                    )
                }
                currentBalanceStrStateOb.emit(
                    value = editAccount.balanceCurrent.toYuan().value,
                )
                isExcludedAssetsStateOb.emit(
                    value = editAccount.isExcluded,
                )
                isDefaultStateOb.emit(
                    value = editAccount.isDefault,
                )
            }
        }
    }

    @IntentProcess
    private suspend fun iconSelect(
        intent: AccountCrudIntent.IconSelect,
    ) {
        val canEdit = canEditStateOb.first()
        if (canEdit) {
            val intentResult = AppRouterCoreApi::class
                .routeApi()
                .accountIconSelectSuspendForResult(
                    context = intent.context,
                )
            val iconName = intentResult.getStringExtra("iconName")
            iconNameStateOb.emit(
                value = iconName,
            )
        }
    }

    @IntentProcess
    private suspend fun submit(intent: AccountCrudIntent.Submit) {
        val userId = AppServices
            .userSpi
            .requiredLastUserId()
        val bookInfo = AppServices
            .tallyDataSourceSpi
            .selectedBookStateOb
            .firstOrNull() ?: throw NoBookSelectException()
        val editId = editIdStateOb.firstOrNull()
        val iconName = iconNameStateOb.firstOrNull()
        val name = nameStateOb.firstOrNull()?.text
        val initialBalanceStr = initialBalanceStrStateOb.firstOrNull()
        val isExcludedAssets = isExcludedAssetsStateOb.first()
        val isDefault = isDefaultStateOb.first()
        if (iconName.isNullOrEmpty()) {
            confirmDialog(
                content = "请选择账户图标".toStringItemDto(),
                negative = null,
            )
            return
        }
        if (name.isNullOrEmpty()) {
            confirmDialog(
                content = "请填写账户名称".toStringItemDto(),
                negative = null,
            )
            return
        }
        val initialBalance = if (initialBalanceStr.isNullOrEmpty()) {
            0.0
        } else {
            initialBalanceStr.toDoubleOrNull()
        }
        if (initialBalance == null) {
            confirmDialog(
                content = "账户余额请填写正确的格式".toStringItemDto(),
                negative = null,
            )
            return
        }
        if (editId.isNullOrEmpty()) {
            if (isDefault) {
                AppServices
                    .tallyDataSourceSpi
                    .updateAllAccountToNotDefaultByBookId(
                        bookId = bookInfo.id,
                    )
            }
            AppServices
                .tallyDataSourceSpi
                .insertOrUpdateAccount(
                    target = TallyAccountInsertDto(
                        userId = userId,
                        bookId = bookInfo.id,
                        iconName = iconName,
                        name = name,
                        balanceInit = MoneyFen(
                            value = initialBalance.times(other = 100).roundToLong(),
                        ),
                        isExcluded = isExcludedAssets,
                        isDefault = isDefault,
                    )
                )
            tip(content = "新增账户成功".toStringItemDto())
        } else {
            AppServices
                .tallyDataSourceSpi
                .getAccountByIdAndBookId(
                    id = editId,
                    bookId = bookInfo.id,
                )?.let { accountInfo ->
                    if (isDefault) {
                        AppServices
                            .tallyDataSourceSpi
                            .updateAllAccountToNotDefaultByBookId(
                                bookId = bookInfo.id,
                            )
                    }
                    AppServices
                        .tallyDataSourceSpi
                        .updateAccount(
                            target = accountInfo.copy(
                                iconName = iconName,
                                name = name,
                                balanceInit = MoneyFen(
                                    value = initialBalance.times(other = 100).roundToLong(),
                                ),
                                isExcluded = isExcludedAssets,
                                isDefault = isDefault,
                            )
                        )
                    tip(content = "编辑账户成功".toStringItemDto())
                }
        }
        postActivityFinishEvent()
    }

    @IntentProcess
    private suspend fun delete(intent: AccountCrudIntent.Delete) {
        val bookInfo = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        val editId = editIdStateOb.firstOrNull() ?: notSupportError()
        confirmDialogOrError(
            title = "⚠️".toStringItemDto(),
            content = "您确定要删除这个账户吗?".toStringItemDto(),
        )
        AppServices
            .tallyDataSourceSpi
            .getAccountByIdAndBookId(
                id = editId,
                bookId = bookInfo.id,
            )?.let { targetAccount ->
                AppServices
                    .tallyDataSourceSpi
                    .updateAccount(
                        target = targetAccount.copy(
                            isDeleted = true,
                        )
                    )
            }
        tip(content = "删除成功".toStringItemDto())
        postActivityFinishEvent()
    }

    @IntentProcess
    private suspend fun toEditBalance(intent: AccountCrudIntent.EditBalance) {
        val originStr = initialBalanceStrStateOb.firstOrNull()
        val newValue = AppRouterCoreApi::class
            .routeApi()
            .priceCalculateViewSuspend(
                context = intent.context,
                value = originStr,
            )
            .getDoubleExtra("data", 0.0)
        initialBalanceStrStateOb.emit(
            value = newValue.format2f(),
        )
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}