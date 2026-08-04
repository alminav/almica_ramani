// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
//    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.3"
    id("idea") // Ensure the IDEA plugin is applied
}
idea {
    module {
        excludeDirs.add(file("app/src/main/assets"))
        excludeDirs.add(file(".artifacts"))
        excludeDirs.add(file(".kotlin"))
        excludeDirs.add(file(".idea"))
        excludeDirs.add(file(".gradle"))
        excludeDirs.add(file("backups"))
        excludeDirs.add(file("pictures"))
        excludeDirs.add(file("app/release"))
    }
}