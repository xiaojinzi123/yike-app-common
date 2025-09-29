import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.plugin.KaptExtension

open class CompilerConfigPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            plugins.apply {
                apply("kotlin-kapt")
                apply("com.google.devtools.ksp")
                if (this.findPlugin("com.google.dagger.hilt.android") == null) {
                    apply("com.google.dagger.hilt.android")
                }
                if (this.findPlugin("org.jetbrains.kotlin.plugin.compose") == null) {
                    apply("org.jetbrains.kotlin.plugin.compose")
                }
            }
            extensions.configure<KaptExtension> {
                correctErrorTypes = true
            }
            extensions.configure<KspExtension> {
                var tempProject: Project = project
                var name = tempProject.name
                while (tempProject.parent != null) {
                    tempProject = tempProject.parent!!
                    name = tempProject.name + "_" + name
                }
                arg("ModuleName", name)
            }
            dependencies.apply {
                add("ksp", libs.findLibrary("kcomponent_compiler").get().get().toString())
                add("ksp", libs.findLibrary("room_compiler").get().get().toString())
                add("kapt", libs.findLibrary("hilt_compiler").get().get().toString())
            }
        }
    }

}