import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Firebase (push) is only wired in when app/google-services.json exists, so the project builds without it.
// Put the file at: FlameInFreefall/app/google-services.json  (the Android app in Firebase must use this applicationId).
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

// Backend settings — ONE place. Override in local.properties (never committed) or with -PflameBaseUrl=...
//   flame.baseUrl=https://xxxx.trycloudflare.com/     (the current tunnel is temporary and changes when it restarts)
//   flame.useFake=true                                 (offline demo: every repository returns mock data)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val defaultBaseUrl = "https://gap-concerned-sorted-pending.trycloudflare.com/"
val baseUrl: String = (localProps.getProperty("flame.baseUrl") ?: (project.findProperty("flameBaseUrl") as String?) ?: defaultBaseUrl)
    .trim().let { if (it.endsWith("/")) it else "$it/" }
val useFake: Boolean = (localProps.getProperty("flame.useFake") ?: (project.findProperty("flameUseFake") as String?) ?: "false").trim().toBoolean()

android {
    namespace = "com.flame.astronaut"
    compileSdk = 35

    defaultConfig {
        // Must match the package registered in Firebase (google-services.json). That app was registered as
        // "com.flame.astronuat" (typo). To use the correct id, re-add the Android app in Firebase and swap the json.
        applicationId = "com.flame.astronuat"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        buildConfigField("boolean", "USE_FAKE", useFake.toString())
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
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.04.00")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // Session storage (token is AES-GCM encrypted with an Android Keystore key before it is written)
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // Push notifications (FCM)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}
