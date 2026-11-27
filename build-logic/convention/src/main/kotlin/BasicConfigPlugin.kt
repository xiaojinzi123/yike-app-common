import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class BasicConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with<Project, Unit>(project) {
            plugins.apply {
                if (this.findPlugin("com.android.library") == null && this.findPlugin("com.android.application") == null) {
                    throw RuntimeException("You must apply com.android.library or com.android.application plugin first")
                }
                if (this.findPlugin("kotlin-android") == null) {
                    apply("kotlin-android")
                }
                if (this.findPlugin("org.jetbrains.kotlin.android") == null) {
                    apply("org.jetbrains.kotlin.android")
                }
            }

            extensions.findByType(AppExtension::class.java)?.run {
                (this as CommonExtension).basicConfig(project = project)
            }

            extensions.findByType(LibraryExtension::class.java)?.run {
                (this as CommonExtension).basicConfig(project = project)
            }

        }
    }

}