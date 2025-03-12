plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "it.polito.wa2"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
application{
    mainClass= "it.polito.wa2.MainKt"
}
