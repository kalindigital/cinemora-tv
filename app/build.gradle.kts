import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Credenciais ficam em local.properties (fora do controle de versão), não no código.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun secret(name: String): String = localProps.getProperty(name).orEmpty()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "br.com.cinemora.tv"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "br.com.cinemora.tv"
        minSdk = 23
        targetSdk = 35
        versionCode = 31
        versionName = "1.7.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GITHUB_REPO", "\"${secret("GITHUB_REPO")}\"")
    }

    signingConfigs {
        create("release") {
            val store = file(secret("RELEASE_STORE_FILE").ifBlank { "ausente.jks" })
            if (store.exists()) {
                storeFile = store
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Só o build local carrega a chave; quem instalar o APK público configura a sua por QR.
            buildConfigField("String", "OPENAI_API_KEY", "\"${secret("OPENAI_API_KEY")}\"")
            buildConfigField("String", "OPENAI_ORGANIZATION", "\"${secret("OPENAI_ORGANIZATION")}\"")
            buildConfigField("String", "OPENAI_PROJECT", "\"${secret("OPENAI_PROJECT")}\"")
        }
        release {
            buildConfigField("String", "OPENAI_API_KEY", "\"\"")
            buildConfigField("String", "OPENAI_ORGANIZATION", "\"\"")
            buildConfigField("String", "OPENAI_PROJECT", "\"\"")
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        // O detector NullSafeMutableLiveData (lint do androidx.lifecycle) quebra com
        // IncompatibleClassChangeError nesta versão; é bug da ferramenta, não do app.
        disable += "NullSafeMutableLiveData"
    }

    buildFeatures { compose = true; buildConfig = true }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.media3:media3-exoplayer:1.9.3")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.3")
    implementation("androidx.media3:media3-ui:1.9.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.tvprovider:tvprovider:1.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
