import java.util.Properties

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

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
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
            enableV1Signing = false   // minSdk 26，v1(JAR) 已无必要
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.16.0")
    // FFmpegKit provides FLAC decoding, DSP filters, Vorbis encoding and metadata writing.
    // FFmpegKit upstream has been retired; if this artifact is unavailable in your build
    // environment, use a maintained fork or build FFmpegKit from source.
    implementation("com.arthenica:ffmpeg-kit-full-gpl:6.0-2")
}
