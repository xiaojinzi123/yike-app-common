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
        google()
        mavenCentral()
        // 添加阿里的镜像 Maven
        maven {
            url = uri("https://maven.aliyun.com/nexus/content/groups/public/")
        }
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
