plugins {
    `kotlin-dsl`
}


repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.9.23")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.23")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:11.3.1")
}