plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.pilou.bettersony"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.pilou.bettersony"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // Release key lives outside the repo (~/.gradle/gradle.properties); without it builds stay debug-signed.
    val storeFilePath = findProperty("BETTERSONY_STORE_FILE") as String?
    val release = storeFilePath?.let {
        signingConfigs.create("release") {
            storeFile = file(it)
            storePassword = findProperty("BETTERSONY_STORE_PASSWORD") as String
            keyAlias = findProperty("BETTERSONY_KEY_ALIAS") as String
            keyPassword = storePassword
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            release?.let { signingConfig = it }
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = release ?: signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.compose.foundation:foundation:1.9.0")
    implementation("androidx.compose.ui:ui:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.glance:glance-appwidget:1.1.1")
}
