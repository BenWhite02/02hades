// =============================================================================
// File: src/main/kotlin/com/kairos/hades/enums/TenantEnums.kt
// 🔥 HADES Tenant and Subscription Enumerations
// Author: Sankhadeep Banerjee
// Defines enums for multi-tenancy and subscription management
// =============================================================================

package com.kairos.hades.enums

/**
 * Subscription tiers for multi-tenant platform
 * Each tier has different feature access and limits
 */
enum class SubscriptionTier(
    val displayName: String,
    val description: String,
    val maxAtoms: Int,
    val maxApiRequestsPerMonth: Long,
    val maxStorageGB: Long,
    val maxUsers: Int,
    val features: Set<String>,
    val monthlyPriceUSD: Double
) {
    FREE(
        displayName = "Free",
        description = "Basic tier for getting started",
        maxAtoms = 10,
        maxApiRequestsPerMonth = 1000L,
        maxStorageGB = 1L,
        maxUsers = 1,
        features = setOf("basic_atoms", "dashboard", "api_access"),
        monthlyPriceUSD = 0.0
    ),
    
    STARTER(
        displayName = "Starter",
        description = "Perfect for small teams and projects",
        maxAtoms = 50,
        maxApiRequestsPerMonth = 10000L,
        maxStorageGB = 5L,
        maxUsers = 5,
        features = setOf("basic_atoms", "custom_atoms", "dashboard", "api_access", "webhooks", "email_support"),
        monthlyPriceUSD = 29.0
    ),
    
    PROFESSIONAL(
        displayName = "Professional",
        description = "Advanced features for growing businesses",
        maxAtoms = 200,
        maxApiRequestsPerMonth = 100000L,
        maxStorageGB = 25L,
        maxUsers = 25,
        features = setOf(
            "basic_atoms", "custom_atoms", "ml_atoms", "dashboard", "api_access", 
            "webhooks", "integrations", "advanced_analytics", "priority_support", 
            "sso", "audit_logs"
        ),
        monthlyPriceUSD = 99.0
    ),
    
    ENTERPRISE(
        displayName = "Enterprise",
        description = "Full platform access for large organizations",
        maxAtoms = 1000,
        maxApiRequestsPerMonth = 1000000L,
        maxStorageGB = 100L,
        maxUsers = 100,
        features = setOf(
            "basic_atoms", "custom_atoms", "ml_atoms", "dashboard", "api_access",
            "webhooks", "integrations", "advanced_analytics", "dedicated_support",
            "sso", "audit_logs", "white_label", "custom_integrations", "sla"
        ),
        monthlyPriceUSD = 299.0
    ),
    
    UNLIMITED(
        displayName = "Unlimited",
        description = "No limits for enterprise customers",
        maxAtoms = Int.MAX_VALUE,
        maxApiRequestsPerMonth = Long.MAX_VALUE,
        maxStorageGB = Long.MAX_VALUE,
        maxUsers = Int.MAX_VALUE,
        features = setOf(
            "basic_atoms", "custom_atoms", "ml_atoms", "dashboard", "api_access",
            "webhooks", "integrations", "advanced_analytics", "dedicated_support",
            "sso", "audit_logs", "white_label", "custom_integrations", "sla",
            "on_premise", "custom_development"
        ),
        monthlyPriceUSD = 999.0
    );
    
    /**
     * Check if tier includes a specific feature
     */
    fun hasFeature(feature: String): Boolean = features.contains(feature)
    
    /**
     * Get storage limit in bytes
     */
    fun getMaxStorageBytes(): Long = maxStorageGB * 1024 * 1024 * 1024
    
    /**
     * Check if this tier is higher than another
     */
    fun isHigherThan(other: SubscriptionTier): Boolean = this.ordinal > other.ordinal
    
    /**
     * Check if this tier is lower than another
     */
    fun isLowerThan(other: SubscriptionTier): Boolean = this.ordinal < other.ordinal
}

/**
 * Status of a tenant in the system
 */
enum class TenantStatus(
    val displayName: String,
    val description: String,
    val isActive: Boolean,
    val allowLogin: Boolean,
    val allowApiAccess: Boolean
) {
    ACTIVE(
        displayName = "Active",
        description = "Tenant is active and fully functional",
        isActive = true,
        allowLogin = true,
        allowApiAccess = true
    ),
    
    TRIAL(
        displayName = "Trial",
        description = "Tenant is in trial period",
        isActive = true,
        allowLogin = true,
        allowApiAccess = true
    ),
    
    SUSPENDED(
        displayName = "Suspended",
        description = "Tenant is temporarily suspended",
        isActive = false,
        allowLogin = false,
        allowApiAccess = false
    ),
    
    PAYMENT_REQUIRED(
        displayName = "Payment Required",
        description = "Payment is overdue",
        isActive = false,
        allowLogin = true,
        allowApiAccess = false
    ),
    
    CANCELLED(
        displayName = "Cancelled",
        description = "Tenant subscription has been cancelled",
        isActive = false,
        allowLogin = false,
        allowApiAccess = false
    ),
    
    ARCHIVED(
        displayName = "Archived",
        description = "Tenant data is archived",
        isActive = false,
        allowLogin = false,
        allowApiAccess = false
    ),
    
    PENDING_ACTIVATION(
        displayName = "Pending Activation",
        description = "Waiting for tenant activation",
        isActive = false,
        allowLogin = false,
        allowApiAccess = false
    );
    
    /**
     * Check if tenant can access the system
     */
    fun canAccess(): Boolean = allowLogin || allowApiAccess
}

/**
 * Types of tenant applications
 */
enum class ApplicationType(
    val displayName: String,
    val description: String,
    val defaultSubdomain: String
) {
    MAIN_DASHBOARD(
        displayName = "Main Dashboard",
        description = "Primary tenant dashboard",
        defaultSubdomain = "app"
    ),
    
    API_GATEWAY(
        displayName = "API Gateway",
        description = "API access point",
        defaultSubdomain = "api"
    ),
    
    WEBHOOK_HANDLER(
        displayName = "Webhook Handler",
        description = "Webhook processing endpoint",
        defaultSubdomain = "webhooks"
    ),
    
    DEVELOPER_PORTAL(
        displayName = "Developer Portal",
        description = "Documentation and developer tools",
        defaultSubdomain = "dev"
    ),
    
    CUSTOMER_PORTAL(
        displayName = "Customer Portal",
        description = "External customer-facing interface",
        defaultSubdomain = "portal"
    ),
    
    ANALYTICS_DASHBOARD(
        displayName = "Analytics Dashboard",
        description = "Data visualization and reporting",
        defaultSubdomain = "analytics"
    ),
    
    CUSTOM(
        displayName = "Custom Application",
        description = "User-defined custom application",
        defaultSubdomain = "custom"
    )
}

/**
 * Feature flags for tenant capabilities
 */
enum class TenantFeature(
    val featureKey: String,
    val displayName: String,
    val description: String,
    val requiredTier: SubscriptionTier
) {
    BASIC_ATOMS(
        featureKey = "basic_atoms",
        displayName = "Basic Atoms",
        description = "Access to pre-built basic eligibility atoms",
        requiredTier = SubscriptionTier.FREE
    ),
    
    CUSTOM_ATOMS(
        featureKey = "custom_atoms",
        displayName = "Custom Atoms",
        description = "Create and manage custom eligibility atoms",
        requiredTier = SubscriptionTier.STARTER
    ),
    
    ML_ATOMS(
        featureKey = "ml_atoms",
        displayName = "Machine Learning Atoms",
        description = "AI-powered predictive atoms",
        requiredTier = SubscriptionTier.PROFESSIONAL
    ),
    
    API_ACCESS(
        featureKey = "api_access",
        displayName = "API Access",
        description = "REST API and GraphQL access",
        requiredTier = SubscriptionTier.FREE
    ),
    
    WEBHOOKS(
        featureKey = "webhooks",
        displayName = "Webhooks",
        description = "Real-time event notifications",
        requiredTier = SubscriptionTier.STARTER
    ),
    
    SSO(
        featureKey = "sso",
        displayName = "Single Sign-On",
        description = "SAML/OAuth2 authentication integration",
        requiredTier = SubscriptionTier.PROFESSIONAL
    ),
    
    AUDIT_LOGS(
        featureKey = "audit_logs",
        displayName = "Audit Logs",
        description = "Comprehensive activity logging",
        requiredTier = SubscriptionTier.PROFESSIONAL
    ),
    
    ADVANCED_ANALYTICS(
        featureKey = "advanced_analytics",
        displayName = "Advanced Analytics",
        description = "Detailed performance metrics and insights",
        requiredTier = SubscriptionTier.PROFESSIONAL
    ),
    
    WHITE_LABEL(
        featureKey = "white_label",
        displayName = "White Label",
        description = "Custom branding and domain",
        requiredTier = SubscriptionTier.ENTERPRISE
    ),
    
    PRIORITY_SUPPORT(
        featureKey = "priority_support",
        displayName = "Priority Support",
        description = "24/7 priority customer support",
        requiredTier = SubscriptionTier.PROFESSIONAL
    ),
    
    DEDICATED_SUPPORT(
        featureKey = "dedicated_support",
        displayName = "Dedicated Support",
        description = "Dedicated customer success manager",
        requiredTier = SubscriptionTier.ENTERPRISE
    ),
    
    CUSTOM_INTEGRATIONS(
        featureKey = "custom_integrations",
        displayName = "Custom Integrations",
        description = "Bespoke integration development",
        requiredTier = SubscriptionTier.ENTERPRISE
    ),
    
    ON_PREMISE(
        featureKey = "on_premise",
        displayName = "On-Premise Deployment",
        description = "Deploy in your own infrastructure",
        requiredTier = SubscriptionTier.UNLIMITED
    ),
    
    SLA(
        featureKey = "sla",
        displayName = "Service Level Agreement",
        description = "Guaranteed uptime and response times",
        requiredTier = SubscriptionTier.ENTERPRISE
    );
    
    /**
     * Check if feature is available for given tier
     */
    fun isAvailableForTier(tier: SubscriptionTier): Boolean = 
        tier.isHigherThan(requiredTier) || tier == requiredTier
}

/**
 * Tenant onboarding status
 */
enum class OnboardingStatus(
    val displayName: String,
    val description: String,
    val order: Int
) {
    NOT_STARTED("Not Started", "Onboarding has not begun", 0),
    PROFILE_SETUP("Profile Setup", "Setting up tenant profile", 1),
    TEAM_SETUP("Team Setup", "Adding team members", 2),
    INTEGRATION_SETUP("Integration Setup", "Configuring integrations", 3),
    ATOMS_SETUP("Atoms Setup", "Creating first atoms", 4),
    TESTING("Testing", "Testing the setup", 5),
    COMPLETED("Completed", "Onboarding finished", 6);
    
    /**
     * Get next onboarding step
     */
    fun getNext(): OnboardingStatus? = 
        values().find { it.order == this.order + 1 }
    
    /**
     * Check if this step comes before another
     */
    fun isBefore(other: OnboardingStatus): Boolean = this.order < other.order
    
    /**
     * Check if this step comes after another
     */
    fun isAfter(other: OnboardingStatus): Boolean = this.order > other.order
}

/**
 * Data retention policies
 */
enum class DataRetentionPolicy(
    val displayName: String,
    val description: String,
    val retentionDays: Int
) {
    SHORT_TERM("Short Term", "30 days retention", 30),
    MEDIUM_TERM("Medium Term", "90 days retention", 90),
    LONG_TERM("Long Term", "1 year retention", 365),
    EXTENDED("Extended", "3 years retention", 1095),
    PERMANENT("Permanent", "No automatic deletion", Int.MAX_VALUE);
    
    /**
     * Check if data should be retained given age in days
     */
    fun shouldRetain(ageInDays: Int): Boolean = ageInDays <= retentionDays
}