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

val hasProductFolder = file("../product").exists()

include(":app:opensource")
if (hasProductFolder) {
    include(":app:domestic")
    include(":app:abroad")
}
include(":module-qrcode")
include(":module-image-picker")
include(":module-user")
include(":module-main")
include(":module-core")
include(":module-datasource")
include(":module-network:opensource")
if (hasProductFolder) {
    include(":module-network:product")
    // 暂时不支持 opensource 的版本
    include(":module-datasync:product")
}
include(":module-base")
include(":module-image-preview")
include(":module-widget")
include(":module-qianji")
include(":lib-res")

project(":app:opensource").apply {
    this.projectDir = file("../opensource/app")
}
project(":module-network:opensource").apply {
    this.projectDir = file("../opensource/module-network")
}
if (hasProductFolder) {
    project(":app:domestic").apply {
        this.projectDir = file("../product/app/domestic")
    }
    project(":app:abroad").apply {
        this.projectDir = file("../product/app/abroad")
    }
    project(":module-network:product").apply {
        this.projectDir = file("../product/module-network")
    }
    project(":module-datasync:product").apply {
        this.projectDir = file("../product/module-datasync")
    }
}
