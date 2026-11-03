import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/** 파일이 있으면 읽고, 없으면 빈 Properties. 비밀값 파일이 없어도 빌드가 되게 한다 (AGENTS.md). */
fun loadProperties(file: File): Properties = Properties().apply {
    if (file.exists()) file.inputStream().use { load(it) }
}

/** BuildConfig 문자열 필드에 넣을 수 있게 따옴표와 역슬래시를 이스케이프한다. */
fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// Pleos Client ID/Secret (TTS/STT용). secrets.properties.example 참고. 없으면 빈 값.
val secrets = loadProperties(rootProject.file("secrets.properties"))

android {
    namespace = "com.meongcare.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.meongcare.app"
        minSdk = 28 // Pleos 보안 요구사항
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "PLEOS_CLIENT_ID", secrets.getProperty("PLEOS_CLIENT_ID", "").asBuildConfigString())
        buildConfigField("String", "PLEOS_CLIENT_SECRET", secrets.getProperty("PLEOS_CLIENT_SECRET", "").asBuildConfigString())
    }

    buildTypes {
        release {
            // Vehicle SDK의 R8 규칙이 확인되기 전까지는 난독화·축소를 끈다.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(platform(libs.kotlinx.coroutines.bom))

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(platform(libs.kotlinx.coroutines.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
}
