package com.xiaojinzi.tally.module.qianji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortPrimaryButton
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.qianji.autorecord.RecognitionLabSession

/** 合成页面；必须由服务采集此页面节点，不能直接创建 RecognizedPayment 或确认页。 */
class QianjiRecognitionLabActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        RecognitionLabSession.begin("com.eg.android.AlipayGphone")
    }
    override fun onPause() {
        RecognitionLabSession.end()
        super.onPause()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { PortTheme {
            var mode by remember { mutableStateOf("未付款") }
            var order by remember { mutableIntStateOf(1) }
            PortPage(title = "识别自检", subtitle = "合成页面 · 不会发生真实交易", onBack = { finish() }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { mode = "成功" }) { Text("成功样例") }
                    TextButton(onClick = { mode = "未付款" }) { Text("未付款样例") }
                    TextButton(onClick = { mode = "歧义" }) { Text("歧义样例") }
                }
                PortCard {
                    Text(if (mode == "未付款") "等待付款" else "支付成功", style = MaterialTheme.typography.headlineSmall)
                    Text("实付金额")
                    Text("12.34", style = MaterialTheme.typography.displaySmall)
                    if (mode == "歧义") {
                        Text("实付金额")
                        Text("56.78")
                    }
                    Text("收款方")
                    Text("自检商户")
                    Text("优惠金额")
                    Text("6.00")
                    Text("余额")
                    Text("8888.00")
                    Text("订单号")
                    Text("SYNTHETIC$order")
                }
                PortPrimaryButton(text = "生成新测试订单", onClick = { order++; RecognitionLabSession.begin("com.eg.android.AlipayGphone") })
                Text("请先开启系统无障碍服务。仅成功且无歧义的样例应产生待确认记录；重复停留不应重复弹出。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } } }
    }
}
