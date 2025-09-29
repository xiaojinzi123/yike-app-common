package com.xiaojinzi.tally.module.core.module.icon_select.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.UiContext
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.getActivity
import com.xiaojinzi.tally.module.base.spi.IconMappingSpi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

@Keep
data class IconSelectItemUseCaseDto(
    val name: String,
    @DrawableRes
    val iconRsd: Int,
    val iconName: String,
)

@Keep
data class IconSelectItemGroupUseCaseDto(
    val name: String,
    val list: List<IconSelectItemUseCaseDto>,
)

sealed class IconSelectIntent {

    data class ItemSelect(
        val item: IconSelectItemUseCaseDto,
    ) : IconSelectIntent()


    data class Submit(
        @UiContext val context: Context,
    ) : IconSelectIntent()

}

@ViewModelLayer
interface IconSelectUseCase : BusinessMVIUseCase {

    /**
     * 搜索的 key
     */
    @StateHotObservable
    val searchKeyStateOb: MutableSharedStateFlow<String>

    /**
     * 图标组列表的状态
     */
    @StateHotObservable
    val iconGroupListStateOb: Flow<List<IconSelectItemGroupUseCaseDto>>

    /**
     * 选择的 Item
     */
    @StateHotObservable
    val itemSelectedStateOb: Flow<IconSelectItemUseCaseDto?>

}

@ViewModelLayer
class IconSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), IconSelectUseCase {

    private val dataList = listOf(
        IconSelectItemGroupUseCaseDto(
            name = "基础",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "相机",
                    iconRsd = IconMappingSpi.IconMapping.Camera1.rsd,
                    iconName = IconMappingSpi.IconMapping.Camera1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "书签1",
                    iconRsd = IconMappingSpi.IconMapping.Bookmark1.rsd,
                    iconName = IconMappingSpi.IconMapping.Bookmark1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "书签2",
                    iconRsd = IconMappingSpi.IconMapping.Bookmark2.rsd,
                    iconName = IconMappingSpi.IconMapping.Bookmark2.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "照片1",
                    iconRsd = IconMappingSpi.IconMapping.Image1.rsd,
                    iconName = IconMappingSpi.IconMapping.Image1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "照片2",
                    iconRsd = IconMappingSpi.IconMapping.Image2.rsd,
                    iconName = IconMappingSpi.IconMapping.Image2.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "标签1",
                    iconRsd = IconMappingSpi.IconMapping.Label1.rsd,
                    iconName = IconMappingSpi.IconMapping.Label1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "标签2",
                    iconRsd = IconMappingSpi.IconMapping.Label2.rsd,
                    iconName = IconMappingSpi.IconMapping.Label2.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "刷新",
                    iconRsd = IconMappingSpi.IconMapping.Refresh1.rsd,
                    iconName = IconMappingSpi.IconMapping.Refresh1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "交通",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "飞机",
                    iconRsd = IconMappingSpi.IconMapping.Airplane1.rsd,
                    iconName = IconMappingSpi.IconMapping.Airplane1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "地铁",
                    iconRsd = IconMappingSpi.IconMapping.Subway1.rsd,
                    iconName = IconMappingSpi.IconMapping.Subway1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "公交车",
                    iconRsd = IconMappingSpi.IconMapping.Bus1.rsd,
                    iconName = IconMappingSpi.IconMapping.Bus1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "汽车/出租车",
                    iconRsd = IconMappingSpi.IconMapping.Taxi1.rsd,
                    iconName = IconMappingSpi.IconMapping.Taxi1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "自行车",
                    iconRsd = IconMappingSpi.IconMapping.Bike1.rsd,
                    iconName = IconMappingSpi.IconMapping.Bike1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "加油站",
                    iconRsd = IconMappingSpi.IconMapping.GasStation1.rsd,
                    iconName = IconMappingSpi.IconMapping.GasStation1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "停车/停车场",
                    iconRsd = IconMappingSpi.IconMapping.Parking1.rsd,
                    iconName = IconMappingSpi.IconMapping.Parking1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "维修保养",
                    iconRsd = IconMappingSpi.IconMapping.Repair1.rsd,
                    iconName = IconMappingSpi.IconMapping.Repair1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "违章",
                    iconRsd = IconMappingSpi.IconMapping.BreakRules1.rsd,
                    iconName = IconMappingSpi.IconMapping.BreakRules1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "电商财产",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "支付宝",
                    iconRsd = IconMappingSpi.IconMapping.Alipay1.rsd,
                    iconName = IconMappingSpi.IconMapping.Alipay1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "微信",
                    iconRsd = IconMappingSpi.IconMapping.Wechat1.rsd,
                    iconName = IconMappingSpi.IconMapping.Wechat1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "中国银行",
                    iconRsd = IconMappingSpi.IconMapping.ZhongguoBank1.rsd,
                    iconName = IconMappingSpi.IconMapping.ZhongguoBank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "建设银行",
                    iconRsd = IconMappingSpi.IconMapping.JiansheBank1.rsd,
                    iconName = IconMappingSpi.IconMapping.JiansheBank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "工商银行",
                    iconRsd = IconMappingSpi.IconMapping.GongshangBank1.rsd,
                    iconName = IconMappingSpi.IconMapping.GongshangBank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "农业银行",
                    iconRsd = IconMappingSpi.IconMapping.NongyeBank1.rsd,
                    iconName = IconMappingSpi.IconMapping.NongyeBank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "交通银行",
                    iconRsd = IconMappingSpi.IconMapping.JiaotongBank1.rsd,
                    iconName = IconMappingSpi.IconMapping.JiaotongBank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "招商银行",
                    iconRsd = IconMappingSpi.IconMapping.ZhaoshangBank1.rsd,
                    iconName = IconMappingSpi.IconMapping.ZhaoshangBank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "上海银行",
                    iconRsd = IconMappingSpi.IconMapping.ShanghaiBank1.rsd,
                    iconName = IconMappingSpi.IconMapping.ShanghaiBank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "江苏银行",
                    iconRsd = IconMappingSpi.IconMapping.JiangsuBank1.rsd,
                    iconName = IconMappingSpi.IconMapping.JiangsuBank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "做生意",
                    iconRsd = IconMappingSpi.IconMapping.Business1.rsd,
                    iconName = IconMappingSpi.IconMapping.Business1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "银行",
                    iconRsd = IconMappingSpi.IconMapping.Bank1.rsd,
                    iconName = IconMappingSpi.IconMapping.Bank1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "银行卡1",
                    iconRsd = IconMappingSpi.IconMapping.BankCard1.rsd,
                    iconName = IconMappingSpi.IconMapping.BankCard1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "银行卡2",
                    iconRsd = IconMappingSpi.IconMapping.BankCard2.rsd,
                    iconName = IconMappingSpi.IconMapping.BankCard2.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "淘宝",
                    iconRsd = IconMappingSpi.IconMapping.Taobao1.rsd,
                    iconName = IconMappingSpi.IconMapping.Taobao1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "京东1",
                    iconRsd = IconMappingSpi.IconMapping.JD1.rsd,
                    iconName = IconMappingSpi.IconMapping.JD1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "京东2",
                    iconRsd = IconMappingSpi.IconMapping.JD2.rsd,
                    iconName = IconMappingSpi.IconMapping.JD2.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "支出",
                    iconRsd = IconMappingSpi.IconMapping.Expenses1.rsd,
                    iconName = IconMappingSpi.IconMapping.Expenses1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "收入",
                    iconRsd = IconMappingSpi.IconMapping.Income1.rsd,
                    iconName = IconMappingSpi.IconMapping.Income1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "钱1",
                    iconRsd = IconMappingSpi.IconMapping.Money1.rsd,
                    iconName = IconMappingSpi.IconMapping.Money1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "美元",
                    iconRsd = IconMappingSpi.IconMapping.Dollar1.rsd,
                    iconName = IconMappingSpi.IconMapping.Dollar1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "优惠券",
                    iconRsd = IconMappingSpi.IconMapping.Coupon1.rsd,
                    iconName = IconMappingSpi.IconMapping.Coupon1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "生活",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "家",
                    iconRsd = IconMappingSpi.IconMapping.Home1.rsd,
                    iconName = IconMappingSpi.IconMapping.Home1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "水电费",
                    iconRsd = IconMappingSpi.IconMapping.WaterElectricityCharge1.rsd,
                    iconName = IconMappingSpi.IconMapping.WaterElectricityCharge1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "旅游",
                    iconRsd = IconMappingSpi.IconMapping.Journey1.rsd,
                    iconName = IconMappingSpi.IconMapping.Journey1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "购物",
                    iconRsd = IconMappingSpi.IconMapping.Shopping1.rsd,
                    iconName = IconMappingSpi.IconMapping.Shopping1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "购物车",
                    iconRsd = IconMappingSpi.IconMapping.ShoppingCart1.rsd,
                    iconName = IconMappingSpi.IconMapping.ShoppingCart1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "Wifi",
                    iconRsd = IconMappingSpi.IconMapping.Wifi1.rsd,
                    iconName = IconMappingSpi.IconMapping.Wifi1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "电话",
                    iconRsd = IconMappingSpi.IconMapping.Telephone1.rsd,
                    iconName = IconMappingSpi.IconMapping.Telephone1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "计算器",
                    iconRsd = IconMappingSpi.IconMapping.Calculator1.rsd,
                    iconName = IconMappingSpi.IconMapping.Calculator1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "充电宝",
                    iconRsd = IconMappingSpi.IconMapping.ChargingTreasure1.rsd,
                    iconName = IconMappingSpi.IconMapping.ChargingTreasure1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "手表",
                    iconRsd = IconMappingSpi.IconMapping.Watch1.rsd,
                    iconName = IconMappingSpi.IconMapping.Watch1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "奶瓶",
                    iconRsd = IconMappingSpi.IconMapping.BabyBottle1.rsd,
                    iconName = IconMappingSpi.IconMapping.BabyBottle1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "菜篮子",
                    iconRsd = IconMappingSpi.IconMapping.VegetableBasket1.rsd,
                    iconName = IconMappingSpi.IconMapping.VegetableBasket1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "面条",
                    iconRsd = IconMappingSpi.IconMapping.Noodles1.rsd,
                    iconName = IconMappingSpi.IconMapping.Noodles1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "西瓜",
                    iconRsd = IconMappingSpi.IconMapping.Watermelon1.rsd,
                    iconName = IconMappingSpi.IconMapping.Watermelon1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "纸巾",
                    iconRsd = IconMappingSpi.IconMapping.Tissue1.rsd,
                    iconName = IconMappingSpi.IconMapping.Tissue1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "厕纸",
                    iconRsd = IconMappingSpi.IconMapping.Tissue2.rsd,
                    iconName = IconMappingSpi.IconMapping.Tissue2.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "美容美发美妆",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "口红",
                    iconRsd = IconMappingSpi.IconMapping.Lipstick1.rsd,
                    iconName = IconMappingSpi.IconMapping.Lipstick1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "美容",
                    iconRsd = IconMappingSpi.IconMapping.Makeups1.rsd,
                    iconName = IconMappingSpi.IconMapping.Makeups1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "化妆刷",
                    iconRsd = IconMappingSpi.IconMapping.Paint1.rsd,
                    iconName = IconMappingSpi.IconMapping.Paint1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "理发剪",
                    iconRsd = IconMappingSpi.IconMapping.BarberClippers1.rsd,
                    iconName = IconMappingSpi.IconMapping.BarberClippers1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "办公文档",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "书籍1",
                    iconRsd = IconMappingSpi.IconMapping.Book1.rsd,
                    iconName = IconMappingSpi.IconMapping.Book1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "书籍2",
                    iconRsd = IconMappingSpi.IconMapping.Book2.rsd,
                    iconName = IconMappingSpi.IconMapping.Book2.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "文件夹",
                    iconRsd = IconMappingSpi.IconMapping.Folder1.rsd,
                    iconName = IconMappingSpi.IconMapping.Folder1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "打印机",
                    iconRsd = IconMappingSpi.IconMapping.Printer1.rsd,
                    iconName = IconMappingSpi.IconMapping.Printer1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "动物",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "狗",
                    iconRsd = IconMappingSpi.IconMapping.Dog1.rsd,
                    iconName = IconMappingSpi.IconMapping.Dog1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "鸟",
                    iconRsd = IconMappingSpi.IconMapping.Bird1.rsd,
                    iconName = IconMappingSpi.IconMapping.Bird1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "牛",
                    iconRsd = IconMappingSpi.IconMapping.Cattle1.rsd,
                    iconName = IconMappingSpi.IconMapping.Cattle1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "海豚",
                    iconRsd = IconMappingSpi.IconMapping.Dolphin1.rsd,
                    iconName = IconMappingSpi.IconMapping.Dolphin1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "鸭子",
                    iconRsd = IconMappingSpi.IconMapping.Duck1.rsd,
                    iconName = IconMappingSpi.IconMapping.Duck1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "大象",
                    iconRsd = IconMappingSpi.IconMapping.Elephant1.rsd,
                    iconName = IconMappingSpi.IconMapping.Elephant1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "鱼",
                    iconRsd = IconMappingSpi.IconMapping.Fish1.rsd,
                    iconName = IconMappingSpi.IconMapping.Fish1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "青蛙",
                    iconRsd = IconMappingSpi.IconMapping.Frog1.rsd,
                    iconName = IconMappingSpi.IconMapping.Frog1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "河马",
                    iconRsd = IconMappingSpi.IconMapping.Hippo1.rsd,
                    iconName = IconMappingSpi.IconMapping.Hippo1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "猴子",
                    iconRsd = IconMappingSpi.IconMapping.Monkey1.rsd,
                    iconName = IconMappingSpi.IconMapping.Monkey1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "熊猫",
                    iconRsd = IconMappingSpi.IconMapping.Panda1.rsd,
                    iconName = IconMappingSpi.IconMapping.Panda1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "鲸鱼",
                    iconRsd = IconMappingSpi.IconMapping.Whale1.rsd,
                    iconName = IconMappingSpi.IconMapping.Whale1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "医疗健康",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "医院",
                    iconRsd = IconMappingSpi.IconMapping.Hospital1.rsd,
                    iconName = IconMappingSpi.IconMapping.Hospital1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "救护车",
                    iconRsd = IconMappingSpi.IconMapping.Ambulance1.rsd,
                    iconName = IconMappingSpi.IconMapping.Ambulance1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "心电",
                    iconRsd = IconMappingSpi.IconMapping.CardioElectric1.rsd,
                    iconName = IconMappingSpi.IconMapping.CardioElectric1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "中药",
                    iconRsd = IconMappingSpi.IconMapping.TraditionalChineseMedicine1.rsd,
                    iconName = IconMappingSpi.IconMapping.TraditionalChineseMedicine1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "打针",
                    iconRsd = IconMappingSpi.IconMapping.Needle1.rsd,
                    iconName = IconMappingSpi.IconMapping.Needle1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "输液",
                    iconRsd = IconMappingSpi.IconMapping.Infusion1.rsd,
                    iconName = IconMappingSpi.IconMapping.Infusion1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "牙齿",
                    iconRsd = IconMappingSpi.IconMapping.Teeth1.rsd,
                    iconName = IconMappingSpi.IconMapping.Teeth1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "品牌",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "Google",
                    iconRsd = IconMappingSpi.IconMapping.Google1.rsd,
                    iconName = IconMappingSpi.IconMapping.Google1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "Github",
                    iconRsd = IconMappingSpi.IconMapping.Github1.rsd,
                    iconName = IconMappingSpi.IconMapping.Github1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "应用商店",
                    iconRsd = IconMappingSpi.IconMapping.AppStore1.rsd,
                    iconName = IconMappingSpi.IconMapping.AppStore1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "苹果",
                    iconRsd = IconMappingSpi.IconMapping.Apple1.rsd,
                    iconName = IconMappingSpi.IconMapping.Apple1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "安卓",
                    iconRsd = IconMappingSpi.IconMapping.Android1.rsd,
                    iconName = IconMappingSpi.IconMapping.Android1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "支付宝",
                    iconRsd = IconMappingSpi.IconMapping.Alipay1.rsd,
                    iconName = IconMappingSpi.IconMapping.Alipay1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "微信",
                    iconRsd = IconMappingSpi.IconMapping.Wechat1.rsd,
                    iconName = IconMappingSpi.IconMapping.Wechat1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "QQ",
                    iconRsd = IconMappingSpi.IconMapping.QQ1.rsd,
                    iconName = IconMappingSpi.IconMapping.QQ1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "银行",
                    iconRsd = IconMappingSpi.IconMapping.Bank1.rsd,
                    iconName = IconMappingSpi.IconMapping.Bank1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "日期时间",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "时钟",
                    iconRsd = IconMappingSpi.IconMapping.Clock1.rsd,
                    iconName = IconMappingSpi.IconMapping.Clock1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "日历",
                    iconRsd = IconMappingSpi.IconMapping.Calendar1.rsd,
                    iconName = IconMappingSpi.IconMapping.Calendar1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "服饰",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "裤子",
                    iconRsd = IconMappingSpi.IconMapping.Pants1.rsd,
                    iconName = IconMappingSpi.IconMapping.Pants1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "眼镜",
                    iconRsd = IconMappingSpi.IconMapping.Glasses1.rsd,
                    iconName = IconMappingSpi.IconMapping.Glasses1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "短裤",
                    iconRsd = IconMappingSpi.IconMapping.PantsShort1.rsd,
                    iconName = IconMappingSpi.IconMapping.PantsShort1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "内裤",
                    iconRsd = IconMappingSpi.IconMapping.Briefs1.rsd,
                    iconName = IconMappingSpi.IconMapping.Briefs1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "拖鞋",
                    iconRsd = IconMappingSpi.IconMapping.Slippers1.rsd,
                    iconName = IconMappingSpi.IconMapping.Slippers1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "袜子",
                    iconRsd = IconMappingSpi.IconMapping.Socks1.rsd,
                    iconName = IconMappingSpi.IconMapping.Socks1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "T恤",
                    iconRsd = IconMappingSpi.IconMapping.TShirt1.rsd,
                    iconName = IconMappingSpi.IconMapping.TShirt1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "多媒体",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "钢琴",
                    iconRsd = IconMappingSpi.IconMapping.Piano1.rsd,
                    iconName = IconMappingSpi.IconMapping.Piano1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "耳机",
                    iconRsd = IconMappingSpi.IconMapping.Headset1.rsd,
                    iconName = IconMappingSpi.IconMapping.Headset1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "音乐",
                    iconRsd = IconMappingSpi.IconMapping.Music1.rsd,
                    iconName = IconMappingSpi.IconMapping.Music1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "电影",
                    iconRsd = IconMappingSpi.IconMapping.Movie1.rsd,
                    iconName = IconMappingSpi.IconMapping.Movie1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "硬件",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "摄像头",
                    iconRsd = IconMappingSpi.IconMapping.MonitorCamera1.rsd,
                    iconName = IconMappingSpi.IconMapping.MonitorCamera1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "手表",
                    iconRsd = IconMappingSpi.IconMapping.Watch1.rsd,
                    iconName = IconMappingSpi.IconMapping.Watch1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "星座",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "水瓶座",
                    iconRsd = IconMappingSpi.IconMapping.Aquarius1.rsd,
                    iconName = IconMappingSpi.IconMapping.Aquarius1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "白羊座",
                    iconRsd = IconMappingSpi.IconMapping.Aries1.rsd,
                    iconName = IconMappingSpi.IconMapping.Aries1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "巨蟹座",
                    iconRsd = IconMappingSpi.IconMapping.Cancer1.rsd,
                    iconName = IconMappingSpi.IconMapping.Cancer1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "摩羯座",
                    iconRsd = IconMappingSpi.IconMapping.Capricornus1.rsd,
                    iconName = IconMappingSpi.IconMapping.Capricornus1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "双子座",
                    iconRsd = IconMappingSpi.IconMapping.Gemini1.rsd,
                    iconName = IconMappingSpi.IconMapping.Gemini1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "狮子座",
                    iconRsd = IconMappingSpi.IconMapping.Leo1.rsd,
                    iconName = IconMappingSpi.IconMapping.Leo1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "天秤座",
                    iconRsd = IconMappingSpi.IconMapping.Libra1.rsd,
                    iconName = IconMappingSpi.IconMapping.Libra1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "双鱼座",
                    iconRsd = IconMappingSpi.IconMapping.Pisces1.rsd,
                    iconName = IconMappingSpi.IconMapping.Pisces1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "射手座",
                    iconRsd = IconMappingSpi.IconMapping.Sagittarius1.rsd,
                    iconName = IconMappingSpi.IconMapping.Sagittarius1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "天蝎座",
                    iconRsd = IconMappingSpi.IconMapping.Scorpio1.rsd,
                    iconName = IconMappingSpi.IconMapping.Scorpio1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "金牛座",
                    iconRsd = IconMappingSpi.IconMapping.Taurus1.rsd,
                    iconName = IconMappingSpi.IconMapping.Taurus1.name,
                ),
                IconSelectItemUseCaseDto(
                    name = "处女座",
                    iconRsd = IconMappingSpi.IconMapping.Virgo1.rsd,
                    iconName = IconMappingSpi.IconMapping.Virgo1.name,
                ),
            )
        ),
        IconSelectItemGroupUseCaseDto(
            name = "其他",
            list = listOf(
                IconSelectItemUseCaseDto(
                    name = "其他",
                    iconRsd = IconMappingSpi.IconMapping.More2.rsd,
                    iconName = IconMappingSpi.IconMapping.More2.name,
                ),
            )
        ),
    )

    override val searchKeyStateOb = MutableSharedStateFlow(
        initValue = "",
    )

    override val iconGroupListStateOb = searchKeyStateOb
        .map { searchKey ->
            if (searchKey.isEmpty()) {
                dataList
            } else {
                dataList
                    .map { itemGroup ->
                        itemGroup.copy(
                            list = itemGroup.list.filter { item ->
                                item.name.contains(other = searchKey)
                            }
                        )
                    }
                    .filter { itemGroup ->
                        itemGroup.list.isNotEmpty()
                    }
            }
        }

    override val itemSelectedStateOb = MutableSharedStateFlow<IconSelectItemUseCaseDto?>(
        initValue = null,
    )

    @IntentProcess
    private suspend fun itemSelect(intent: IconSelectIntent.ItemSelect) {
        itemSelectedStateOb.emit(
            value = intent.item,
        )
    }

    @IntentProcess
    private suspend fun submit(intent: IconSelectIntent.Submit) {
        val itemSelected = itemSelectedStateOb.firstOrNull()
        itemSelected?.let {
            intent
                .context
                .getActivity()
                ?.run {
                    this.setResult(
                        Activity.RESULT_OK,
                        Intent().apply {
                            this.putExtra("data", itemSelected.iconName)
                        },
                    )
                    this.finish()
                }
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}