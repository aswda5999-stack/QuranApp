plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.quran.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.quran.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "4.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }


    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // أساسيات النظام والأندرويد الحديث
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // مكتبة الوسائط والتحكم بالصوت في الخلفية وشريط الإشعارات
    implementation("androidx.media:media:1.7.0")

    // واجهات Jetpack Compose الحديثة والرسومات والتقليب الأفقي للصفحات
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // مكتبة Coil لتحميل وتخزين صفحات المصحف الحقيقية وصور القراء للعمل بدون إنترنت
    implementation("io.coil-kt:coil-compose:2.7.0")

    // المعالجة المتزامنة للتحميل في الخلفية ونسب التقدم المئوية
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
