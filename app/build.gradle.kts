plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
fun env(name: String, fallback: String = "") = providers.environmentVariable(name).orElse(fallback).get()
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val releaseAppId = env("ZEROSTAR_APPLICATION_ID", "com.example.zerostar")
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
android {
    namespace = "com.zerostar.game"
    compileSdk = 36
    defaultConfig {
        applicationId = releaseAppId
        minSdk = 26
        targetSdk = 36
        versionCode = env("ZEROSTAR_VERSION_CODE", "1").toInt()
        versionName = env("ZEROSTAR_VERSION_NAME", "0.1.0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PRIVACY_URL", quoted(env("ZEROSTAR_PRIVACY_URL")))
        buildConfigField("String", "SUPPORT_EMAIL", quoted(env("ZEROSTAR_SUPPORT_EMAIL")))
        buildConfigField("String", "ADMOB_APP_ID", quoted(testAppId))
        buildConfigField("String", "ADMOB_BANNER_ID", quoted(testBannerId))
        buildConfigField("String", "AD_AGE_TREATMENT", quoted("UNSPECIFIED"))
        buildConfigField("boolean", "UMP_UNDER_AGE", "false")
        manifestPlaceholders["admobAppId"] = testAppId
    }
    signingConfigs {
        create("upload") {
            val path = env("ZEROSTAR_KEYSTORE")
            if (path.isNotBlank()) storeFile = file(path)
            storePassword = env("ZEROSTAR_STORE_PASSWORD")
            keyAlias = env("ZEROSTAR_KEY_ALIAS")
            keyPassword = env("ZEROSTAR_KEY_PASSWORD")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("upload")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "ADMOB_APP_ID", quoted(env("ZEROSTAR_ADMOB_APP_ID")))
            buildConfigField("String", "ADMOB_BANNER_ID", quoted(env("ZEROSTAR_ADMOB_BANNER_ID")))
            buildConfigField("String", "AD_AGE_TREATMENT", quoted(env("ZEROSTAR_AD_AGE_TREATMENT", "UNSPECIFIED")))
            buildConfigField("boolean", "UMP_UNDER_AGE", env("ZEROSTAR_UMP_UNDER_AGE", "false"))
            manifestPlaceholders["admobAppId"] = env("ZEROSTAR_ADMOB_APP_ID", testAppId)
        }
        create("benchmark") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".benchmark"
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    lint { abortOnError = true; checkReleaseBuilds = true }
    sourceSets.getByName("androidTest").java.srcDir("src/test/java")
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.datastore)
    implementation(libs.serialization)
    implementation(libs.coroutines)
    implementation(libs.ads)
    implementation(libs.ump)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
// Host unit tests need JVM atomic replacement; device tests use the Android implementation.
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.datastore:datastore-core")).using(module("androidx.datastore:datastore-core-jvm:1.2.1"))
        substitute(module("androidx.datastore:datastore-core-android")).using(module("androidx.datastore:datastore-core-jvm:1.2.1"))
    }
}
val verifyReleaseConfiguration = tasks.register("verifyReleaseConfiguration") {
    group = "verification"
    doLast {
        check(releaseAppId != "com.example.zerostar" && releaseAppId.matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+"))) { "Set the owner-approved ZEROSTAR_APPLICATION_ID." }
        listOf("ZEROSTAR_ADMOB_APP_ID", "ZEROSTAR_ADMOB_BANNER_ID").forEach { name ->
            check(env(name).startsWith("ca-app-pub-") && !env(name).contains("3940256099942544")) { "$name must be a production ID." }
        }
        check(env("ZEROSTAR_PRIVACY_URL").startsWith("https://") && !env("ZEROSTAR_PRIVACY_URL").contains("example.")) { "Set the verified public privacy URL." }
        check(env("ZEROSTAR_SUPPORT_EMAIL").contains("@") && !env("ZEROSTAR_SUPPORT_EMAIL").contains("example.")) { "Set the owner-approved support email." }
        check(env("ZEROSTAR_AD_AGE_TREATMENT") in listOf("CHILD", "TEEN", "UNSPECIFIED")) { "Confirm the audience and set ZEROSTAR_AD_AGE_TREATMENT explicitly." }
        check(env("ZEROSTAR_UMP_UNDER_AGE") in listOf("true", "false")) { "Set ZEROSTAR_UMP_UNDER_AGE explicitly." }
        listOf("ZEROSTAR_KEYSTORE", "ZEROSTAR_STORE_PASSWORD", "ZEROSTAR_KEY_ALIAS", "ZEROSTAR_KEY_PASSWORD").forEach { check(env(it).isNotBlank()) { "Missing signing input: $it" } }
        check(file(env("ZEROSTAR_KEYSTORE")).isFile) { "Upload keystore is unavailable." }
    }
}
tasks.configureEach {
    if (name in listOf("packageRelease", "packageReleaseBundle", "signReleaseBundle", "assembleRelease", "bundleRelease")) dependsOn(verifyReleaseConfiguration)
}
