import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { this.load(it) }
    }
}

android {
    namespace = "com.example.busbooking"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.busbooking"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val apiBaseUrl = localProperties.getProperty(
            "api.baseUrl",
            localProperties.getProperty("admin.web.baseUrl", "http://10.0.2.2:8081")
        )
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "ADMIN_WEB_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField(
            "String",
            "ADMIN_WEB_BASE_URLS",
            "\"${localProperties.getProperty("admin.web.baseUrls", apiBaseUrl)}\""
        )    }

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

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // ViewModel
    implementation(libs.androidx.viewmodel)
    implementation(libs.androidx.livedata)

    // Material
    implementation(libs.androidx.material)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Bcrypt for password hashing
    implementation(libs.bcrypt)

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}



