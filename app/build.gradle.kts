plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace   = "eu.kanade.tachiyomi.animeextension.en.animepahe"
    compileSdk  = 34

    defaultConfig {
        applicationId  = "eu.kanade.tachiyomi.animeextension.en.animepahe"
        minSdk         = 24
        targetSdk      = 34
        versionCode    = 1
        versionName    = "1.0"
        manifestPlaceholders["tachiyomi.animeextension.class"] = "en.animepahe.AnimePahe"
        manifestPlaceholders["tachiyomi.animeextension.name"]  = "AnimePahe"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
}
