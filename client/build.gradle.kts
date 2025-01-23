plugins {
    id("base-websocket-api")
    id("io.ktor.plugin") version "2.3.10"
}

dependencies {
    implementation("io.ktor:ktor-client-websockets")
    implementation("io.ktor:ktor-client-cio")
}

application {
    mainClass.set("com.example.ApplicationKt")
}
