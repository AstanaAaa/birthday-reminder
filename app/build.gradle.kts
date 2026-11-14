plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // 应用的包名，手机上靠它区分不同 App
    namespace = "com.xd.birthdayreminder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xd.birthdayreminder"
        // 最低支持 Android 8.0；26 以上才能直接用 java.time 日期 API
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
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
        compose = true
    }

    composeOptions {
        // 必须和 Kotlin 1.9.22 严格配对，否则编译报错
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Kotlin 标准扩展
    implementation("androidx.core:core-ktx:1.12.0")
    // Compose 界面引擎
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
