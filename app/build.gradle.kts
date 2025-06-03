plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // Esto habilita KAPT
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10"
}

android {
    namespace = "com.miapp.kairos24h"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.miapp.kairos24h"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    // Compose y UI
    implementation(platform(libs.androidx.compose.bom.v20240500))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.1")
    implementation(libs.androidx.foundation)
    implementation("androidx.compose.material:material:1.6.1")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.v4120)
    implementation(libs.volley)
    implementation(libs.gson)

    // Base de datos y almacenamiento
    implementation(libs.androidx.sqlite.ktx)

    // Imagen y multimedia
    implementation("io.coil-kt:coil:2.4.0")
    implementation(libs.coil.compose.v222)
    implementation(libs.coil.gif)
    implementation(libs.coil.compose)
    implementation(libs.glide)
    kapt(libs.compiler)

    // Responsive layouts
    implementation(libs.androidx.constraintlayout)

    // Servicios de localización
    implementation(libs.play.services.location)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.1")
    debugImplementation(libs.androidx.ui.test.manifest)
}
