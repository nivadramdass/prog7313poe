plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)


    id("com.google.gms.google-services")
    id("kotlin-kapt")

}

android {
    namespace = "com.example.budgethero"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.budgethero"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.play.services.auth)


    // Glide for image loading
    implementation(libs.glide)
    kapt(libs.glide.compiler) // or libs.glideCompiler, depending on your chosen key!


    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")




}