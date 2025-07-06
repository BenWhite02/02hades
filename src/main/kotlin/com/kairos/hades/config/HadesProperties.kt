// File: src/main/kotlin/com/kairos/hades/config/HadesProperties.kt
// 🔥 HADES Configuration Properties
// Centralized configuration for all Hades backend settings
// Maps application.yml properties to type-safe Kotlin classes

package com.kairos.hades.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.stereotype.Component
import jakarta.validation.constraints.*
import java.time.Duration

/**
 * Main configuration properties for Hades backend
 */
@Component
@ConfigurationProperties(prefix = "hades")
data class HadesProperties(
    
    @NestedConfigurationProperty
    val api: ApiProperties = ApiProperties(),
    
    @NestedConfigurationProperty
    val security: SecurityProperties = SecurityProperties(),
    
    @NestedConfigurationProperty
    val multitenancy: MultitenancyProperties = MultitenancyProperties(),
    
    @NestedConfigurationProperty
    val atoms: AtomsProperties = AtomsProperties(),
    
    @NestedConfigurationProperty
    val moments: MomentsProperties = MomentsProperties(),
    
    @NestedConfigurationProperty
    val decisionEngine: DecisionEngineProperties = DecisionEngineProperties(),
    
    @NestedConfigurationProperty
    val theme: ThemeProperties = ThemeProperties(),
    
    @NestedConfigurationProperty
    val analytics: AnalyticsProperties = AnalyticsProperties(),
    
    @NestedConfigurationProperty
    val features: FeatureProperties = FeatureProperties()
)

/**
 * API configuration properties
 */
data class ApiProperties(
    @NotBlank
    val version: String = "1.0.0",
    
    @NotBlank
    val title: String = "Hades Backend API",
    
    @NotBlank
    val description: String = "The Powerful Decision Engine for Kairos",
    
    @NestedConfigurationProperty
    val contact: ContactProperties = ContactProperties(),
    
    @NestedConfigurationProperty
    val license: LicenseProperties = LicenseProperties(),
    
    @NestedConfigurationProperty
    val error: ErrorProperties = ErrorProperties()
)

/**
 * API contact information
 */
data class ContactProperties(
    val name: String = "Kairos Team",
    
    @Email
    val email: String = "team@kairos.app",
    
    val url: String = "https://kairos.app"
)

/**
 * API license information
 */
data class LicenseProperties(
    val name: String = "Proprietary",
    val url: String = ""
)

/**
 * Error handling configuration
 */
data class ErrorProperties(
    val includeStackTrace: Boolean = false,
    val includeInternalDetails: Boolean = false,
    
    @Min(100)
    @Max(5000)
    val maxErrorMessageLength: Int = 500
)

/**
 * Security configuration properties
 */
data class SecurityProperties(
    @NestedConfigurationProperty
    val jwt: JwtProperties = JwtProperties(),
    
    @NestedConfigurationProperty
    val cors: CorsProperties = CorsProperties(),
    
    @NestedConfigurationProperty
    val rateLimiting: RateLimitingProperties = RateLimitingProperties()
)

/**
 * JWT configuration
 */
data class JwtProperties(
    @NotBlank
    @Size(min = 32, message = "JWT secret must be at least 32 characters")
    val secret: String = "your-256-bit-secret-key-change-this-in-production",
    
    @Min(300) // 5 minutes minimum
    @Max(86400) // 24 hours maximum
    val expiration: Long = 86400, // 24 hours in seconds
    
    @Min(86400) // 1 day minimum
    @Max(2592000) // 30 days maximum
    val refreshExpiration: Long = 604800, // 7 days in seconds
    
    @NotBlank
    val issuer: String = "hades-backend",
    
    @NotBlank
    val audience: String = "kairos-frontend"
)

/**
 * CORS configuration
 */
data class CorsProperties(
    val allowedOrigins: List<String> = listOf("http://localhost:3000", "http://localhost:3001"),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"),
    val allowedHeaders: List<String> = listOf("*"),
    val exposedHeaders: List<String> = listOf("X-Total-Count", "X-Page-Number", "X-Page-Size"),
    val allowCredentials: Boolean = true,
    
    @Min(0)
    @Max(86400)
    val maxAge: Long = 3600 // 1 hour
)

/**
 * Rate limiting configuration
 */
data class RateLimitingProperties(
    val enabled: Boolean = true,
    
    @Min(1)
    @Max(10000)
    val requestsPerMinute: Int = 100,
    
    @Min(1)
    @Max(100)
    val burstCapacity: Int = 10
)

/**
 * Multi-tenancy configuration properties
 */
data class MultitenancyProperties(
    val enabled: Boolean = true,
    
    @NotBlank
    val headerName: String = "X-Tenant-ID",
    
    @NotBlank
    val defaultTenant: String = "default",
    
    @Pattern(regexp = "^(SHARED|SCHEMA|DATABASE)$", message = "Isolation level must be SHARED, SCHEMA, or DATABASE")
    val isolationLevel: String = "SCHEMA",
    
    @Min(1)
    @Max(10000)
    val maxTenantsPerInstance: Int = 1000
)

/**
 * EligibilityAtoms configuration properties
 */
data class AtomsProperties(
    val cacheEnabled: Boolean = true,
    
    @Min(60) // 1 minute minimum
    @Max(86400) // 24 hours maximum
    val cacheTtl: Long = 1800, // 30 minutes
    
    @Min(1)
    @Max(50)
    val maxCompositionDepth: Int = 10,
    
    @Min(1000) // 1 second minimum
    @Max(30000) // 30 seconds maximum
    val executionTimeout: Long = 5000, // 5 seconds
    
    val parallelExecution: Boolean = true,
    val validationEnabled: Boolean = true
)

/**
 * Moment management configuration properties
 */
data class MomentsProperties(
    val cacheEnabled: Boolean = true,
    
    @Min(300) // 5 minutes minimum
    @Max(86400) // 24 hours maximum
    val cacheTtl: Long = 3600, // 1 hour
    
    @Min(1)
    @Max(100)
    val maxVariants: Int = 10,
    
    val versioningEnabled: Boolean = true,
    val approvalWorkflow: Boolean = false
)

/**
 * Decision engine configuration properties
 */
data class DecisionEngineProperties(
    val cacheEnabled: Boolean = true,
    
    @Min(60) // 1 minute minimum
    @Max(3600) // 1 hour maximum
    val cacheTtl: Long = 300, // 5 minutes
    
    @Min(1)
    @Max(1000)
    val maxConcurrentDecisions: Int = 100,
    
    @Min(500) // 0.5 seconds minimum
    @Max(10000) // 10 seconds maximum
    val timeout: Long = 3000, // 3 seconds
    
    val fallbackEnabled: Boolean = true,
    val auditEnabled: Boolean = true
)

/**
 * Theme system configuration properties
 */
data class ThemeProperties(
    @Min(300) // 5 minutes minimum
    @Max(86400) // 24 hours maximum
    val cssCacheDuration: Long = 3600, // 1 hour
    
    @Min(1024) // 1KB minimum
    @Max(1048576) // 1MB maximum
    val maxCustomCssSize: Long = 51200, // 50KB
    
    val defaultThemeEnabled: Boolean = true,
    val customThemesEnabled: Boolean = true,
    val themeCreatorEnabled: Boolean = true,
    val validationEnabled: Boolean = true
)

/**
 * Analytics configuration properties
 */
data class AnalyticsProperties(
    val enabled: Boolean = true,
    
    @Min(1)
    @Max(10000)
    val batchSize: Int = 100,
    
    @Min(1000) // 1 second minimum
    @Max(300000) // 5 minutes maximum
    val flushInterval: Long = 60000, // 1 minute
    
    @Min(1)
    @Max(365)
    val retentionDays: Int = 90
)

/**
 * Feature flags configuration
 */
data class FeatureProperties(
    val experimentation: Boolean = true,
    val aiOptimization: Boolean = false,
    val realTimeUpdates: Boolean = true,
    val webhookSystem: Boolean = true,
    val graphqlApi: Boolean = false
)