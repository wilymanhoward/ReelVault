import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Firebase is optional at build time: without app/google-services.json (gitignored) the app still builds
// and runs fully offline; sign-in and sync just report that they aren't set up. See the README.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Share links live on Firebase Hosting at https://<project-id>.web.app/c/<id>; the project id comes from
// google-services.json so forks get their own domain. Without a config the placeholder host never matches.
val shareHost: String = file("google-services.json").takeIf { it.exists() }?.let { json ->
    val info = (groovy.json.JsonSlurper().parse(json) as Map<*, *>)["project_info"] as Map<*, *>
    "${info["project_id"]}.web.app"
} ?: "share.invalid"

// Personal settings that must not be committed (local.properties is gitignored).
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "io.github.wilymanhoward.reelvault"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.wilymanhoward.reelvault"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // Optional Meta app token ("appId|clientToken") for Instagram/Facebook oEmbed.
        // Empty = Open Graph scraping only.
        buildConfigField("String", "SHARE_HOST", "\"$shareHost\"")
        manifestPlaceholders["shareHost"] = shareHost
        buildConfigField("String", "META_OEMBED_TOKEN","\"${localProperties.getProperty("meta.oembedToken", "").trim()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric needs merged resources and the manifest for Room/Context-based tests.
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // Robolectric's native SQLite loader can't open its cached jars when the home path has a
            // space (it reads "%20" literally), so give the test JVM a space-free home for its cache.
            if (' ' in System.getProperty("user.home")) {
                val home = rootProject.file(".gradle/robolectric-home").apply { mkdirs() }
                test.systemProperty("user.home", home.absolutePath)
            }
        }
    }
}

room {
    // Exported schemas are committed so future migrations can be written and tested against them.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.browser)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
}
