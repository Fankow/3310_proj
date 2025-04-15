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
        minSdk = 32
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = readLocalProperties()
        val mapsApiKey = properties.getProperty("MAPS_API_KEY", "")
        val exchangeRatesApiKey = properties.getProperty("EXCHANGE_RATES_API_KEY", "")
        val serpApiKey = properties.getProperty("SERP_API_KEY", "")
        
        resValue("string", "google_maps_api_key", mapsApiKey)
        buildConfigField("String", "EXCHANGE_RATES_API_KEY", "\"${exchangeRatesApiKey}\"")
        buildConfigField("String", "SERP_API_KEY", "\"${serpApiKey}\"")
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

// helper function to read local.properties safely
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
    implementation(libs.google.firebase.messaging) // for notifications
    implementation(libs.play.services.maps.v1820) // for maps

    implementation(libs.glide)

    implementation(libs.places) // Places API
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.google.code.gson:gson:2.10.1")
    
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // Remove the SerpAPI dependency (com.serpapi:google-search-results-java:2.0.3)
}