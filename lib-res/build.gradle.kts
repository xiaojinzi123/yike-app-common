plugins {
    id("libPlugin")
}

android {
    namespace = "com.xiaojinzi.tally.lib.res"
}

dependencies {
    // androidx 的 annotation
    api(libs.kotlin.parcelize.runtime)
    api(libs.androidx.annotation)
    api(libs.xiaojinzi.android.support.annotation)
    api(libs.xiaojinzi.android.support.bean)
    api(libs.xiaojinzi.android.support.ktx)
    api(libs.compose.runtime)
    api(libs.compose.runtime.android)
    api(libs.compose.ui.android)
    api(libs.compose.foundation.android)
    api(libs.compose.foundation.layout.android)
    api(libs.compose.material.android)
    api(libs.compose.material.icons.extended)
    api(libs.compose.material3)
}