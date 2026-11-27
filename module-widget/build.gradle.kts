plugins {
    id("moduleBusinessPlugin")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.xiaojinzi.tally.module.widget"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    testImplementation(libs.junit)
}