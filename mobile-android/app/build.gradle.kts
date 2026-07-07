import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun configuredApiBaseUrl(defaultValue: String): String =
    providers.gradleProperty("ACHABITATION_API_BASE_URL")
        .orElse(providers.environmentVariable("ACHABITATION_API_BASE_URL"))
        .orElse(defaultValue)
        .get()

fun configuredApiBaseUrlProvider(defaultValue: String) =
    providers.gradleProperty("ACHABITATION_API_BASE_URL")
        .orElse(providers.environmentVariable("ACHABITATION_API_BASE_URL"))
        .orElse(defaultValue)

fun quoted(value: String): String = "\"$value\""

val releaseApiBaseUrlProvider = configuredApiBaseUrlProvider("https://api.achabitation.example/api/v1")
val releaseApiBaseUrl = releaseApiBaseUrlProvider.get()

abstract class CheckReleaseApiBaseUrlTask : DefaultTask() {
    @get:Input
    abstract val apiBaseUrl: Property<String>

    @TaskAction
    fun check() {
        val url = apiBaseUrl.get()
        if (!url.startsWith("https://")) {
            throw GradleException("ACHABITATION_API_BASE_URL doit être en HTTPS pour une release Android.")
        }
        if (url.contains("localhost") || url.contains("10.0.2.2") || url.contains("127.0.0.1")) {
            throw GradleException("ACHABITATION_API_BASE_URL release ne doit pas pointer vers une URL locale/debug.")
        }
    }
}

android {
    namespace = "fr.achabitation.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.achabitation.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEFAULT_API_BASE_URL", quoted(configuredApiBaseUrl("http://10.0.2.2:8080/api/v1")))
        }
        release {
            buildConfigField("String", "DEFAULT_API_BASE_URL", quoted(releaseApiBaseUrl))
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.security:security-crypto:1.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}


tasks.register<CheckReleaseApiBaseUrlTask>("checkReleaseApiBaseUrl") {
    group = "verification"
    description = "Vérifie que l'URL API Android release est HTTPS et non locale."
    apiBaseUrl.set(releaseApiBaseUrlProvider)
}
