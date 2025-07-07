// =============================================================================
// File: src/main/kotlin/com/kairos/hades/multitenancy/TenantInterceptor.kt
// 🔥 HADES Tenant Interceptor
// Author: Sankhadeep Banerjee
// HTTP interceptor for automatic tenant resolution and context setup
// =============================================================================

package com.kairos.hades.multitenancy

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import java.util.*

/**
 * Interceptor to automatically resolve and set tenant context from HTTP requests
 * Supports multiple tenant resolution strategies
 */
@Component
class TenantInterceptor @Autowired constructor(
    private val tenantContext: TenantContext,
    private val tenantResolver: TenantResolver
) : HandlerInterceptor {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantInterceptor::class.java)
        
        // MDC keys for logging
        const val MDC_TENANT_ID = "tenantId"
        const val MDC_USER_ID = "userId"
        const val MDC_REQUEST_ID = "requestId"
        const val MDC_SESSION_ID = "sessionId"
        
        // Request attributes
        const val ATTR_TENANT_ID = "hades.tenantId"
        const val ATTR_USER_ID = "hades.userId"
        const val ATTR_REQUEST_ID = "hades.requestId"
    }
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        try {
            // Generate or extract request ID for tracing
            val requestId = extractRequestId(request)
            tenantContext.setRequestId(requestId)
            
            // Resolve tenant from request
            val tenantId = tenantResolver.resolveTenant(request)
            if (tenantId.isNullOrBlank()) {
                logger.warn("Could not resolve tenant ID from request: {}", request.requestURI)
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant identification required")
                return false
            }
            
            // Set tenant context
            tenantContext.setTenantId(tenantId)
            
            // Try to resolve user context if available
            val userInfo = extractUserInfo(request)
            if (userInfo != null) {
                tenantContext.setUserContext(
                    userInfo.userId,
                    userInfo.roles,
                    userInfo.sessionId
                )
            }
            
            // Set request attributes for downstream use
            request.setAttribute(ATTR_TENANT_ID, tenantId)
            request.setAttribute(ATTR_USER_ID, tenantContext.getUserId())
            request.setAttribute(ATTR_REQUEST_ID, requestId)
            
            // Setup MDC for structured logging
            setupMDC()
            
            logger.debug("Tenant context established: {}", tenantContext.getContextString())
            
            return true
            
        } catch (e: Exception) {
            logger.error("Error setting up tenant context", e)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error")
            return false
        }
    }
    
    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        // Add tenant information to response headers if needed
        response.setHeader("X-Tenant-ID", tenantContext.getTenantId())
        response.setHeader("X-Request-ID", tenantContext.getRequestId())
        
        logger.debug("Request completed for tenant: {}", tenantContext.getTenantId())
    }
    
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        try {
            // Log any exceptions with tenant context
            if (ex != null) {
                logger.error("Request failed for tenant: {} - {}", 
                    tenantContext.getTenantId(), ex.message, ex)
            }
            
            // Clear MDC
            clearMDC()
            
            // Clear tenant context to prevent memory leaks
            tenantContext.clear()
            
        } catch (e: Exception) {
            logger.error("Error cleaning up tenant context", e)
        }
    }
    
    /**
     * Extract or generate request ID for tracing
     */
    private fun extractRequestId(request: HttpServletRequest): String {
        // Try common request ID headers
        val headers = listOf(
            "X-Request-ID",
            "X-Correlation-ID", 
            "X-Trace-ID",
            "Request-ID"
        )
        
        for (header in headers) {
            val value = request.getHeader(header)
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        
        // Generate new request ID
        return UUID.randomUUID().toString()
    }
    
    /**
     * Extract user information from request
     */
    private fun extractUserInfo(request: HttpServletRequest): UserInfo? {
        return try {
            // Extract from JWT token if present
            val authHeader = request.getHeader("Authorization")
            if (authHeader?.startsWith("Bearer ") == true) {
                val token = authHeader.substring(7)
                parseJwtToken(token)
            } else {
                // Extract from session if available
                extractFromSession(request)
            }
        } catch (e: Exception) {
            logger.debug("Could not extract user info from request", e)
            null
        }
    }
    
    /**
     * Parse JWT token to extract user information
     */
    private fun parseJwtToken(token: String): UserInfo? {
        // This would be implemented with your JWT library
        // For now, returning null as placeholder
        // TODO: Implement JWT parsing
        return null
    }
    
    /**
     * Extract user info from HTTP session
     */
    private fun extractFromSession(request: HttpServletRequest): UserInfo? {
        return try {
            val session = request.getSession(false)
            if (session != null) {
                val userId = session.getAttribute("userId") as? String
                val roles = session.getAttribute("roles") as? Set<String>
                val sessionId = session.id
                
                if (userId != null) {
                    UserInfo(userId, roles ?: emptySet(), sessionId)
                } else null
            } else null
        } catch (e: Exception) {
            logger.debug("Error extracting user info from session", e)
            null
        }
    }
    
    /**
     * Setup MDC (Mapped Diagnostic Context) for structured logging
     */
    private fun setupMDC() {
        MDC.put(MDC_TENANT_ID, tenantContext.getTenantId())
        MDC.put(MDC_REQUEST_ID, tenantContext.getRequestId())
        
        tenantContext.getUserId()?.let { 
            MDC.put(MDC_USER_ID, it)
        }
        
        tenantContext.getSessionId()?.let {
            MDC.put(MDC_SESSION_ID, it)
        }
    }
    
    /**
     * Clear MDC to prevent memory leaks
     */
    private fun clearMDC() {
        MDC.remove(MDC_TENANT_ID)
        MDC.remove(MDC_USER_ID)
        MDC.remove(MDC_REQUEST_ID)
        MDC.remove(MDC_SESSION_ID)
    }
    
    /**
     * Data class for user information
     */
    data class UserInfo(
        val userId: String,
        val roles: Set<String>,
        val sessionId: String?
    )
}

// =============================================================================
// File: src/main/kotlin/com/kairos/hades/multitenancy/TenantResolver.kt
// 🔥 HADES Tenant Resolver
// Author: Sankhadeep Banerjee
// Multiple strategies for resolving tenant from HTTP requests
// =============================================================================

package com.kairos.hades.multitenancy

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Resolves tenant ID from HTTP requests using multiple strategies
 */
@Component
class TenantResolver {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantResolver::class.java)
        
        // Header names for tenant identification
        const val HEADER_TENANT_ID = "X-Tenant-ID"
        const val HEADER_TENANT_SLUG = "X-Tenant-Slug"
        const val HEADER_API_KEY = "X-API-Key"
        
        // URL patterns
        const val TENANT_PATH_PATTERN = "^/api/v1/tenants/([^/]+)/.*"
        const val SUBDOMAIN_PATTERN = "^([^.]+)\\."
    }
    
    /**
     * Resolve tenant ID from HTTP request using multiple strategies
     */
    fun resolveTenant(request: HttpServletRequest): String? {
        // Strategy 1: Direct tenant ID header
        request.getHeader(HEADER_TENANT_ID)?.let { tenantId ->
            if (tenantId.isNotBlank()) {
                logger.debug("Resolved tenant from header {}: {}", HEADER_TENANT_ID, tenantId)
                return tenantId
            }
        }
        
        // Strategy 2: Tenant slug header (would need to resolve to ID)
        request.getHeader(HEADER_TENANT_SLUG)?.let { slug ->
            if (slug.isNotBlank()) {
                val tenantId = resolveTenantBySlug(slug)
                if (tenantId != null) {
                    logger.debug("Resolved tenant from slug header: {} -> {}", slug, tenantId)
                    return tenantId
                }
            }
        }
        
        // Strategy 3: Extract from URL path
        val pathTenantId = extractTenantFromPath(request.requestURI)
        if (pathTenantId != null) {
            logger.debug("Resolved tenant from URL path: {}", pathTenantId)
            return pathTenantId
        }
        
        // Strategy 4: Extract from subdomain
        val subdomainTenantId = extractTenantFromSubdomain(request)
        if (subdomainTenantId != null) {
            logger.debug("Resolved tenant from subdomain: {}", subdomainTenantId)
            return subdomainTenantId
        }
        
        // Strategy 5: Resolve from API key
        request.getHeader(HEADER_API_KEY)?.let { apiKey ->
            if (apiKey.isNotBlank()) {
                val tenantId = resolveTenantByApiKey(apiKey)
                if (tenantId != null) {
                    logger.debug("Resolved tenant from API key")
                    return tenantId
                }
            }
        }
        
        // Strategy 6: Default tenant for system/health endpoints
        if (isSystemEndpoint(request.requestURI)) {
            logger.debug("Using system tenant for system endpoint: {}", request.requestURI)
            return "system"
        }
        
        logger.warn("Could not resolve tenant from request: {} {}", 
            request.method, request.requestURI)
        return null
    }
    
    /**
     * Extract tenant ID from URL path
     * Expects pattern: /api/v1/tenants/{tenantId}/...
     */
    private fun extractTenantFromPath(uri: String): String? {
        return try {
            val regex = Regex(TENANT_PATH_PATTERN)
            val matchResult = regex.find(uri)
            matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.debug("Error extracting tenant from path: {}", uri, e)
            null
        }
    }
    
    /**
     * Extract tenant from subdomain
     * Expects pattern: {tenant}.domain.com
     */
    private fun extractTenantFromSubdomain(request: HttpServletRequest): String? {
        return try {
            val host = request.getHeader("Host") ?: return null
            val regex = Regex(SUBDOMAIN_PATTERN)
            val matchResult = regex.find(host)
            val subdomain = matchResult?.groupValues?.get(1)
            
            // Filter out common non-tenant subdomains
            if (subdomain != null && !isSystemSubdomain(subdomain)) {
                // Would resolve subdomain to tenant ID via database lookup
                resolveTenantBySubdomain(subdomain)
            } else null
        } catch (e: Exception) {
            logger.debug("Error extracting tenant from subdomain", e)
            null
        }
    }
    
    /**
     * Check if subdomain is a system subdomain (not a tenant)
     */
    private fun isSystemSubdomain(subdomain: String): Boolean {
        val systemSubdomains = setOf(
            "www", "api", "admin", "app", "dashboard", 
            "docs", "status", "health", "dev", "staging"
        )
        return systemSubdomains.contains(subdomain.lowercase())
    }
    
    /**
     * Check if endpoint is a system endpoint that doesn't require tenant
     */
    private fun isSystemEndpoint(uri: String): Boolean {
        val systemPaths = setOf(
            "/actuator", "/health", "/metrics", "/info",
            "/swagger", "/api-docs", "/favicon.ico"
        )
        return systemPaths.any { uri.startsWith(it) }
    }
    
    /**
     * Resolve tenant ID by slug (placeholder - would query database)
     */
    private fun resolveTenantBySlug(slug: String): String? {
        // TODO: Implement database lookup
        // return tenantRepository.findBySlug(slug)?.id
        return null
    }
    
    /**
     * Resolve tenant ID by subdomain (placeholder - would query database)
     */
    private fun resolveTenantBySubdomain(subdomain: String): String? {
        // TODO: Implement database lookup
        // return applicationRepository.findBySubdomain(subdomain)?.tenantId
        return null
    }
    
    /**
     * Resolve tenant ID by API key (placeholder - would query database)
     */
    private fun resolveTenantByApiKey(apiKey: String): String? {
        // TODO: Implement database lookup
        // return apiKeyRepository.findByKey(apiKey)?.tenantId
        return null
    }
}