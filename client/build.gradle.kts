plugins {
    id("paraiso")
    id("io.ktor.plugin") version "3.0.1"
}

dependencies {
    implementation(project(":domain"))
    // rest client
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
}
