// =============================================================================
// 🔥 HADES BACKEND - SIMPLIFIED MAIN APPLICATION
// =============================================================================
// File: src/main/kotlin/com/kairos/hades/HadesApplication.kt
// Purpose: Minimal Spring Boot application that starts successfully

package com.kairos.hades

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * 🔥 Main application class for Hades Backend
 * Simplified version to ensure it starts without issues
 */
@SpringBootApplication
@EnableJpaAuditing
class HadesApplication

fun main(args: Array<String>) {
    runApplication<HadesApplication>(*args)
}
