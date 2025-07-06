// =============================================================================

// File: src/main/kotlin/com/kairos/hades/enums/TenantStatus.kt
// 🔥 HADES Tenant Status Enumeration
// Defines the lifecycle states of a tenant

package com.kairos.hades.enums

/**
 * Tenant status enumeration
 */
enum class TenantStatus(val displayName: String, val description: String) {
    PENDING(
        displayName = "Pending",
        description = "Tenant is being set up and not yet active"
    ),
    
    ACTIVE(
        displayName = "Active",
        description = "Tenant is active and fully functional"
    ),
    
    SUSPENDED(
        displayName = "Suspended",
        description = "Tenant is temporarily suspended (payment issues, violations, etc.)"
    ),
    
    CANCELLED(
        displayName = "Cancelled",
        description = "Tenant subscription has been cancelled"
    ),
    
    EXPIRED(
        displayName = "Expired",
        description = "Tenant subscription has expired"
    ),
    
    ARCHIVED(
        displayName = "Archived",
        description = "Tenant is archived (data retained but not accessible)"
    );
    
    /**
     * Check if tenant can perform operations
     */
    fun isOperational(): Boolean = this == ACTIVE
    
    /**
     * Check if tenant can access data
     */
    fun canAccessData(): Boolean = this in listOf(ACTIVE, SUSPENDED)
    
    /**
     * Check if tenant is in a final state
     */
    fun isFinalState(): Boolean = this in listOf(CANCELLED, ARCHIVED)
}