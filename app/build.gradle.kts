plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.pilltracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.pilltracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Release key stays out of git: set these in ~/.gradle/gradle.properties (see README).
    // Without them assembleRelease still builds, unsigned.
    val storeFile = providers.gradleProperty("pilltracker.storeFile").orNull
    signingConfigs {
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = providers.gradleProperty("pilltracker.storePassword").get()
                keyAlias = providers.gradleProperty("pilltracker.keyAlias").get()
                keyPassword = providers.gradleProperty("pilltracker.keyPassword").get()
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }
    // Ship only our locales; no localeConfig, so the app follows the system language (AC5.2).
    androidResources { localeFilters += listOf("en", "es") }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }
    testOptions { unitTests.all { it.useJUnitPlatform() } }
    lint {
        warningsAsErrors = false
        error += "MissingTranslation"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit5)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    debugImplementation(libs.compose.ui.test.manifest)
}
