// =============================================================================

// File: src/main/kotlin/com/kairos/hades/enums/SubscriptionTier.kt
// 🔥 HADES Subscription Tier Enumeration
// Defines different subscription levels and their capabilities

package com.kairos.hades.enums

/**
 * Subscription tiers available in the platform
 */
enum class SubscriptionTier(
    val displayName: String,
    val maxUsers: Int,
    val maxStorageGB: Int,
    val maxApiRequestsPerMonth: Long,
    val maxAtoms: Int,
    val maxMoments: Int,
    val customThemes: Boolean,
    val advancedAnalytics: Boolean,
    val webhooks: Boolean,
    val aiOptimization: Boolean,
    val priority: Int // Higher number = higher tier
) {
    FREE(
        displayName = "Free",
        maxUsers = 3,
        maxStorageGB = 1,
        maxApiRequestsPerMonth = 10_000L,
        maxAtoms = 10,
        maxMoments = 50,
        customThemes = false,
        advancedAnalytics = false,
        webhooks = false,
        aiOptimization = false,
        priority = 1
    ),
    
    STARTER(
        displayName = "Starter",
        maxUsers = 10,
        maxStorageGB = 5,
        maxApiRequestsPerMonth = 100_000L,
        maxAtoms = 50,
        maxMoments = 500,
        customThemes = true,
        advancedAnalytics = false,
        webhooks = true,
        aiOptimization = false,
        priority = 2
    ),
    
    PROFESSIONAL(
        displayName = "Professional",
        maxUsers = 50,
        maxStorageGB = 25,
        maxApiRequestsPerMonth = 1_000_000L,
        maxAtoms = 200,
        maxMoments = 2_000,
        customThemes = true,
        advancedAnalytics = true,
        webhooks = true,
        aiOptimization = true,
        priority = 3
    ),
    
    ENTERPRISE(
        displayName = "Enterprise",
        maxUsers = Int.MAX_VALUE,
        maxStorageGB = Int.MAX_VALUE,
        maxApiRequestsPerMonth = Long.MAX_VALUE,
        maxAtoms = Int.MAX_VALUE,
        maxMoments = Int.MAX_VALUE,
        customThemes = true,
        advancedAnalytics = true,
        webhooks = true,
        aiOptimization = true,
        priority = 4
    );
    
    /**
     * Check if this tier has a specific feature
     */
    fun hasFeature(feature: String): Boolean = when (feature) {
        "custom_themes" -> customThemes
        "advanced_analytics" -> advancedAnalytics
        "webhooks" -> webhooks
        "ai_optimization" -> aiOptimization
        else -> false
    }
    
    /**
     * Check if this tier can be upgraded to another tier
     */
    fun canUpgradeTo(targetTier: SubscriptionTier): Boolean = priority < targetTier.priority
    
    /**
     * Check if this tier can be downgraded to another tier
     */
    fun canDowngradeTo(targetTier: SubscriptionTier): Boolean = priority > targetTier.priority
}