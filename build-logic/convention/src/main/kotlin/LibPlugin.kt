import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class LibPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            plugins.apply {
                if (this.findPlugin("com.android.library") == null) {
                    apply("com.android.library")
                }
                if (this.findPlugin("kotlin-android") == null) {
                    apply("kotlin-android")
                }
                if (this.findPlugin("kotlin-parcelize") == null) {
                    apply("kotlin-parcelize")
                }
                if (this.findPlugin("org.jetbrains.kotlin.android") == null) {
                    apply("org.jetbrains.kotlin.android")
                }
            }
            extensions.configure<LibraryExtension> {
                defaultConfig {
                    consumerProguardFiles("consumer-rules.pro")
                }
            }
        }
        BasicConfigPlugin().apply(project = project)
        with(project) {
            extensions.configure<LibraryExtension> {
                buildFeatures {
                }
            }
        }
    }

}