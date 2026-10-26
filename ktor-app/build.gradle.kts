import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
    id("io.ktor.plugin") version "2.3.11"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    kotlin("plugin.spring") version "1.9.20"
}

group = "com.sulck.backend"
version = "1.0.0-SNAPSHOT"

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:2.3.11")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.11")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.11")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.11")
    implementation("io.ktor:ktor-server-auth-jvm:2.3.11")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.11")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.11")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.11")

    // Database - Exposed + HikariCP
    implementation("org.jetbrains.exposed:exposed-core:0.49.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.49.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.49.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.flywaydb:flyway-core:10.18.0")

    // DI - Koin
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation("io.insert-koin:koin-ktor:3.5.6")

    // Config
    implementation("com.github.pureconfig:pureconfig-core_2.13:0.17.6")
    implementation("com.github.pureconfig:pureconfig-generic_2.13:0.17.6")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Security
    implementation("org.bouncycastle:bcprov-jdk18on:1.78")
    implementation("com.auth0:java-jwt:4.5.0")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.11")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.20")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.testcontainers:postgresql:1.20.0")
    testImplementation("org.testcontainers:junit-jupiter:1.20.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
    )
}

application {
    mainClass.set("com.sulck.backend.ApplicationKt")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    manifest {
        attributes("Main-Class" to "com.sulck.backend.ApplicationKt")
    }
}