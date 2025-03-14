plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.gradleup.shadow") version "8.3.6"
    application
}

group = "it.polito.wa2"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

}

tasks.test {
    useJUnitPlatform()
}

tasks.jar{
    manifest{
        attributes["Main-Class"] = "it.polito.wa2.MainKt"
    }
}

kotlin {
    jvmToolchain(17)

}
application{
    mainClass= "it.polito.wa2.MainKt"
}
