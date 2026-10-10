plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.coroutines.core)
    testFixturesImplementation(libs.coroutines.core)
    testImplementation(libs.junit5)
}

tasks.test { useJUnitPlatform() }
