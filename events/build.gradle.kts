plugins {
    id("paraiso")
    id("io.ktor.plugin") version "3.0.1"
}

dependencies {
    implementation(project(":domain"))
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    // redis
    implementation("io.lettuce:lettuce-core:6.8.0.RELEASE")
}

application {
    mainClass.set("com.paraiso.ApplicationKt")
}
