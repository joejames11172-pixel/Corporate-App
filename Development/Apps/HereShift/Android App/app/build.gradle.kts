plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.peostectica.hereshift"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.peostectica.hereshift"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.4.1-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
    // SoundPool.openFd requires sound clips to be stored uncompressed in the APK.
    androidResources { noCompress += "wav" }
}