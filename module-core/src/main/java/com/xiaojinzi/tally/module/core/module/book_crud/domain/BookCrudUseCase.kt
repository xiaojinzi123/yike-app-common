package com.xiaojinzi.tally.module.core.module.book_crud.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.newUUid
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyRemoteCategoryRes
import com.xiaojinzi.tally.lib.res.model.tally.toInsertDto
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.LocalLedgerIdentity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class BookTemplate(
    val type: String,
    val name: String,
    val iconName: String,
    val description: String,
)

sealed class BookCrudIntent {

    data object Submit : BookCrudIntent()

    data class DataInit(
        val bookId: String?,
    ) : BookCrudIntent()

    data class ItemSelect(
        val item: BookTemplate,
    ) : BookCrudIntent()

}

@ViewModelLayer
interface BookCrudUseCase : BusinessMVIUseCase {

    @StateHotObservable
    val isEditStateOb: Flow<Boolean>

    /**
     * 账本名字
     */
    @StateHotObservable
    val bookNameStateOb: MutableSharedStateFlow<String>

    /**
     * 本地账本模板
     */
    @StateHotObservable
    val bookTypeListStateOb: Flow<List<BookTemplate>>

    /**
     * 选择的账本模板
     */
    @StateHotObservable
    val bookTypeSelectedStateOb: Flow<BookTemplate?>

}

@ViewModelLayer
class BookCrudUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BookCrudUseCase {

    private val editIdStateOb = MutableSharedStateFlow<String?>(initValue = null)
    private var isInitialized = false
    private var isSubmitting = false
    private var isSaved = false

    override val isEditStateOb = editIdStateOb.map { it != null }

    override val bookNameStateOb = MutableSharedStateFlow(
        initValue = "",
    )

    private val localTemplates = listOf(
        BookTemplate(
            type = TallyBookDto.TYPE_NORMAL,
            name = "日常账本",
            iconName = "book1",
            description = "内置常用收支分类，创建后即可记账",
        ),
        BookTemplate(
            type = TallyBookDto.TYPE_EMPTY,
            name = "空白账本",
            iconName = "book1",
            description = "从空白开始，自行添加收支分类",
        ),
    )

    override val bookTypeListStateOb = MutableSharedStateFlow(
        initValue = localTemplates,
    )

    override val bookTypeSelectedStateOb = MutableSharedStateFlow<BookTemplate?>(
        initValue = localTemplates.first(),
    )

    @IntentProcess
    private suspend fun dataInit(intent: BookCrudIntent.DataInit) {
        editIdStateOb.emit(value = intent.bookId)
        if (intent.bookId != null) {
            val book = AppServices.tallyDataSourceSpi.getBookById(id = intent.bookId)
            if (book == null) {
                tip(content = "账本不存在".toStringItemDto())
                postActivityFinishEvent()
                return
            }
            bookNameStateOb.emit(value = book.name.orEmpty())
        }
        isInitialized = true
    }

    @IntentProcess
    private suspend fun itemSelect(intent: BookCrudIntent.ItemSelect) {
        if (editIdStateOb.first() == null && intent.item in localTemplates) {
            bookTypeSelectedStateOb.emit(value = intent.item)
        }
    }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: BookCrudIntent.Submit) {
        if (!isInitialized || isSubmitting || isSaved) return
        isSubmitting = true
        try {
            val dataSource = AppServices.tallyDataSourceSpi
            val editId = editIdStateOb.first()
            val typeItem = bookTypeSelectedStateOb.first()
            val name = bookNameStateOb.first().trim()
                .ifEmpty { if (editId == null) typeItem?.name.orEmpty() else "" }
            if (name.isEmpty()) {
                tip(content = "请填写账本名称".toStringItemDto())
                return
            }
            if (editId != null) {
                val updated = dataSource.withTransaction {
                    val book = dataSource.getBookById(id = editId)
                        ?: return@withTransaction false
                    dataSource.insertBookList(
                        targetList = listOf(
                            book.toInsertDto().copy(
                                name = name,
                                timeModify = System.currentTimeMillis(),
                            ),
                        ),
                    )
                    true
                }
                if (!updated) {
                    tip(content = "账本不存在".toStringItemDto())
                    postActivityFinishEvent()
                    return
                }
                isSaved = true
                tip(content = "已保存".toStringItemDto())
            } else {
                if (typeItem == null) {
                    tip(content = "请选择账本模板".toStringItemDto())
                    return
                }
                val bookId = newUUid()
                val userId = LocalLedgerIdentity.requiredId()
                val currentTime = System.currentTimeMillis()
                dataSource.withTransaction {
                    dataSource.insertBookList(
                        targetList = listOf(
                            TallyBookInsertDto(
                                id = bookId,
                                userId = userId,
                                isSystem = false,
                                type = typeItem.type,
                                name = name,
                                iconName = typeItem.iconName,
                                timeCreate = currentTime,
                                timeModify = currentTime,
                            ),
                        ),
                    )
                    if (typeItem.type == TallyBookDto.TYPE_NORMAL) {
                        dataSource.insertOrUpdateCategoryList(
                            targetList = TallyRemoteCategoryRes.createForOpenSource().map {
                                it.toInsertDto().copy(bookId = bookId, userId = userId)
                            },
                        )
                    }
                }
                isSaved = true
                dataSource.switchBook(bookId = bookId)
                tip(content = "已创建并切换账本".toStringItemDto())
            }
            postActivityFinishEvent()
        } finally {
            isSubmitting = false
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}