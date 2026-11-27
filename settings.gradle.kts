pluginManagement {
    includeBuild("./build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./libs.versions.toml"))
        }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内网络优先走阿里云镜像：直连 Maven Central 下载几十 MB 的 jar 会长时间卡住
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "yike-app"

include(":app:opensource")
include(":module-image-picker")
include(":module-user")
include(":module-main")
include(":module-core")
include(":module-datasource")
include(":module-base")
include(":module-image-preview")
include(":module-widget")
include(":module-qianji")
include(":lib-res")

project(":app:opensource").apply {
    this.projectDir = file("../opensource/app")
}
