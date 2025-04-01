import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "edu.cuhk.a3310_final_proj"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.cuhk.a3310_final_proj"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Use a safer approach to read the API key
        val properties = readLocalProperties()
        val mapsApiKey = properties.getProperty("MAPS_API_KEY", "")

        // Create a string resource with the API key
        resValue("string", "google_maps_api_key", mapsApiKey)
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

// Helper function to read local.properties safely
fun readLocalProperties(): Properties {
    val properties = Properties()
    val localPropertiesFile = File(rootDir, "local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { properties.load(it) }
    }
    return properties
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.appcheck.debug)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.com.google.firebase.firebase.analytics)
    implementation(libs.google.firebase.auth)
    implementation(libs.google.firebase.firestore)
    implementation(libs.google.firebase.storage)

    // Optional: add these if needed
    implementation(libs.google.firebase.messaging) // for notifications
    implementation(libs.play.services.maps.v1820) // for maps

    // Image loading library (recommended for display of Firebase Storage images)
    implementation(libs.glide)

    implementation(libs.places) // Places API
    implementation("com.google.android.gms:play-services-maps:19.0.0")
}