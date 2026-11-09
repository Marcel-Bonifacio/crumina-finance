plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ksPath: String? = System.getenv("ANDROID_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

android {
    namespace = "id.tirtawijata.crumina"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.tirtawijata.crumina"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.1.1"
        buildConfigField("String", "API_BASE", "\"https://crumina.tirtawijata.com\"")
    }

    signingConfigs {
        if (ksPath != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (ksPath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        resources {
            // Keep third-party licence and notice files in the APK instead of dropping them.
            // AL2.0 and LGPL2.1 are identical texts shipped by several libraries, so one copy is kept.
            // Other licence and notice files that share a path (AGP excludes these by default)
            // are concatenated, so no library's notice is lost. Non-legal duplicates such as
            // MANIFEST.MF, signature files and maven metadata stay excluded by AGP's defaults.
            val notices = setOf(
                "/META-INF/LICENSE", "/META-INF/LICENSE.txt", "/META-INF/LICENSE.md",
                "/META-INF/NOTICE", "/META-INF/NOTICE.txt", "/META-INF/NOTICE.md",
                "/LICENSE", "/LICENSE.txt", "/NOTICE", "/NOTICE.txt"
            )
            excludes -= notices
            merges += notices
            pickFirsts += setOf("/META-INF/AL2.0", "/META-INF/LGPL2.1")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
