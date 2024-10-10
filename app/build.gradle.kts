plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication2"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    //implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    //implementation(libs.androidx.lifecycle.viewmodel.ktx)
    //testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    //per l'utilizzo di firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation (libs.firebase.database)
    //implementation(libs.kotlin.coroutines.core)
    //implementation(libs.kotlin.coroutines.android)
    //per l'autenticazione di firebase
    implementation (libs.firebase.auth)

    implementation(libs.firebase.messaging)
    implementation(libs.work.runtime.ktx)


    //implementation(libs.bcrypt)
    implementation(libs.favre.bcrypt)
    implementation(libs.mindrot.bcrypt)

    implementation(libs.apache.poi)

    implementation(libs.firebase.storage)

    implementation(libs.gson)





}
