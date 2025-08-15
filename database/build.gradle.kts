plugins {
    id("paraiso")
}

dependencies {
    implementation(project(":domain"))
    //database
    implementation(platform("org.mongodb:mongodb-driver-bom:5.5.1"))
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine")
    implementation("org.mongodb:bson-kotlinx")
}

application {
    mainClass.set("com.paraiso.Application")
}
