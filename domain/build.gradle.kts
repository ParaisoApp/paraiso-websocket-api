plugins {
    id("paraiso")
    id("io.ktor.plugin") version "2.3.10"
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json")
}

application {
    mainClass.set("com.paraiso.ApplicationKt")
}
