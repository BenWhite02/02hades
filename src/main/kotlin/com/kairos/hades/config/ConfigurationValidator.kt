// =============================================================================

// File: src/main/kotlin/com/kairos/hades/config/ConfigurationValidator.kt
// 🔥 HADES Configuration Validator
// Validates configuration properties on startup

package com.kairos.hades.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator

/**
 * Validates configuration properties on application startup
 */
@Component
class ConfigurationValidator @Autowired constructor(
    private val hadesProperties: HadesProperties,
    private val externalServicesProperties: ExternalServicesProperties,
    private val databaseProperties: DatabaseProperties,
    private val validator: Validator
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(ConfigurationValidator::class.java)
    }
    
    @EventListener(ApplicationReadyEvent::class)
    fun validateConfiguration() {
        logger.info("Validating Hades configuration...")
        
        val violations = mutableListOf<ConstraintViolation<*>>()
        
        // Validate main properties
        violations.addAll(validator.validate(hadesProperties))
        violations.addAll(validator.validate(externalServicesProperties))
        violations.addAll(validator.validate(databaseProperties))
        
        if (violations.isNotEmpty()) {
            logger.error("Configuration validation failed:")
            violations.forEach { violation ->
                logger.error("  ${violation.propertyPath}: ${violation.message}")
            }
            throw IllegalStateException("Configuration validation failed. Check application.yml settings.")
        }
        
        // Additional custom validations
        validateCustomRules()
        
        logger.info("Configuration validation completed successfully")
        logConfigurationSummary()
    }
    
    private fun validateCustomRules() {
        // JWT secret validation
        if (hadesProperties.security.jwt.secret.length < 32) {
            throw IllegalStateException("JWT secret must be at least 32 characters long")
        }
        
        // Multi-tenancy validation
        if (hadesProperties.multitenancy.enabled && hadesProperties.multitenancy.defaultTenant.isBlank()) {
            throw IllegalStateException("Default tenant ID cannot be blank when multi-tenancy is enabled")
        }
        
        // Cache TTL validations
        if (hadesProperties.atoms.cacheTtl > hadesProperties.moments.cacheTtl) {
            logger.warn("Atoms cache TTL is longer than moments cache TTL - this may cause inconsistencies")
        }
        
        // AI service validation
        if (hadesProperties.features.aiOptimization && !externalServicesProperties.ai.enabled) {
            logger.warn("AI optimization is enabled but AI service is disabled")
        }
        
        // Webhook validation
        if (hadesProperties.features.webhookSystem && !externalServicesProperties.webhooks.enabled) {
            logger.warn("Webhook system feature is enabled but webhook service is disabled")
        }
    }
    
    private fun logConfigurationSummary() {
        logger.info("=== Hades Configuration Summary ===")
        logger.info("API Version: {}", hadesProperties.api.version)
        logger.info("Multi-tenancy: {}", if (hadesProperties.multitenancy.enabled) "Enabled" else "Disabled")
        logger.info("Features:")
        logger.info("  - Experimentation: {}", hadesProperties.features.experimentation)
        logger.info("  - AI Optimization: {}", hadesProperties.features.aiOptimization)
        logger.info("  - Real-time Updates: {}", hadesProperties.features.realTimeUpdates)
        logger.info("  - Webhook System: {}", hadesProperties.features.webhookSystem)
        logger.info("  - GraphQL API: {}", hadesProperties.features.graphqlApi)
        logger.info("Security:")
        logger.info("  - JWT Expiration: {} seconds", hadesProperties.security.jwt.expiration)
        logger.info("  - Rate Limiting: {}", if (hadesProperties.security.rateLimiting.enabled) "Enabled" else "Disabled")
        logger.info("Cache Settings:")
        logger.info("  - Atoms Cache TTL: {} seconds", hadesProperties.atoms.cacheTtl)
        logger.info("  - Moments Cache TTL: {} seconds", hadesProperties.moments.cacheTtl)
        logger.info("  - Decision Cache TTL: {} seconds", hadesProperties.decisionEngine.cacheTtl)
        logger.info("===================================")
    }
}