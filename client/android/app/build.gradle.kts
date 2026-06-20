plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val appVersionName = providers.gradleProperty("APP_VERSION_NAME").orElse("0.1.1")
val appVersionCode = providers.gradleProperty("APP_VERSION_CODE").orElse("1")

android {
    namespace = "com.vincentzyu.androidseenetflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vincentzyu.androidseenetflow"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode.get().toInt()
        versionName = appVersionName.get()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        viewBinding = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
