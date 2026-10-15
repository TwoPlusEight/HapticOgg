pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // com.arthenica:ffmpeg-kit-* 已随上游停止维护而从 Maven Central 下架，
        // 这里只对 com.arthenica 组回退到仍保留该构件的镜像；其余依赖不受影响。
        maven("https://maven.aliyun.com/repository/public") {
            content { includeGroup("com.arthenica") }
        }
    }
}
rootProject.name = "HapticOgg"
include(":app")
