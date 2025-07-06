/ =============================================================================

// File: src/main/kotlin/com/kairos/hades/entity/Tenant.kt
// 🔥 HADES Tenant Entity
// Multi-tenant architecture - each tenant has isolated data
// Supports subscription tiers and feature flags

package com.kairos.hades.entity

import com.kairos.hades.enums.SubscriptionTier
import com.kairos.hades.enums.TenantStatus
import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.LocalDateTime

/**
 * Tenant entity for multi-tenancy support
 * Each tenant represents an organization or customer using the platform
 */
@Entity
@Table(
    name = "tenants",
    indexes = [
        Index(name = "idx_tenant_slug", columnList = "slug", unique = true),
        Index(name = "idx_tenant_status", columnList = "status"),
        Index(name = "idx_tenant_subscription_tier", columnList = "subscription_tier"),
        Index(name = "idx_tenant_created_at", columnList = "created_at")
    ]
)
class Tenant : BaseEntity() {
    
    /**
     * Tenant name (organization name)
     */
    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Tenant name is required")
    @Size(min = 1, max = 255, message = "Tenant name must be between 1 and 255 characters")
    var name: String = ""
    
    /**
     * Unique slug for tenant identification in URLs
     */
    @Column(name = "slug", nullable = false, length = 100, unique = true)
    @NotBlank(message = "Tenant slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 3, max = 100, message = "Slug must be between 3 and 100 characters")
    var slug: String = ""
    
    /**
     * Tenant description
     */
    @Column(name = "description", length = 1000)
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    var description: String? = null
    
    /**
     * Primary contact email
     */
    @Column(name = "contact_email", nullable = false, length = 255)
    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    var contactEmail: String = ""
    
    /**
     * Primary contact phone
     */
    @Column(name = "contact_phone", length = 20)
    var contactPhone: String? = null
    
    /**
     * Tenant website URL
     */
    @Column(name = "website_url", length = 500)
    @Size(max = 500, message = "Website URL cannot exceed 500 characters")
    var websiteUrl: String? = null
    
    /**
     * Tenant logo URL
     */
    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null
    
    /**
     * Current subscription tier
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 50)
    var subscriptionTier: SubscriptionTier = SubscriptionTier.FREE
    
    /**
     * Tenant status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: TenantStatus = TenantStatus.ACTIVE
    
    /**
     * Maximum number of users allowed
     */
    @Column(name = "max_users", nullable = false)
    @Min(value = 1, message = "Max users must be at least 1")
    var maxUsers: Int = 5
    
    /**
     * Maximum storage in bytes
     */
    @Column(name = "max_storage_bytes", nullable = false)
    @Min(value = 0, message = "Max storage cannot be negative")
    var maxStorageBytes: Long = 1073741824L // 1GB default
    
    /**
     * Current storage usage in bytes
     */
    @Column(name = "current_storage_bytes", nullable = false)
    @Min(value = 0, message = "Current storage cannot be negative")
    var currentStorageBytes: Long = 0L
    
    /**
     * Maximum API requests per month
     */
    @Column(name = "max_api_requests_per_month", nullable = false)
    @Min(value = 0, message = "Max API requests cannot be negative")
    var maxApiRequestsPerMonth: Long = 10000L
    
    /**
     * Current API requests this month
     */
    @Column(name = "current_api_requests_month", nullable = false)
    @Min(value = 0, message = "Current API requests cannot be negative")
    var currentApiRequestsMonth: Long = 0L
    
    /**
     * Feature flags as JSON
     */
    @Column(name = "feature_flags", columnDefinition = "TEXT")
    var featureFlags: String? = null
    
    /**
     * Custom settings as JSON
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    var settings: String? = null
    
    /**
     * Subscription start date
     */
    @Column(name = "subscription_start_date")
    var subscriptionStartDate: LocalDateTime? = null
    
    /**
     * Subscription end date
     */
    @Column(name = "subscription_end_date")
    var subscriptionEndDate: LocalDateTime? = null
    
    /**
     * Trial end date
     */
    @Column(name = "trial_end_date")
    var trialEndDate: LocalDateTime? = null
    
    /**
     * Last billing date
     */
    @Column(name = "last_billing_date")
    var lastBillingDate: LocalDateTime? = null
    
    /**
     * Next billing date
     */
    @Column(name = "next_billing_date")
    var nextBillingDate: LocalDateTime? = null
    
    /**
     * Billing customer ID (for payment processor)
     */
    @Column(name = "billing_customer_id", length = 100)
    var billingCustomerId: String? = null
    
    /**
     * Time zone for this tenant
     */
    @Column(name = "timezone", length = 50, nullable = false)
    @NotBlank(message = "Timezone is required")
    var timezone: String = "UTC"
    
    /**
     * Locale/language for this tenant
     */
    @Column(name = "locale", length = 10, nullable = false)
    @NotBlank(message = "Locale is required")
    var locale: String = "en"
    
    /**
     * Custom domain for this tenant
     */
    @Column(name = "custom_domain", length = 255)
    var customDomain: String? = null
    
    /**
     * SSL certificate information
     */
    @Column(name = "ssl_certificate_info", columnDefinition = "TEXT")
    var sslCertificateInfo: String? = null
    
    /**
     * Onboarding completion status
     */
    @Column(name = "onboarding_completed", nullable = false)
    var onboardingCompleted: Boolean = false
    
    /**
     * Onboarding completion date
     */
    @Column(name = "onboarding_completed_at")
    var onboardingCompletedAt: LocalDateTime? = null
    
    /**
     * Last login timestamp for any user in this tenant
     */
    @Column(name = "last_activity_at")
    var lastActivityAt: LocalDateTime? = null
    
    /**
     * Check if tenant is active
     */
    fun isActive(): Boolean = status == TenantStatus.ACTIVE
    
    /**
     * Check if tenant is suspended
     */
    fun isSuspended(): Boolean = status == TenantStatus.SUSPENDED
    
    /**
     * Check if tenant is in trial
     */
    fun isInTrial(): Boolean = trialEndDate?.isAfter(LocalDateTime.now()) == true
    
    /**
     * Check if subscription is expired
     */
    fun isSubscriptionExpired(): Boolean = 
        subscriptionEndDate?.isBefore(LocalDateTime.now()) == true
    
    /**
     * Check if tenant has exceeded storage limit
     */
    fun hasExceededStorageLimit(): Boolean = currentStorageBytes >= maxStorageBytes
    
    /**
     * Check if tenant has exceeded API request limit
     */
    fun hasExceededApiLimit(): Boolean = currentApiRequestsMonth >= maxApiRequestsPerMonth
    
    /**
     * Get storage usage percentage
     */
    fun getStorageUsagePercentage(): Double = 
        if (maxStorageBytes > 0) (currentStorageBytes.toDouble() / maxStorageBytes.toDouble()) * 100.0 else 0.0
    
    /**
     * Get API usage percentage for current month
     */
    fun getApiUsagePercentage(): Double = 
        if (maxApiRequestsPerMonth > 0) (currentApiRequestsMonth.toDouble() / maxApiRequestsPerMonth.toDouble()) * 100.0 else 0.0
    
    /**
     * Reset monthly API usage (called on billing cycle)
     */
    fun resetMonthlyApiUsage() {
        currentApiRequestsMonth = 0L
    }
    
    /**
     * Increment API usage
     */
    fun incrementApiUsage(requests: Long = 1L) {
        currentApiRequestsMonth += requests
    }
    
    /**
     * Update storage usage
     */
    fun updateStorageUsage(bytes: Long) {
        currentStorageBytes = maxOf(0L, currentStorageBytes + bytes)
    }
    
    /**
     * Activate tenant
     */
    fun activate() {
        status = TenantStatus.ACTIVE
        lastActivityAt = LocalDateTime.now()
    }
    
    /**
     * Suspend tenant
     */
    fun suspend() {
        status = TenantStatus.SUSPENDED
    }
    
    /**
     * Complete onboarding
     */
    fun completeOnboarding() {
        onboardingCompleted = true
        onboardingCompletedAt = LocalDateTime.now()
    }
    
    override fun prePersist() {
        super.prePersist()
        // For Tenant entity, we need to set tenantId to its own ID after generation
        // This will be handled in the service layer
    }
}
