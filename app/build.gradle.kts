import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Load API keys from secrets.properties (gitignored). Never hardcode keys in source.
val secrets = Properties().apply {
    val f = rootProject.file("secrets.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Load release signing config from keystore.properties (gitignored).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.example.chineselock"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.han.teumsae"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // For production, route through your own proxy instead of embedding the key.
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${secrets.getProperty("OPENAI_API_KEY", "")}\""
        )
        // Gemini(무료 티어) 키 — OCR 텍스트 구조화에 사용. Google AI Studio에서 발급.
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${secrets.getProperty("GEMINI_API_KEY", "")}\""
        )
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.containsKey("storeFile")) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.containsKey("storeFile")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // ML Kit OCR (Chinese)
    implementation(libs.mlkit.text.chinese)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Misc
    implementation(libs.coil.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // AdMob (수익화) — 현재 테스트 광고ID 사용. 출시 시 실제 AdMob 앱ID/광고ID로 교체.
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    // play-services-ads가 CameraX의 ListenableFuture(guava listenablefuture stub)와 충돌 →
    // 전체 guava(android)를 명시해 컴파일 클래스패스에 ListenableFuture를 확보.
    implementation("com.google.guava:guava:33.3.1-android")
}
