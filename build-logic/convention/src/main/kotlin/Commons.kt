import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun CommonExtension<*, BuildType, *, *, *, *>.basicConfig(project: Project) {
    compileSdk = project.libs.findVersion("compileSdk").get().toString().toInt()
    defaultConfig {
        minSdk = project.libs.findVersion("minSdk").get().toString().toInt()
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (
                (project.findProperty("support.isIncludeKspFolder")
                    ?.toString()
                    ?: "false").toBoolean()
            ) {
                sourceSets {
                    this.getAt("debug").kotlin.srcDirs("./build/generated/ksp/debug/kotlin")
                }
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    project.tasks.withType(KotlinCompile::class.java) {
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

/**
 * 不管版本号是几位的. 这个都是补充到 四位
 * 比如：1.0
 * 实际上会补充为：1.0.0.0
 * 1.2.3.4 -> 1020304
 * 1.17.3.11 -> 1170311
 */
fun String.toVersionCode(): Int {
    val numberList = this
        .split(".")
        .map {
            it.toInt()
        }
        .toMutableList()
    (0 until (4 - numberList.size)).forEach { _ ->
        numberList.add(0)
    }
    return numberList.reduce { acc, item -> acc * 100 + item }
}