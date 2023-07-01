plugins {
    id("java")
}

group = "tech.thatgravyboat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    implementation("net.fabricmc", "mapping-io", "0.4.2")
    implementation("com.google.code.gson", "gson", "2.10.1")
}