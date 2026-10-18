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
        versionCode = 8
        versionName = "0.5.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
        }
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
dependencies {
    testImplementation("junit:junit:4.13.2")
}
