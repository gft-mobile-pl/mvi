plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs)
    id("kotlin-parcelize")
}

android {
    namespace = "com.gft.example.mvi.xml"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.gft.example.mvi.xml"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = Java.sourceCompatibility
        targetCompatibility = Java.targetCompatibility
    }
    kotlinOptions {
        jvmTarget = Java.jvmTarget
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":mvi-fragment-ktx"))

    //core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.ktx)

    //views
    implementation(libs.material)
    implementation(libs.androidx.constraint.layout)

    //navigation
    implementation(libs.androidx.navigation.fragment)

    //koin
    implementation(libs.koin.android)

    //tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.test)
    androidTestImplementation(libs.espresso.core)
}
