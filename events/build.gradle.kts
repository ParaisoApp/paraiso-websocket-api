plugins {
    id("paraiso")
    id("io.ktor.plugin") version "2.3.10"
}

dependencies {
    implementation(project(":domain"))
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    //redis
    implementation("io.lettuce:lettuce-core:6.2.0.RELEASE")
}

application {
    mainClass.set("com.paraiso.Application")
}
