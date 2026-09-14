plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Signing details arrive as environment variables from GitHub Actions.
// When they are absent (a local debug build) the release config is skipped.
val ksPath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "uz.abuhafs.genki"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.abuhafs.genki"
        minSdk = 26              // Android 8.0, same as Ruffle requires
        targetSdk = 34
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0"
    }

    signingConfigs {
        if (ksPath != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksPath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // The Ruffle .wasm must not be compressed inside the APK.
    androidResources {
        noCompress += listOf("wasm")
    }
}

dependencies {
    implementation("org.nanohttpd:nanohttpd:2.3.1")
}
