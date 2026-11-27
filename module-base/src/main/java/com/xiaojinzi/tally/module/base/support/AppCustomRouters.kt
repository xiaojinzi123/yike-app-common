package com.xiaojinzi.tally.module.base.support

import android.content.Intent
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.component.impl.RouterRequest
import com.xiaojinzi.component.support.ParameterSupport

@RouterAnno(
    hostAndPath = AppRouterConfig.CUSTOM_SYSTEM_SHARE,
)
fun systemShare(request: RouterRequest): Intent {
    val text = ParameterSupport.getString(
        request.bundle,
        key = "text",
    )
    return Intent.createChooser(
        Intent().apply {
            this.setAction(
                Intent.ACTION_SEND
            )
            this.putExtra(
                Intent.EXTRA_SUBJECT,
                AppServices.appInfoSpi.appName,
            )
            this.putExtra(
                Intent.EXTRA_TEXT,
                text,
            )
            this.setType("text/plain")
        },
        null,
    )
}