plugins {
    id("modulePlugin")
}

android {
    namespace = "com.xiaojinzi.tally.module.base"
}

dependencies {

    api(project(":lib-res"))
    api(libs.xiaojinzi.android.support.init)
    api(libs.xiaojinzi.android.support.activitystack)
    // 保留本地存储、媒体及图片能力，裁掉外部 module-base 传递的接口与下载库。
    api(libs.xiaojinzi.android.module.storage) {
        exclude(group = "com.github.xiaojinzi123.AndroidSupport", module = "lib-ktx-retrofit")
        exclude(group = "com.github.xiaojinzi123.AndroidSupport", module = "lib-download")
        exclude(group = "com.squareup.retrofit2")
        // 旧 UI 预览依赖与当前 Compose Runtime 的类和命名空间冲突。
        exclude(group = "androidx.ui")
        exclude(group = "androidx.compose", module = "compose-runtime")
    }
    api(libs.xiaojinzi.android.module.ffmpeg) {
        exclude(group = "com.github.xiaojinzi123.AndroidSupport", module = "lib-ktx-retrofit")
        exclude(group = "com.github.xiaojinzi123.AndroidSupport", module = "lib-download")
        exclude(group = "com.squareup.retrofit2")
        // 旧 UI 预览依赖与当前 Compose Runtime 的类和命名空间冲突。
        exclude(group = "androidx.ui")
        exclude(group = "androidx.compose", module = "compose-runtime")
    }
    api(libs.xiaojinzi.android.reactive.core)
    api(libs.xiaojinzi.android.reactive.template)
    api(libs.xiaojinzi.android.reactive.template.compose)

    api(libs.kcomponent.core)

    api(libs.androidx.work.manager.ktx)

    api(libs.lottie.compose)

    api("com.github.HaoZai000.NexioUI:nexioui:0.1.1")

    api(libs.compose.runtime)
    api(libs.compose.runtime.android)
    api(libs.compose.ui.android)
    api(libs.compose.foundation.android)
    api(libs.compose.foundation.layout.android)
    api(libs.compose.material.android)
    api(libs.compose.material3)

    api(libs.glance.appwidget)
    api(libs.glance.material3)

    // api(libs.koin.android)
    // implementation(libs.hilt)

}