
// =============================================================================

// File: src/main/kotlin/com/kairos/hades/config/ExternalServicesProperties.kt
// 🔥 HADES External Services Configuration
// Configuration for third-party integrations

package com.kairos.hades.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.stereotype.Component
import jakarta.validation.constraints.*

/**
 * External services configuration properties
 */
@Component
@ConfigurationProperties(prefix = "external")
data class ExternalServicesProperties(
    
    @NestedConfigurationProperty
    val email: EmailServiceProperties = EmailServiceProperties(),
    
    @NestedConfigurationProperty
    val webhooks: WebhookProperties = WebhookProperties(),
    
    @NestedConfigurationProperty
    val ai: AiServiceProperties = AiServiceProperties()
)

/**
 * Email service configuration
 */
data class EmailServiceProperties(
    @Pattern(regexp = "^(smtp|sendgrid|mailgun|ses)$", message = "Email provider must be smtp, sendgrid, mailgun, or ses")
    val provider: String = "smtp",
    
    val templatesEnabled: Boolean = true,
    
    val apiKey: String = "",
    val fromAddress: String = "noreply@kairos.app",
    val fromName: String = "Kairos Platform"
)

/**
 * Webhook configuration
 */
data class WebhookProperties(
    val enabled: Boolean = true,
    
    @Min(1000) // 1 second minimum
    @Max(120000) // 2 minutes maximum
    val timeout: Long = 30000, // 30 seconds
    
    @Min(0)
    @Max(10)
    val retryAttempts: Int = 3,
    
    @Min(1000) // 1 second minimum
    @Max(60000) // 1 minute maximum
    val retryDelay: Long = 5000, // 5 seconds
    
    @Min(1)
    @Max(1000)
    val maxConcurrentRequests: Int = 50
)

/**
 * AI service configuration
 */
data class AiServiceProperties(
    val enabled: Boolean = false,
    
    @Pattern(regexp = "^(openai|anthropic|google|azure)$", message = "AI provider must be openai, anthropic, google, or azure")
    val provider: String = "openai",
    
    val apiKey: String = "",
    val model: String = "gpt-3.5-turbo",
    
    @Min(5000) // 5 seconds minimum
    @Max(300000) // 5 minutes maximum
    val timeout: Long = 30000, // 30 seconds
    
    @Min(1)
    @Max(100)
    val maxTokens: Int = 1000,
    
    @DecimalMin("0.0")
    @DecimalMax("2.0")
    val temperature: Double = 0.7
)