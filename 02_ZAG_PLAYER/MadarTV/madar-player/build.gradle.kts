import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = project.file("keystore.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use { load(it) }
    }
}
val releaseStoreFile = releaseSigningProperties.getProperty("storeFile")?.let { project.file(it) }
val releaseStorePassword = releaseSigningProperties.getProperty("storePassword")
val releaseKeyAlias = releaseSigningProperties.getProperty("keyAlias")
val releaseKeyPassword = releaseSigningProperties.getProperty("keyPassword")
val releaseSigningConfigured = releaseStoreFile?.isFile == true &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.madar.player"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.madar.player"
        minSdk = 24
        targetSdk = 36
        versionCode = 37
        versionName = "1.1.37"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = requireNotNull(releaseStoreFile)
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        checkDependencies = false
        disable += "InvalidFragmentVersionForActivityResult"
    }
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

tasks.configureEach {
    if (name in setOf("validateSigningRelease", "signReleaseBundle", "bundleRelease", "assembleRelease")) {
        doFirst {
            check(releaseSigningConfigured) {
                "Release signing is not configured. Add madar-player/keystore.properties and the matching release key."
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.okhttp)
    implementation(libs.google.play.services.cast.framework)
    // StartApp SDK removed
}
