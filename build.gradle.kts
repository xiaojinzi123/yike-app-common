buildscript {

    dependencies {
        classpath("com.android.tools.build:gradle:${libs.versions.agp}")
        classpath(libs.kcomponent.gradleplugin)
    }

}


// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version libs.versions.agp  apply false
    id("com.android.library") version libs.versions.agp apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin apply false
    id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin apply false
    id("com.google.devtools.ksp") version libs.versions.ksp apply false
    id("com.google.dagger.hilt.android") version libs.versions.hilt apply false
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    configurations.configureEach {
        // Parcelize 运行库已包含旧包名兼容类，避免传递依赖重复打包。
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
    }
}