plugins {
    id("paraiso")
    id("io.ktor.plugin") version "2.3.10"
}

dependencies {
    // modules
    implementation(project(":domain"))
    implementation(project(":client"))
    // ktor server
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-websockets")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-cors")
    // xss filter
    implementation("org.jsoup:jsoup:1.17.2")
}

application {
    mainClass.set("com.paraiso.Application")
}
