plugins {
    id("paraiso")
    id("io.ktor.plugin") version "2.3.10"
}

dependencies {
    // modules
    implementation(project(":domain"))
    implementation(project(":client"))
    implementation(project(":database"))
    // ktor server
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-websockets")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-cors")
    //db connection
    implementation(platform("org.mongodb:mongodb-driver-bom:5.5.1"))
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine")
    implementation("org.mongodb:bson-kotlinx")
}

application {
    mainClass.set("com.paraiso.Application")
}
