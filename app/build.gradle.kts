plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.americanmilestone.texport"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.americanmilestone.texport"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // No AndroidX: the app uses only the Android platform. Tests need a real org.json (android.jar only has stubs).
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
