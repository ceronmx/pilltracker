plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "dev.pilltracker.data"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// Exported schemas feed MigrationTestHelper on every schema bump (ADR 0003).
room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(project(":domain"))
    api(libs.room.runtime) // PillDatabase extends RoomDatabase; :app builds it
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.coroutines.test)
}
