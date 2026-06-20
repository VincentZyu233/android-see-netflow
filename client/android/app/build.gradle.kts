plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val appVersionName = providers.gradleProperty("APP_VERSION_NAME").orElse("0.1.2")
val appVersionCode = providers.gradleProperty("APP_VERSION_CODE").orElse("1")

android {
    namespace = "com.vincentzyu.androidseenetflow"
    compileSdk = 34

    signingConfigs {
        create("ciDebugRelease") {
            val storeFileProp = providers.gradleProperty("APP_SIGNING_STORE_FILE").orNull
            val storePasswordProp = providers.gradleProperty("APP_SIGNING_STORE_PASSWORD").orNull
            val keyAliasProp = providers.gradleProperty("APP_SIGNING_KEY_ALIAS").orNull
            val keyPasswordProp = providers.gradleProperty("APP_SIGNING_KEY_PASSWORD").orNull

            if (!storeFileProp.isNullOrBlank()) {
                storeFile = file(storeFileProp)
                storePassword = storePasswordProp ?: "android"
                keyAlias = keyAliasProp ?: "androiddebugkey"
                keyPassword = keyPasswordProp ?: "android"
            }
        }
    }

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
            val storeFileProp = providers.gradleProperty("APP_SIGNING_STORE_FILE").orNull
            if (!storeFileProp.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("ciDebugRelease")
            }
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

    sourceSets {
        getByName("main").jniLibs.srcDirs("build/generated/rustJniLibs/jniLibs")
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
}
