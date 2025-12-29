val koinVersion: String by project
val kotlinVersion: String by project
val exposedVersion: String by project
val postgresqlVersion: String by project
val hikariVersion: String by project
val slf4jVersion: String by project
val log4jVersion: String by project
val jansiVersion: String by project

plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

group = "com.github.hank9999"
version = "0.0.1"

application {
    mainClass = "com.github.hank9999.ApplicationKt"
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-sse")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-request-validation")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-netty")

    // PostgreSQL + Exposed
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("org.postgresql:postgresql:${postgresqlVersion}")
    implementation("com.zaxxer:HikariCP:${hikariVersion}")

    // 日志
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.fusesource.jansi:jansi:$jansiVersion")
}

tasks.register<JavaExec>("runDecoder") {
    mainClass.set("com.github.hank9999.model.kiro.events.RunDecoderLocal")
    classpath = sourceSets["main"].runtimeClasspath

    standardInput = System.`in`
}