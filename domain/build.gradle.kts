plugins {
    id("paraiso")
    id("io.ktor.plugin") version "2.3.10"
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.mongodb:bson-kotlinx:5.5.1") // BSON support for kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0") // Kotlinx Serialization core
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
}

application {
    mainClass.set("com.paraiso.ApplicationKt")
}
