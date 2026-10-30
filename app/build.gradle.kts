import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// 설정값 우선순위: 환경변수(CI) > local.properties(로컬 개발) > 기본값
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun config(envKey: String, propKey: String, default: String = ""): String =
    System.getenv(envKey)?.takeIf { it.isNotBlank() } ?: localProps.getProperty(propKey) ?: default

val signingStoreFile = System.getenv("SIGNING_STORE_FILE")

android {
    namespace = "io.github.jennaslee.silverapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.jennaslee.silverapp"
        minSdk = 26
        targetSdk = 35
        versionCode = config("VERSION_CODE", "version.code", "1").toInt()
        versionName = config("VERSION_NAME", "version.name", "0.1.0-dev")

        buildConfigField("String", "API_BASE_URL", "\"${config("API_BASE_URL", "api.baseUrl", "http://10.0.2.2:3000")}\"")
        // PoC용 기기 토큰. 정식 버전에서는 기기 등록 API로 대체한다.
        buildConfigField("String", "DEVICE_TOKEN", "\"${config("DEVICE_TOKEN", "device.token", "dev-token-1")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // CI에서 SIGNING_* 환경변수가 있을 때만 릴리스 서명
        if (signingStoreFile != null) {
            create("release") {
                storeFile = file(signingStoreFile)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (signingStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    // 빌드 JDK는 17 이상이면 무엇이든 OK, 출력 바이트코드만 17로 고정
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
