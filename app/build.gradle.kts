plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id ("kotlin-kapt") // Esto habilita KAPT
}

android {
    namespace = "com.miapp.iDEMO_kairos24hXtragabuche"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.miapp.iDEMO_kairos24hXtragabuche"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true  // Habilitar soporte de Compose
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"  // Versión del compilador de Compose
    }
}

dependencies {
    // Dependencias principales de Compose UI
    implementation(libs.androidx.ui.v178)
    implementation(libs.material3)
    implementation(libs.androidx.foundation.v178)
    implementation(libs.androidx.navigation.compose.v287)

    implementation (libs.play.services.location.v2101)

    // Dependencias de Jetpack Compose
    implementation (libs.androidx.ui.v178)
    implementation (libs.material3)  // Para Material3
    implementation (libs.androidx.material)  // Para Material Design 2
    implementation (libs.androidx.ui.tooling.preview.v178)
    implementation (libs.androidx.lifecycle.runtime.ktx)


    // Material Components (Material Design 2)
    implementation (libs.material) // Verifica que tengas esta versión o superior

    // Material3 (si usas Material Design 3)
    implementation (libs.material3) // Solo si usas Material3

    implementation(libs.androidx.material)
    implementation(libs.androidx.material.icons.extended.v178)
    implementation(libs.androidx.animation)



    implementation(libs.androidx.material.v143)
    implementation(libs.androidx.material.icons.extended)

    // Dependencias adicionales
    implementation(libs.okhttp)
    implementation(libs.play.services.location)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose.v1100)

    // Dependencia del compilador de Compose (requerido en Kotlin 2.0)
    implementation(libs.androidx.compiler)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.volley)
    implementation(libs.androidx.espresso.core)

    // Dependencias de pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling.v178)
    debugImplementation(libs.androidx.ui.tooling.preview.v178)
    androidTestImplementation(libs.ui.test.junit4)

    implementation(libs.okhttp)

    // Dependencias para añadir gif animados
    implementation(libs.coil.compose.v222)
    implementation(libs.coil.gif)

    implementation(libs.coil.compose)

    // Dependencia principal de Glide
    implementation (libs.glide)

    // Glide library
    implementation (libs.glide)
    //noinspection KaptUsageInsteadOfKsp
    kapt (libs.compiler)
}

