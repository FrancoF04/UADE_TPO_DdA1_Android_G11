import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

// La URL del backend se lee de un archivo `.env` en la raíz (ignorado por git).
// Ver `.env.example`. Si no existe, se usa el emulador apuntando a localhost.
val envFile = rootProject.file(".env")
val env = Properties().apply {
    if (envFile.exists()) envFile.inputStream().use { load(it) }
}
val apiBaseUrl: String = (env.getProperty("API_BASE_URL") ?: "http://10.0.2.2:3000/api/")
    .trim()
    .let { if (it.endsWith("/")) it else "$it/" }

android {
    namespace = "com.example.androidapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.androidapp"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.security.crypto)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)   // Java usa annotationProcessor (no kapt)
    implementation(libs.okhttp)
    implementation(libs.biometric)
    implementation(libs.glide)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.osmdroid.android)
    // CameraX: captura de video frame a frame para el analizador de QR
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    // ML Kit: reconocimiento de códigos de barras/QR sin servidor externo
    implementation(libs.mlkit.barcode)
}
