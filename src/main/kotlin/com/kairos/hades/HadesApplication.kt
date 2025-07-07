// =============================================================================
// File: src/main/kotlin/com/kairos/hades/HadesApplication.kt
// 🔥 HADES BACKEND - MAIN APPLICATION CLASS
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// Purpose: Main Spring Boot application entry point (MISSING FROM CURRENT REPO)
// =============================================================================

package com.kairos.hades

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * Main Spring Boot application class for Hades
 * The powerful marketing decisioning backend engine
 */
@SpringBootApplication(
    scanBasePackages = ["com.kairos.hades"]
)
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = ["com.kairos.hades.config"])
class HadesApplication

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    // Set system properties for better performance
    System.setProperty("spring.jmx.enabled", "false")
    System.setProperty("spring.main.lazy-initialization", "false")
    System.setProperty("logging.pattern.console", "%clr(%d{HH:mm:ss.SSS}){faint} %clr(%5p) %clr([%X{tenantId:-SYSTEM}]){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n")
    
    // Print startup banner
    printStartupBanner()
    
    // Run the application
    runApplication<HadesApplication>(*args)
}

/**
 * Print custom startup banner
 */
private fun printStartupBanner() {
    val banner = """
        
    ██╗  ██╗ █████╗ ██████╗ ███████╗███████╗
    ██║  ██║██╔══██╗██╔══██╗██╔════╝██╔════╝
    ███████║███████║██║  ██║█████╗  ███████╗
    ██╔══██║██╔══██║██║  ██║██╔══╝  ╚════██║
    ██║  ██║██║  ██║██████╔╝███████╗███████║
    ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚══════╝╚══════╝
    
    🔥 The Powerful Marketing Decisioning Engine
    ⚡ Built with Kotlin + Spring Boot 3
    👨‍💻 Author: Sankhadeep Banerjee
    
    """.trimIndent()
    
    println(banner)
}