import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    //alias(libs.plugins.kotlin.parcelize)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

// gradle.properties
//val useOpenGLFlag = project.findProperty("useOpenGL")?.toString()?.toBoolean() ?: true
//println("useOpenGL: $useOpenGL")
// In build.gradle.kts
val useOpenGL = project.findProperty("ramani.render.useOpenGL")?.toString()?.toBoolean() ?: true
logger.lifecycle("Ramani Build: Using ${if (useOpenGL) "OpenGL" else "Vulkan"} rendering backend")

android {
    namespace = "com.almica.ramani"
    compileSdk = 37

    // 10jul2026 use maplibre without vulkan (works on Nokia 1)
    // The configurations.all ... stmt resolves duplicate class error in maplibre
    //      org.maplibre.gl:android-sdk-opengl and maplibre plugins
    // org.maplibre.gl:android-sdk uses vulkan

    // Alternatively, to set via command line: ./gradlew assembleDebug -PuseOpenGL=true

    if (useOpenGL) {
        configurations.all {
            exclude(group = "org.maplibre.gl", module = "android-sdk")
        }
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { props.load(it) }
            }
            
            storeFile = file("${rootDir}/keystore/keystore.jks")
            storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.almica.ramani"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        android.buildFeatures.buildConfig = true
        buildConfigField("String", "ORS_API_KEY", "\"5b3ce3597851110001cf624800f86f4d35dd452a923c73be6cd20943\"")
        buildConfigField("boolean", "USE_OPEN_GL", "$useOpenGL")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release") // Link the config here
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

//    kotlin {
//        compilerOptions {
//            jvmTarget = JvmTarget.JVM_11
//        }
//    }

    buildFeatures {
        compose = true
        buildConfig = true
//        viewBinding = true
//        dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.pdf.compose)
    implementation(libs.androidx.pdf.viewer.fragment)
    // To recognize Latin script
    implementation(libs.text.recognition)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.navigation.compose)
    implementation(project(":live-preferences"))
    implementation(project(":room-locations"))
    implementation(project(":composecharts"))
    implementation(project(":YChartsLib"))
    implementation(project(":graphhopper"))
    implementation(project(":gpssatstatus"))
    implementation(project(":googlePlacesSearch"))
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material.android)
    //implementation(libs.material3)
    //implementation(platform(libs.androidx.compose.bom))
    //implementation(libs.androidx.navigation.compose)
    implementation(libs.googleMapsUtils)
    implementation(libs.livedata)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.kotlin.coroutines.play)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)
    if (useOpenGL) {
        implementation(libs.maplibre.sdk)
    } else {
        implementation(libs.maplibre.sdk.vulkan)
    }
    //Nokia 1
    //implementation(libs.maplibre.sdk)
    implementation(libs.maplibre.annotations.plugin)
    implementation(libs.maplibre.scalebar.plugin)
//    implementation(libs.ramani.maplibre)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.places)
    implementation(libs.maps.compose)
    implementation(libs.timber)
//    implementation(libs.androidx.material3.jvmstubs)
     implementation(libs.androidx.jetbrains.material3)
    implementation(libs.androidx.exifinterface)
    implementation(libs.fragment.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.lifecycle.service)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.core)
//    implementation(libs.androidx.material3.android)
    ksp(libs.androidx.room.compiler.v250)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.webkit)
    // coil
    implementation(libs.coil.compose)
}