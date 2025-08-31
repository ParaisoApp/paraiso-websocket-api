val ktorVersion  = "2.3.10"

plugins {
    id("paraiso")
    id("io.ktor.plugin") version "2.3.10"
}

dependencies {
    // modules
    implementation(project(":domain"))
    implementation(project(":client"))
    implementation(project(":database"))
    implementation(project(":events"))

    // ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion ")
    implementation("io.ktor:ktor-websockets:$ktorVersion ")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion ")
    implementation("io.ktor:ktor-server-netty:$ktorVersion ")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion ")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion ")
    implementation("io.ktor:ktor-server-cors:$ktorVersion ")

    // db connection
    implementation(platform("org.mongodb:mongodb-driver-bom:5.5.1"))
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine")
    implementation("org.mongodb:bson-kotlinx")

    //redis
    implementation("io.lettuce:lettuce-core:6.2.0.RELEASE")
}

application {
    mainClass.set("com.paraiso.ApplicationKt")
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("server-all")
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })

    manifest {
        attributes(
            "Main-Class" to "com.paraiso.ApplicationKt"
        )
    }
}
