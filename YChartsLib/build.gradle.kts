import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
//    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    compileSdk = 37
    namespace = "co.yml.charts.components"
    defaultConfig {
        minSdk = 24
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_18
        }
    }
    buildFeatures {
        compose = true
    }
}
dependencies {
//    implementation(project(":common"))
//    implementation(project(":room"))
    implementation(libs.googleMapsUtils)
//    implementation(libs.playServicesMaps)
    //implementation(libs.maplibre.sdk)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.jetbrains.material3)
    implementation(libs.androidx.material.android)
    //implementation(libs.androidx.material.android)
    //implementation(libs.androidx.compose.material)
    //implementation(libs.androidx.material)
    implementation(libs.androidx.animation.core.android)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.compose)
}