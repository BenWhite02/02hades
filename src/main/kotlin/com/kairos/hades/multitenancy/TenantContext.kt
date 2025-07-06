// File: src/main/kotlin/com/kairos/hades/multitenancy/TenantContext.kt
// 🔥 HADES Multi-Tenancy Context Management
// Thread-local tenant context for data isolation
// Ensures all database operations are tenant-aware

package com.kairos.hades.multitenancy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Thread-local tenant context manager
 * Provides tenant isolation across the application
 */
@Component
class TenantContext {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantContext::class.java)
        private val tenantIdHolder = ThreadLocal<String>()
        private val userIdHolder = ThreadLocal<String>()
        
        const val DEFAULT_TENANT_ID = "default"
        const val SYSTEM_TENANT_ID = "system"
    }
    
    /**
     * Set the current tenant ID for this thread
     */
    fun setTenantId(tenantId: String?) {
        if (tenantId.isNullOrBlank()) {
            logger.warn("Attempting to set null or blank tenant ID, using default")
            tenantIdHolder.set(DEFAULT_TENANT_ID)
        } else {
            tenantIdHolder.set(tenantId)
            logger.debug("Set tenant context: {}", tenantId)
        }
    }
    
    /**
     * Get the current tenant ID for this thread
     */
    fun getTenantId(): String {
        return tenantIdHolder.get() ?: DEFAULT_TENANT_ID
    }
    
    /**
     * Set the current user ID for this thread
     */
    fun setUserId(userId: String?) {
        userIdHolder.set(userId)
        logger.debug("Set user context: {}", userId)
    }
    
    /**
     * Get the current user ID for this thread
     */
    fun getUserId(): String? {
        return userIdHolder.get()
    }
    
    /**
     * Check if tenant context is set
     */
    fun hasTenantContext(): Boolean {
        return tenantIdHolder.get() != null
    }
    
    /**
     * Check if user context is set
     */
    fun hasUserContext(): Boolean {
        return userIdHolder.get() != null
    }
    
    /**
     * Clear tenant context for current thread
     */
    fun clearTenantContext() {
        tenantIdHolder.remove()
        logger.debug("Cleared tenant context")
    }
    
    /**
     * Clear user context for current thread
     */
    fun clearUserContext() {
        userIdHolder.remove()
        logger.debug("Cleared user context")
    }
    
    /**
     * Clear all context for current thread
     */
    fun clearContext() {
        clearTenantContext()
        clearUserContext()
        logger.debug("Cleared all context")
    }
    
    /**
     * Execute a block of code with a specific tenant context
     */
    fun <T> withTenant(tenantId: String, block: () -> T): T {
        val originalTenantId = getTenantId()
        return try {
            setTenantId(tenantId)
            block()
        } finally {
            setTenantId(originalTenantId)
        }
    }
    
    /**
     * Execute a block of code with system tenant context
     */
    fun <T> withSystemTenant(block: () -> T): T {
        return withTenant(SYSTEM_TENANT_ID, block)
    }
    
    /**
     * Get tenant context information
     */
    fun getContextInfo(): TenantContextInfo {
        return TenantContextInfo(
            tenantId = getTenantId(),
            userId = getUserId(),
            threadName = Thread.currentThread().name,
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Data class holding tenant context information
 */
data class TenantContextInfo(
    val tenantId: String,
    val userId: String?,
    val threadName: String,
    val timestamp: Long
)