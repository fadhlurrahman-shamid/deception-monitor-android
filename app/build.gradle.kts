import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Deployment secrets live in local.properties (git-ignored), never in source.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProperty(key: String, fallback: String): String =
    localProperties.getProperty(key)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "com.example.amanahstaffapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.amanahstaffapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "2.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiBaseUrl = localProperty("decoy.apiBaseUrl", "http://10.0.2.2/deception-monitor/")
        val apiKey = localProperty("decoy.apiKey", "CHANGE_ME_BEFORE_DEPLOYMENT")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "DECOY_KEY", "\"$apiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}