// =============================================================================
// 🔥 HADES PROJECT - JAVA 17 COMPATIBLE BUILD CONFIGURATION
// =============================================================================
// File: build.gradle.kts (Updated for Java 17 compatibility)

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.5"  // Compatible with Java 17
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.8.22"  // Compatible with Java 17
    kotlin("plugin.spring") version "1.8.22"
    kotlin("plugin.jpa") version "1.8.22"
}

group = "com.kairos"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17  // Changed to Java 17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Core Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Kotlin Support
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Database
    implementation("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    
    // Development Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"  // Changed to Java 17
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
