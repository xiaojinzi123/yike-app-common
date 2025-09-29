package com.xiaojinzi.tally.module.base.module.web.view

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.view.compose.AppbarNormalM3
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun WebView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    BusinessContentView<WebViewModel>(
        needInit = needInit,
    ) { vm ->
        val url by vm.urlStateOb.collectAsState(initial = null)
        url.orNull()?.let { url1 ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .nothing(),
                factory = { context ->
                    WebView(context).apply {
                        this.webChromeClient = object: WebChromeClient() {
                            override fun onReceivedTitle(view: WebView, title: String?) {
                                super.onReceivedTitle(view, title)
                                vm.titleStateOb.value = title
                            }
                        }
                    }
                },
                update = { webView ->
                    webView.loadUrl(
                        url1,
                    )
                },
            )
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun WebViewWrap() {
    val vm: WebViewModel = viewModel()
    val title by vm.titleStateOb.collectAsState(initial = "")
    Scaffold(
        topBar = {
            AppbarNormalM3(
                title = title?.toStringItemDto(),
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .nothing(),
        ) {
            WebView()
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun WebViewPreview() {
    WebView(
        needInit = false,
    )
}