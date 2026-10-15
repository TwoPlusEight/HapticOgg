import java.util.Properties

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
// 没有密钥时不配置签名（产出未签名包），而不是在配置阶段就崩掉。
val releaseKeystoreFile: String = keystoreProps.getProperty("storeFile") ?: ""
val hasReleaseKey: Boolean = releaseKeystoreFile.isNotEmpty()

plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.hapticogg"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hapticogg"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // AGP 9 compiles Kotlin itself; kotlin.compilerOptions.jvmTarget defaults to
    // compileOptions.targetCompatibility (17), so no kotlinOptions{} block is needed.
    signingConfigs {
        create("release") {
            if (hasReleaseKey) {
                storeFile = rootProject.file(releaseKeystoreFile)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
            enableV1Signing = false   // minSdk 26，v1(JAR) 已无必要
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // 瘦身版：R8 压缩代码 + 剔除无用资源，只配给 arm64 出包。
        create("slim") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 每个 ABI 各出一个 APK，另外再出一个包含全部 ABI 的 universal 包。
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.16.0")
    // 只做音频转换，所以用 audio 包：带 libvorbis（编码 OGG）、lame、opus 等音频库，
    // 以及 FFmpeg 自带的全部音频解码器（mp3/flac/aac/wav/opus/alac/amr…），
    // 但不含视频库，native 体积比 full-gpl 小约 35%。需要视频时换回 full-gpl 即可。
    // FFmpegKit upstream has been retired; if this artifact is unavailable in your build
    // environment, use a maintained fork or build FFmpegKit from source.
    implementation("com.arthenica:ffmpeg-kit-audio:6.0-2")
}
