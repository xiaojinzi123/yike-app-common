package com.xiaojinzi.tally.module.base.support

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.UiContext
import com.xiaojinzi.component.anno.router.AfterRouteActionAnno
import com.xiaojinzi.component.anno.router.AfterRouteEventActionAnno
import com.xiaojinzi.component.anno.router.HostAndPathAnno
import com.xiaojinzi.component.anno.router.NavigateAnno
import com.xiaojinzi.component.anno.router.ParameterAnno
import com.xiaojinzi.component.anno.router.RequestCodeAnno
import com.xiaojinzi.component.anno.router.RouterApiAnno
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.component.support.ParameterSupport
import com.xiaojinzi.module.common.base.CommonRouterConfig
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.ktx.notSupportError
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.tally.lib.res.model.support.DateTimeModel
import com.xiaojinzi.tally.lib.res.model.support.DateTimeType
import com.xiaojinzi.tally.lib.res.model.support.MenuItem
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi

@RouterApiAnno
interface AppRouterCoreApi {

    @HostAndPathAnno(value = AppRouterConfig.CORE_ICON_SELECT)
    fun toIconSelectView(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    /**
     * data: String [iconName]
     */
    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_ICON_SELECT)
    suspend fun iconSelectBySuspend(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @HostAndPathAnno(value = AppRouterConfig.CORE_BILL_SEARCH)
    fun toBillSearchView(
        @UiContext context: Context,
        // 只能是同一个账本下面的, 否则不生效
        @ParameterAnno("accountIdList") accountIdList: ArrayList<String> = arrayListOf(),
        // 只能是同一个账本下面的, 否则不生效
        @ParameterAnno("labelIdList") labelIdList: ArrayList<String> = arrayListOf(),
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_BILL_IMAGE_CURD)
    suspend fun billImageCrudBySuspend(
        @UiContext context: Context,
        @ParameterAnno("imageUrlList") imageUrlList: ArrayList<String> = arrayListOf(),
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @HostAndPathAnno(value = AppRouterConfig.CORE_BILL_CURD)
    fun toBillCrudView(
        @UiContext context: Context,
        // 编辑的时候用
        @ParameterAnno("billId") billId: String? = null,
        @ParameterAnno("associatedRefundBillId") associatedRefundBillId: String? = null,
        @ParameterAnno("initBookId") initBookId: String? = null,
        @ParameterAnno("billType") billType: TallyBillDto.Type? = null,
        @ParameterAnno("initCategoryId") initCategoryId: String? = null,
        @ParameterAnno("initAccountId") initAccountId: String? = null,
        @ParameterAnno("initTransferAccountId") initTransferAccountId: String? = null,
        @ParameterAnno("initTransferTargetAccountId") initTransferTargetAccountId: String? = null,
        @ParameterAnno("initLabelIdList") initLabelIdList: ArrayList<String>? = null,
        @ParameterAnno("initImageUrlList") initImageUrlList: ArrayList<String>? = null,
        @ParameterAnno("initTime") initTime: Long? = null,
        @ParameterAnno("initAmount") initAmount: Long? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.CORE_BILL_DETAIL)
    fun toBillDetailView(
        @UiContext context: Context,
        @ParameterAnno("billId") billId: String,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.CORE_BILL_LIST)
    fun toBillListView(
        @UiContext context: Context,
        @ParameterAnno("title") title: StringItemDto? = null,
        @ParameterAnno("question") question: TallyDataSourceSpi.Companion.BillQueryConditionDto? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_PRICE_CALCULATE)
    suspend fun priceCalculateViewSuspend(
        @UiContext context: Context,
        @ParameterAnno("value") value: String? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @HostAndPathAnno(value = AppRouterConfig.CORE_LABEL_INFO)
    fun toLabelInfoView(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.CORE_LABEL_CRUD)
    fun toLabelCrudView(
        @UiContext context: Context,
        @ParameterAnno("labelId") labelId: String? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_LABEL_SELECT)
    suspend fun labelSelectBySuspend(
        @UiContext context: Context,
        @ParameterAnno("bookId") bookId: String? = null,
        @ParameterAnno("idList") idList: ArrayList<String> = arrayListOf(),
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @HostAndPathAnno(value = AppRouterConfig.CORE_CATEGORY_INFO)
    fun toCategoryInfoView(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.CORE_CATEGORY_SUB_INFO)
    fun toCategorySubInfoView(
        @UiContext context: Context,
        @ParameterAnno("parentId") parentId: String,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.CORE_CATEGORY_CRUD)
    fun toCategoryCrudView(
        @UiContext context: Context,
        // 如果有 id 参数, name其他参数都将作废
        @ParameterAnno("id") id: String? = null,
        @ParameterAnno("parentId") parentId: String? = null,
        // 如果 id 或者 parentId 有值, 则不支持
        @ParameterAnno("categoryType") categoryType: TallyCategoryDto.Companion.TallyCategoryType? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_CATEGORY_SELECT)
    suspend fun categorySelectBySuspend(
        @UiContext context: Context,
        @ParameterAnno("bookId") bookId: String? = null,
        @ParameterAnno("categoryId") categoryId: String? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_CATEGORY_SELECT1)
    suspend fun categorySelect1BySuspend(
        @UiContext context: Context,
        @ParameterAnno("bookId") bookId: String,
        @ParameterAnno("categoryIdList") categoryIdList: ArrayList<String> = arrayListOf(),
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @HostAndPathAnno(value = AppRouterConfig.CORE_BOOK_INFO)
    fun toBookInfoView(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.CORE_BOOK_CRUD)
    fun toBookCrudView(
        @UiContext context: Context,
        @ParameterAnno("bookId") bookId: String? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.CORE_BOOK_SWITCH)
    fun toBookSwitch(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_BOOK_SELECT1)
    suspend fun bookSelect1SuspendForResult(
        @UiContext context: Context,
        @ParameterAnno("maxCount") maxCount: Int? = null,
        @ParameterAnno("bookIdList") bookIdList: ArrayList<String> = arrayListOf(),
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @HostAndPathAnno(value = AppRouterConfig.CORE_ACCOUNT_INFO)
    fun toAccountInfoView(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    // 创建和编辑同在
    @HostAndPathAnno(value = AppRouterConfig.CORE_ACCOUNT_DETAIL)
    fun toAccountDetailView(
        @UiContext context: Context,
        @ParameterAnno("accountId") accountId: String? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    // 创建和编辑同在
    @HostAndPathAnno(value = AppRouterConfig.CORE_ACCOUNT_CRUD)
    fun toAccountCrudView(
        @UiContext context: Context,
        @ParameterAnno("accountId") accountId: String? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.CORE_ACCOUNT_ICON_SELECT)
    fun toAccountIconSelectView(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_ACCOUNT_ICON_SELECT)
    suspend fun accountIconSelectSuspendForResult(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_ACCOUNT_SELECT)
    suspend fun accountSelectBySuspend(
        @UiContext context: Context,
        @ParameterAnno("bookId") bookId: String? = null,
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @NavigateAnno(
        forIntent = true,
        resultCodeMatch = Activity.RESULT_OK,
    )
    @RequestCodeAnno
    @HostAndPathAnno(value = AppRouterConfig.CORE_ACCOUNT_SELECT1)
    suspend fun accountSelect1BySuspend(
        @UiContext context: Context,
        @ParameterAnno("bookId") bookId: String,
        @ParameterAnno("accountIdList") accountIdList: ArrayList<String> = arrayListOf(),
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    @HostAndPathAnno(value = AppRouterConfig.CORE_BILL_ALBUM)
    fun toBillAlbumView(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

}

@RouterApiAnno
interface AppRouterUserApi {

    @HostAndPathAnno(value = AppRouterConfig.USER_ABOUT_US)
    fun toAboutUsView(
        @UiContext context: Context,
        @AfterRouteActionAnno action: () -> Unit = {},
    )

}

@RouterApiAnno
interface AppRouterMainApi {

    @HostAndPathAnno(value = AppRouterConfig.MAIN_MAIN)
    fun toMainView(
        @UiContext context: Context,
        @AfterRouteEventActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.MAIN_SETTING)
    fun toSettingView(
        @UiContext context: Context,
        @AfterRouteEventActionAnno action: () -> Unit = {},
    )

    @HostAndPathAnno(value = AppRouterConfig.MAIN_THEME_SELECT)
    fun toThemeSelectView(
        @UiContext context: Context,
        @AfterRouteEventActionAnno action: () -> Unit = {},
    )

}

@RouterApiAnno
interface AppRouterBaseApi {

    /**
     * 返回的字段是 "data": Long
     */
    @RequestCodeAnno
    @NavigateAnno(forIntent = true, resultCodeMatch = Activity.RESULT_OK)
    @HostAndPathAnno(value = AppRouterConfig.BASE_DATE_TIME_SELECT)
    suspend fun dateTimeSelectBySuspend(
        @UiContext context: Context,
        @ParameterAnno("time") time: Long? = null,
        // 默认选择的日期时间到 年-月-日
        @ParameterAnno("type") type: DateTimeType = DateTimeType.Day,
        @ParameterAnno("model") model: DateTimeModel = DateTimeModel.Current,
        @AfterRouteActionAnno action: () -> Unit = {},
    ): Intent

    /**
     * 菜单选择, 这个支持的 item 不可以很多, 用于固定的几个菜单选择会很好
     * 和 ios 的样式比较接近
     */
    @RequestCodeAnno
    @NavigateAnno(forIntent = true, resultCodeMatch = Activity.RESULT_OK)
    @HostAndPathAnno(value = AppRouterConfig.BASE_BOTTOM_MENU_SELECT)
    suspend fun bottomMenuSelectBySuspend(
        @UiContext context: Context,
        @ParameterAnno("data") items: ArrayList<MenuItem>,
    ): Intent

    /**
     * 菜单选择, 是在中间弹出的样式
     */
    @RequestCodeAnno
    @NavigateAnno(forIntent = true, resultCodeMatch = Activity.RESULT_OK)
    @HostAndPathAnno(value = AppRouterConfig.BASE_CENTER_MENU_SELECT)
    suspend fun centerMenuSelectBySuspend(
        @UiContext context: Context,
        @ParameterAnno("data") items: ArrayList<MenuItem>,
    ): Intent

}

@RouterApiAnno
interface AppRouterCustomApi {

    @HostAndPathAnno(value = AppRouterConfig.CUSTOM_SYSTEM_SHARE)
    fun toSystemShareTextView(
        @UiContext context: Context,
        @ParameterAnno("text") text: String,
        @AfterRouteEventActionAnno action: () -> Unit = {},
    )

}

suspend fun String.copyBillToBillCrudView(
    @UiContext context: Context,
    billTime: Long?,
) {
    AppServices.tallyDataSourceSpi.getBillDetailById(
        id = this,
    )?.let { billDetail ->
        val billImageList = billDetail.book?.let { bookInfo ->
            AppServices.tallyDataSourceSpi.getBillImageListByBillId(
                bookId = bookInfo.id,
                billId = billDetail.core.id,
            )
        } ?: emptyList()
        AppRouterCoreApi::class
            .routeApi()
            .toBillCrudView(
                context = context,
                associatedRefundBillId = billDetail.core.originBillId,
                initBookId = billDetail.core.bookId,
                billType = billDetail.core.type,
                initCategoryId = billDetail.core.categoryId,
                initAccountId = when (billDetail.core.type) {
                    TallyBillDto.Type.NORMAL,
                    TallyBillDto.Type.REFUND -> billDetail.core.accountId

                    else -> null
                },
                initTransferAccountId = when (billDetail.core.type) {
                    TallyBillDto.Type.TRANSFER -> billDetail.core.accountId
                    else -> null
                },
                initTransferTargetAccountId = when (billDetail.core.type) {
                    TallyBillDto.Type.TRANSFER -> billDetail.core.transferTargetAccountId
                    else -> null
                },
                initLabelIdList = ArrayList(
                    billDetail.labelList.map { it.id },
                ),
                initImageUrlList = ArrayList(
                    billImageList.mapNotNull { it.url.orNull() },
                ),
                initTime = billTime,
                initAmount = billDetail.core.amount.value,
            )
    }
}

suspend fun AppRouterBaseApi.bottomMenuSelect(
    @UiContext context: Context,
    items: List<MenuItem>,
): Int {
    return ParameterSupport.getInt(
        intent = this.bottomMenuSelectBySuspend(
            context = context,
            items = ArrayList(items),
        ),
        key = "data",
    ) ?: notSupportError()
}

suspend fun AppRouterBaseApi.bottomMenuSelectSimple(
    @UiContext context: Context,
    items: List<StringItemDto>,
): Int {
    return this.bottomMenuSelect(
        context = context,
        items = ArrayList(
            items.map {
                MenuItem(
                    content = it,
                )
            }
        ),
    )
}

suspend fun AppRouterBaseApi.centerMenuSelect(
    @UiContext context: Context,
    items: List<MenuItem>,
): Int {
    return ParameterSupport.getInt(
        intent = this.centerMenuSelectBySuspend(
            context = context,
            items = ArrayList(items),
        ),
        key = "data",
    ) ?: notSupportError()
}

suspend fun AppRouterBaseApi.centerMenuSelectSimple(
    @UiContext context: Context,
    items: List<StringItemDto>,
): Int {
    return this.centerMenuSelect(
        context = context,
        items = ArrayList(
            items.map {
                MenuItem(
                    content = it,
                )
            }
        ),
    )
}

@RouterApiAnno
interface AppRouterSystemApi {

    @HostAndPathAnno(value = CommonRouterConfig.SYSTEM_APP_MARKET)
    fun toSystemAppMarket(
        @UiContext context: Context,
        @AfterRouteEventActionAnno action: () -> Unit = {},
    )

}

@RouterApiAnno
interface AppRouterImagePreviewApi {

    @HostAndPathAnno(value = AppRouterConfig.IMAGE_PREVIEW_MAIN)
    fun toImagePreviewView(
        @UiContext context: Context,
        @ParameterAnno("urlList") urlList: ArrayList<String>,
        // 如果参数超出范围, 会被纠正
        @ParameterAnno("index") index: Int = 0,
        @AfterRouteEventActionAnno action: () -> Unit = {},
    )

}