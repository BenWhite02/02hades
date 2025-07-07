// =============================================================================
// File: src/main/kotlin/com/kairos/hades/multitenancy/TenantContext.kt
// 🔥 HADES Tenant Context Management
// Author: Sankhadeep Banerjee
// Thread-local tenant context for multi-tenant data isolation
// =============================================================================

package com.kairos.hades.multitenancy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

/**
 * Manages the current tenant context using ThreadLocal storage
 * Ensures tenant isolation across all operations
 */
@Component
class TenantContext {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantContext::class.java)
        private val TENANT_ID: ThreadLocal<String> = ThreadLocal()
        private val USER_ID: ThreadLocal<String> = ThreadLocal()
        private val USER_ROLES: ThreadLocal<Set<String>> = ThreadLocal()
        private val REQUEST_ID: ThreadLocal<String> = ThreadLocal()
        private val SESSION_ID: ThreadLocal<String> = ThreadLocal()
        
        const val DEFAULT_TENANT = "system"
        const val SYSTEM_USER = "system"
    }
    
    /**
     * Set the current tenant ID
     */
    fun setTenantId(tenantId: String?) {
        if (tenantId.isNullOrBlank()) {
            logger.warn("Attempting to set null or blank tenant ID, using default")
            TENANT_ID.set(DEFAULT_TENANT)
        } else {
            TENANT_ID.set(tenantId)
            logger.debug("Set tenant context: {}", tenantId)
        }
    }
    
    /**
     * Get the current tenant ID
     */
    fun getTenantId(): String {
        return TENANT_ID.get() ?: run {
            logger.warn("No tenant ID found in context, using default")
            DEFAULT_TENANT
        }
    }
    
    /**
     * Set the current user ID
     */
    fun setUserId(userId: String?) {
        if (userId.isNullOrBlank()) {
            USER_ID.set(SYSTEM_USER)
        } else {
            USER_ID.set(userId)
            logger.debug("Set user context: {}", userId)
        }
    }
    
    /**
     * Get the current user ID
     */
    fun getUserId(): String? {
        return USER_ID.get()
    }
    
    /**
     * Set the current user roles
     */
    fun setUserRoles(roles: Set<String>?) {
        USER_ROLES.set(roles ?: emptySet())
        logger.debug("Set user roles: {}", roles)
    }
    
    /**
     * Get the current user roles
     */
    fun getUserRoles(): Set<String> {
        return USER_ROLES.get() ?: emptySet()
    }
    
    /**
     * Set the current request ID for tracing
     */
    fun setRequestId(requestId: String?) {
        REQUEST_ID.set(requestId ?: UUID.randomUUID().toString())
    }
    
    /**
     * Get the current request ID
     */
    fun getRequestId(): String {
        return REQUEST_ID.get() ?: run {
            val newRequestId = UUID.randomUUID().toString()
            REQUEST_ID.set(newRequestId)
            newRequestId
        }
    }
    
    /**
     * Set the current session ID
     */
    fun setSessionId(sessionId: String?) {
        SESSION_ID.set(sessionId)
    }
    
    /**
     * Get the current session ID
     */
    fun getSessionId(): String? {
        return SESSION_ID.get()
    }
    
    /**
     * Set complete user context
     */
    fun setUserContext(userId: String?, roles: Set<String>?, sessionId: String?) {
        setUserId(userId)
        setUserRoles(roles)
        setSessionId(sessionId)
    }
    
    /**
     * Check if current user has a specific role
     */
    fun hasRole(role: String): Boolean {
        return getUserRoles().contains(role)
    }
    
    /**
     * Check if current user has any of the specified roles
     */
    fun hasAnyRole(vararg roles: String): Boolean {
        val userRoles = getUserRoles()
        return roles.any { userRoles.contains(it) }
    }
    
    /**
     * Check if current user has all of the specified roles
     */
    fun hasAllRoles(vararg roles: String): Boolean {
        val userRoles = getUserRoles()
        return roles.all { userRoles.contains(it) }
    }
    
    /**
     * Check if current user is system user
     */
    fun isSystemUser(): Boolean {
        return getUserId() == SYSTEM_USER
    }
    
    /**
     * Check if tenant context is set
     */
    fun hasTenantContext(): Boolean {
        return TENANT_ID.get() != null
    }
    
    /**
     * Check if user context is set
     */
    fun hasUserContext(): Boolean {
        return USER_ID.get() != null
    }
    
    /**
     * Get full context as string for logging
     */
    fun getContextString(): String {
        return "TenantContext(tenantId='${getTenantId()}', userId='${getUserId()}', " +
                "roles=${getUserRoles()}, requestId='${getRequestId()}', sessionId='${getSessionId()}')"
    }
    
    /**
     * Clear all context (important for thread reuse)
     */
    fun clear() {
        TENANT_ID.remove()
        USER_ID.remove()
        USER_ROLES.remove()
        REQUEST_ID.remove()
        SESSION_ID.remove()
        logger.debug("Cleared tenant context")
    }
    
    /**
     * Clear only user context but keep tenant
     */
    fun clearUserContext() {
        USER_ID.remove()
        USER_ROLES.remove()
        SESSION_ID.remove()
        logger.debug("Cleared user context for tenant: {}", getTenantId())
    }
    
    /**
     * Execute code with specific tenant context
     */
    fun <T> withTenant(tenantId: String, block: () -> T): T {
        val originalTenantId = TENANT_ID.get()
        return try {
            setTenantId(tenantId)
            block()
        } finally {
            if (originalTenantId != null) {
                TENANT_ID.set(originalTenantId)
            } else {
                TENANT_ID.remove()
            }
        }
    }
    
    /**
     * Execute code with specific user context
     */
    fun <T> withUser(userId: String, roles: Set<String> = emptySet(), block: () -> T): T {
        val originalUserId = USER_ID.get()
        val originalRoles = USER_ROLES.get()
        return try {
            setUserId(userId)
            setUserRoles(roles)
            block()
        } finally {
            if (originalUserId != null) {
                USER_ID.set(originalUserId)
            } else {
                USER_ID.remove()
            }
            if (originalRoles != null) {
                USER_ROLES.set(originalRoles)
            } else {
                USER_ROLES.remove()
            }
        }
    }
    
    /**
     * Execute code with system user context
     */
    fun <T> withSystemUser(block: () -> T): T {
        return withUser(SYSTEM_USER, setOf("SYSTEM")) {
            block()
        }
    }
    
    /**
     * Execute code with complete context
     */
    fun <T> withContext(
        tenantId: String,
        userId: String? = null,
        roles: Set<String> = emptySet(),
        requestId: String? = null,
        sessionId: String? = null,
        block: () -> T
    ): T {
        val originalTenantId = TENANT_ID.get()
        val originalUserId = USER_ID.get()
        val originalRoles = USER_ROLES.get()
        val originalRequestId = REQUEST_ID.get()
        val originalSessionId = SESSION_ID.get()
        
        return try {
            setTenantId(tenantId)
            setUserId(userId)
            setUserRoles(roles)
            setRequestId(requestId)
            setSessionId(sessionId)
            block()
        } finally {
            // Restore original context
            if (originalTenantId != null) TENANT_ID.set(originalTenantId) else TENANT_ID.remove()
            if (originalUserId != null) USER_ID.set(originalUserId) else USER_ID.remove()
            if (originalRoles != null) USER_ROLES.set(originalRoles) else USER_ROLES.remove()
            if (originalRequestId != null) REQUEST_ID.set(originalRequestId) else REQUEST_ID.remove()
            if (originalSessionId != null) SESSION_ID.set(originalSessionId) else SESSION_ID.remove()
        }
    }
    
    /**
     * Validate that tenant context is properly set
     */
    fun validateContext() {
        val tenantId = getTenantId()
        if (tenantId.isBlank() || tenantId == DEFAULT_TENANT) {
            logger.warn("Invalid tenant context: {}", tenantId)
            throw IllegalStateException("Valid tenant context is required")
        }
    }
    
    /**
     * Get context as map for logging/debugging
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "tenantId" to getTenantId(),
            "userId" to getUserId(),
            "roles" to getUserRoles(),
            "requestId" to getRequestId(),
            "sessionId" to getSessionId(),
            "isSystemUser" to isSystemUser(),
            "hasTenantContext" to hasTenantContext(),
            "hasUserContext" to hasUserContext()
        )
    }
}