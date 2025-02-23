plugins {
    `kotlin-dsl`
}


repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.1.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.1.10")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:11.3.1")
}