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

// release 서명 정보. keystore.properties.example 참고. 없으면 서명하지 않은 release APK가 나온다.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseKeystore = keystorePropertiesFile.exists()

android {
    namespace = "com.meongcare.app"
    compileSdk = 35

    // AAOS Car API (CarUxRestrictionsManager)를 컴파일할 때만 쓴다. 실행할 때는 차량 기기에 있는 것을 쓴다.
    // 근거: Google parked/automotive-os 문서 (docs/pleos/sdk-notes.md). 일반 폰에서는 부르지 않는다.
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.meongcare.app"
        minSdk = 28 // Pleos 보안 요구사항
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "PLEOS_CLIENT_ID", secrets.getProperty("PLEOS_CLIENT_ID", "").asBuildConfigString())
        buildConfigField("String", "PLEOS_CLIENT_SECRET", secrets.getProperty("PLEOS_CLIENT_SECRET", "").asBuildConfigString())

        // Firebase (012, 제휴 병원 목록·카드 전송). 없으면 빈 값 → 앱은 내장 시연 병원으로 돈다 (data/server/ServerConfig.kt).
        buildConfigField("String", "FIREBASE_PROJECT_ID", secrets.getProperty("FIREBASE_PROJECT_ID", "").asBuildConfigString())
        buildConfigField("String", "FIREBASE_API_KEY", secrets.getProperty("FIREBASE_API_KEY", "").asBuildConfigString())
        buildConfigField("String", "FIREBASE_EMULATOR_HOST", secrets.getProperty("FIREBASE_EMULATOR_HOST", "").asBuildConfigString())
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            val keystore = loadProperties(keystorePropertiesFile)
            create("release") {
                // storeFile은 app 모듈 기준 경로다. 예시 값 ../release.jks는 저장소 루트를 가리킨다.
                storeFile = file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            // release는 PC의 Firebase 에뮬레이터(평문 HTTP)에 붙지 않는다. ServerConfig도 무시하지만 한 번 더 막는다 (012).
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"\"")
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
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navihelper)
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
    testImplementation(libs.org.json)
}

// 단위 테스트가 docs/data/의 예시 JSON을 읽는다 (012). 파일 위치를 넘기고, 바뀌면 테스트를 다시 돌리게 입력으로 둔다.
tasks.withType<Test>().configureEach {
    val docsData = rootProject.file("docs/data")
    inputs.dir(docsData).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("docsData")
    systemProperty("meongcare.docsData", docsData.absolutePath)
}
