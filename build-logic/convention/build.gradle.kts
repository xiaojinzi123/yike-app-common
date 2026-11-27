plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    implementation(libs.gradle.kotlin.dsl.plugin)
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin.api)
    implementation(libs.kotlin.gradlePlugin)
    // 依赖 ksp
    implementation(libs.ksp.gradlePlugin)
    // https://mvnrepository.com/artifact/com.google.devtools.ksp/symbol-processing-api
    // runtimeOnly("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.13")
}


gradlePlugin {
    plugins {
        register("basicConfigPlugin") {
            id = "basicConfigPlugin"
            implementationClass = "BasicConfigPlugin"
        }
        register("appPlugin") {
            id = "appPlugin"
            implementationClass = "AppPlugin"
        }
        register("libPlugin") {
            id = "libPlugin"
            implementationClass = "LibPlugin"
        }
        register("modulePlugin") {
            id = "modulePlugin"
            implementationClass = "ModulePlugin"
        }
        register("moduleBusinessPlugin") {
            id = "moduleBusinessPlugin"
            implementationClass = "ModuleBusinessPlugin"
        }
    }
}