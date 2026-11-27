package com.xiaojinzi.tally.module.base.support

import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.component.impl.service.ServiceManager
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.tally.lib.res.LOG_KEYWORDS_TALLY_DATASOURCE
import com.xiaojinzi.tally.module.base.spi.AppConfigSpi
import com.xiaojinzi.tally.module.base.spi.AppInfoSpi
import com.xiaojinzi.tally.module.base.spi.AppWidgetSpi
import com.xiaojinzi.tally.module.base.spi.CoreSpi
import com.xiaojinzi.tally.module.base.spi.IconMappingSpi
import com.xiaojinzi.tally.module.base.spi.ImagePickerSpi
import com.xiaojinzi.tally.module.base.spi.SystemSpi
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceInitSpi

object AppServices {

    const val TAG = "AppServices"

    private var _tallyDataSourceSpi: TallyDataSourceSpi? = null

    val systemSpi: SystemSpi?
        get() = ServiceManager.get(tClass = SystemSpi::class)

    val appConfigSpi: AppConfigSpi
        get() = ServiceManager.requiredGet(tClass = AppConfigSpi::class)

    val appInfoSpi: AppInfoSpi by lazy {
        ServiceManager.requiredGet(tClass = AppInfoSpi::class)
    }

    val iconMappingSpi: IconMappingSpi by lazy {
        ServiceManager.requiredGet(tClass = IconMappingSpi::class)
    }

    val tallyDataSourceInitSpi: TallyDataSourceInitSpi
        get() = ServiceManager.requiredGet(tClass = TallyDataSourceInitSpi::class)

    val tallyDataSourceSpi: TallyDataSourceSpi
        get() {
            // 获取调用此方法的调用链
            LogSupport.d(
                tag = TAG,
                content = "get tallyDataSourceSpi",
                keywords = arrayOf(LOG_KEYWORDS_TALLY_DATASOURCE),
            )
            if (_tallyDataSourceSpi == null) {
                LogSupport.d(
                    tag = TAG,
                    content = "create tallyDataSourceSpi",
                    keywords = arrayOf(LOG_KEYWORDS_TALLY_DATASOURCE),
                )
                val stackTrace = Thread.currentThread().stackTrace
                // 遍历堆栈跟踪并打印调用信息
                /*for (element in stackTrace) {
                    LogSupport.d(
                        tag = TAG,
                        content = "create tallyDataSourceSpi 调用链 \n ClassName: ${element.className}, MethodName: ${element.methodName}",
                        keywords = arrayOf(LOG_KEYWORDS_TALLY_DATASOURCE),
                    )
                }*/
                _tallyDataSourceSpi = ServiceManager.requiredGet(tClass = TallyDataSourceSpi::class)
            }
            return _tallyDataSourceSpi!!
        }

    val imagePickerSpi: ImagePickerSpi?
        get() = ServiceManager.get(tClass = ImagePickerSpi::class)

    val coreSpi: CoreSpi?
        get() = ServiceManager.get(tClass = CoreSpi::class)

    val appWidgetSpi: AppWidgetSpi?
        get() = ServiceManager.get(tClass = AppWidgetSpi::class)

}