package com.xiaojinzi.tally.module.qianji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDivider
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.base.theme.PortWidgetPicker

/** 旧 tools 路由的兼容入口；主流程从首页/设置直达各独立页面。 */
@RouterAnno(hostAndPath = AppRouterConfig.QIANJI_TOOLS)
class QianjiToolsActivity : ComponentActivity() {

    private var status by mutableStateOf<String?>(null)
    private var statusIsError by mutableStateOf(false)
    private var showWidgetPicker by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                PortTheme {
                    PortPage(title = "钱迹工具", subtitle = "记账与数据管理的快捷入口", onBack = { finish() }) {
                        PortSectionTitle("记账与数据")
                        PortCard(contentPadding = PaddingValues(0.dp)) {
                            PortActionRow("自动记账", subtitle = "管理识别与记账设置", onClick = { navigate(AppRouterConfig.QIANJI_AUTO_RECORD) })
                            PortDivider()
                            PortActionRow("导入账单", subtitle = "选择账本，导入 CSV / XLS / 钱迹 DB", onClick = { navigate(AppRouterConfig.QIANJI_IMPORT) })
                            PortDivider()
                            PortActionRow("预算", subtitle = "查看月度支出与分类额度", onClick = { navigate(AppRouterConfig.QIANJI_BUDGET) })
                        }
                        PortSectionTitle("桌面")
                        PortCard(contentPadding = PaddingValues(0.dp)) {
                            PortActionRow("桌面小部件", subtitle = "预览并添加收支、预算小部件", onClick = { showWidgetPicker = true })
                        }
                        status?.let { TransferStatus(title = if (statusIsError) "暂时无法完成" else "提示", message = it, error = statusIsError) }
                    }
                    if (showWidgetPicker) {
                        PortWidgetPicker(onDismiss = { showWidgetPicker = false })
                    }
                }
            }
        }
    }

    private fun navigate(path: String) {
        status = null
        try {
            Router.with(context = this).hostAndPath(hostAndPath = path).forward()
        } catch (_: Exception) {
            statusIsError = true
            status = "页面暂时无法打开，请返回后重试。"
        }
    }

}
