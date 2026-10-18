plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val releaseStorePath = System.getenv("TAPVOYAGE_RELEASE_KEYSTORE_FILE")
    ?: throw GradleException("FAIL CLOSED: Tap Voyage approved release keystore not configured")
val releaseStorePass = System.getenv("TAPVOYAGE_RELEASE_STORE_PASSWORD")
    ?: throw GradleException("FAIL CLOSED: Tap Voyage release store secret missing")
val releaseAlias = System.getenv("TAPVOYAGE_RELEASE_KEY_ALIAS")
    ?: throw GradleException("FAIL CLOSED: Tap Voyage release alias missing")
val releaseKeyPass = System.getenv("TAPVOYAGE_RELEASE_KEY_PASSWORD")
    ?: throw GradleException("FAIL CLOSED: Tap Voyage release key secret missing")
android {
    namespace = "com.peostectica.tapvoyage"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.peostectica.tapvoyage"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("production") {
            storeFile = file(releaseStorePath)
            storePassword = releaseStorePass
            keyAlias = releaseAlias
            keyPassword = releaseKeyPass
        }
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("production")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += "wav" }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
}

val generateApprovedAudio by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    environment("TAPVOYAGE_AUDIO_OUT", file("src/main/assets/audio").absolutePath)
    commandLine("python3", "qa/generate_audio_assets.py")
}
tasks.named("preBuild").configure { dependsOn(generateApprovedAudio) }
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }
    .configureEach { dependsOn(generateApprovedAudio) }
