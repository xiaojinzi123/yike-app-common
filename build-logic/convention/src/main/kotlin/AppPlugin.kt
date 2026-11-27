import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AppPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            plugins.apply {
                if (this.findPlugin("com.android.application") == null) {
                    apply("com.android.application")
                }
                if (this.findPlugin("com.xiaojinzi.kcomponent.plugin") == null) {
                    apply("com.xiaojinzi.kcomponent.plugin")
                }
                if (this.findPlugin("org.jetbrains.kotlin.plugin.compose") == null) {
                    apply("org.jetbrains.kotlin.plugin.compose")
                }
            }
        }
        BasicConfigPlugin().apply(project = project)
        CompilerConfigPlugin().apply(project = project)
        with(project) {
            extensions.configure<AppExtension> {
                defaultConfig {
                    targetSdk = libs.findVersion("targetSdk").get().toString().toInt()
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }
                buildFeatures.apply {
                    compose = true
                    buildConfig = true
                }
                packagingOptions.apply {
                    resources.merges.add("/META-INF/{AL2.0,LGPL2.1}")
                }
            }
            dependencies {
                add("implementation", libs.findLibrary("hilt").get().get().toString())
                add("implementation", project(":module-base"))
                add("implementation", project(":module-core"))
                add("implementation", project(":module-main"))
                add("implementation", project(":module-widget"))
                add("implementation", project(":module-datasource"))
                add("implementation", project(":module-image-picker"))
                add("implementation", project(":module-image-preview"))
                add("implementation", project(":module-user"))
                add("debugImplementation", "androidx.compose.runtime:runtime-tracing:1.7.3")
            }
        }
    }

}