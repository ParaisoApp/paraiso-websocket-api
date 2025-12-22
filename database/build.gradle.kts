plugins {
    id("paraiso")
    id("io.ktor.plugin") version "3.0.1"
}

dependencies {
    implementation(project(":domain"))
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    // database
    implementation(platform("org.mongodb:mongodb-driver-bom:5.5.1"))
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine")
    implementation("org.mongodb:bson-kotlinx")
}

application {
    mainClass.set("com.paraiso.ApplicationKt")
}
