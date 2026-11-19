import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.madar.player"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.madar.player"
        minSdk = 23
        targetSdk = 36
        versionCode = 28
        versionName = "1.1.28"
    }

    signingConfigs {
        create("release") {
            val key = file("release-key.jks")
            if (key.exists()) {
                storeFile = key
                storePassword = "zaqzouka_release_pass"
                keyAlias = "zaqzouka"
                keyPassword = "zaqzouka_release_pass"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            val releaseKey = file("release-key.jks")
            signingConfig = if (releaseKey.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
    implementation(libs.google.play.services.cast.framework)
}
