import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun props(name: String) = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use { load(it) }
}

// Release signing comes only from keystore.properties (never committed; see docs/RELEASE.md).
val keystoreProperties = props("keystore.properties")
// Real AdMob ids for release builds (never committed). Debug builds always use Google's test ids.
val admob = props("admob.properties")

// Google's published sample ids: test ads only, earn nothing, safe to click.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testRewarded = "ca-app-pub-3940256099942544/5224354917"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
val testBanner = "ca-app-pub-3940256099942544/9214589741"

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.bahalabs.sortopia"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt()) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.bahalabs.tubejoy"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "ADMOB_REWARDED", quoted(testRewarded))
            buildConfigField("String", "ADMOB_INTERSTITIAL", quoted(testInterstitial))
            buildConfigField("String", "ADMOB_BANNER", quoted(testBanner))
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
            manifestPlaceholders["admobAppId"] = admob.getProperty("appId", testAppId)
            buildConfigField("String", "ADMOB_REWARDED", quoted(admob.getProperty("rewardedUnitId", testRewarded)))
            buildConfigField("String", "ADMOB_INTERSTITIAL", quoted(admob.getProperty("interstitialUnitId", testInterstitial)))
            buildConfigField("String", "ADMOB_BANNER", quoted(admob.getProperty("bannerUnitId", testBanner)))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
    }
}

// The Play bundle must carry real AdMob ids; otherwise players would only see test ads and the
// game would earn nothing. Gate the packaging task itself so no .aab is written first.
val admobConfigured = rootProject.file("admob.properties").exists()
tasks.matching { it.name == "packageReleaseBundle" || it.name == "bundleRelease" }.configureEach {
    val configured = admobConfigured
    doFirst {
        check(configured) {
            "admob.properties is missing: add the real AdMob app id and ad unit ids before building the Play bundle (docs/RELEASE.md)."
        }
    }
}

dependencies {
    implementation(project(":engine"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
