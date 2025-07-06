// =============================================================================

// File: src/main/kotlin/com/kairos/hades/multitenancy/TenantInterceptor.kt
// 🔥 HADES Tenant Interceptor
// HTTP interceptor to extract and set tenant context from requests

package com.kairos.hades.multitenancy

import com.kairos.hades.config.HadesProperties
import com.kairos.hades.exception.TenantNotFoundException
import com.kairos.hades.service.TenantService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

/**
 * Interceptor to handle tenant context for incoming HTTP requests
 */
@Component
class TenantInterceptor @Autowired constructor(
    private val tenantContext: TenantContext,
    private val tenantService: TenantService,
    private val hadesProperties: HadesProperties
) : HandlerInterceptor {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantInterceptor::class.java)
        private val EXCLUDED_PATHS = setOf(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/health",
            "/info",
            "/metrics"
        )
    }
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val requestURI = request.requestURI
        
        // Skip tenant resolution for excluded paths
        if (EXCLUDED_PATHS.any { requestURI.startsWith(it) }) {
            tenantContext.setTenantId(TenantContext.SYSTEM_TENANT_ID)
            return true
        }
        
        try {
            val tenantId = resolveTenantId(request)
            
            // Validate tenant exists and is active
            if (hadesProperties.multitenancy.enabled && tenantId != TenantContext.SYSTEM_TENANT_ID) {
                val tenant = tenantService.findByIdOrSlug(tenantId)
                if (tenant == null || !tenant.isActive()) {
                    logger.warn("Invalid or inactive tenant: {}", tenantId)
                    throw TenantNotFoundException("Tenant '$tenantId' not found or inactive")
                }
            }
            
            tenantContext.setTenantId(tenantId)
            
            // Also extract user ID if available (from JWT token, etc.)
            val userId = extractUserId(request)
            if (userId != null) {
                tenantContext.setUserId(userId)
            }
            
            logger.debug("Set tenant context for request: tenantId={}, userId={}, path={}", 
                tenantId, userId, requestURI)
            
            return true
            
        } catch (exception: Exception) {
            logger.error("Failed to resolve tenant context for request: {}", requestURI, exception)
            response.status = HttpServletResponse.SC_BAD_REQUEST
            response.writer.write("""{"error": "Invalid tenant context", "message": "${exception.message}"}""")
            return false
        }
    }
    
    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        // Add tenant ID to response headers for debugging
        response.setHeader("X-Tenant-ID", tenantContext.getTenantId())
    }
    
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        // Clear tenant context after request completion
        tenantContext.clearContext()
        logger.debug("Cleared tenant context after request completion")
    }
    
    /**
     * Resolve tenant ID from request
     * Priority: Header > Subdomain > Default
     */
    private fun resolveTenantId(request: HttpServletRequest): String {
        // 1. Check X-Tenant-ID header
        val headerTenantId = request.getHeader(hadesProperties.multitenancy.headerName)
        if (!headerTenantId.isNullOrBlank()) {
            logger.debug("Resolved tenant from header: {}", headerTenantId)
            return headerTenantId.trim()
        }
        
        // 2. Check subdomain (e.g., tenant1.kairos.app -> tenant1)
        val host = request.getHeader("Host") ?: request.serverName
        if (host != null) {
            val subdomainTenant = extractTenantFromSubdomain(host)
            if (subdomainTenant != null) {
                logger.debug("Resolved tenant from subdomain: {}", subdomainTenant)
                return subdomainTenant
            }
        }
        
        // 3. Check path parameter (e.g., /api/v1/tenant1/...)
        val pathTenant = extractTenantFromPath(request.requestURI)
        if (pathTenant != null) {
            logger.debug("Resolved tenant from path: {}", pathTenant)
            return pathTenant
        }
        
        // 4. Fall back to default tenant
        val defaultTenant = hadesProperties.multitenancy.defaultTenant
        logger.debug("Using default tenant: {}", defaultTenant)
        return defaultTenant
    }
    
    /**
     * Extract tenant ID from subdomain
     */
    private fun extractTenantFromSubdomain(host: String): String? {
        val parts = host.lowercase().split(".")
        
        // For subdomains like tenant1.kairos.app, extract tenant1
        if (parts.size >= 3 && !parts[0].equals("www", ignoreCase = true)) {
            val subdomain = parts[0]
            
            // Validate subdomain format (alphanumeric and hyphens only)
            if (subdomain.matches(Regex("^[a-z0-9-]+$")) && subdomain.length in 3..50) {
                return subdomain
            }
        }
        
        return null
    }
    
    /**
     * Extract tenant ID from request path
     */
    private fun extractTenantFromPath(requestURI: String): String? {
        // Pattern: /api/v1/tenant/{tenantId}/...
        val pathPattern = Regex("/api/v[0-9]+/tenant/([a-z0-9-]+)")
        val matchResult = pathPattern.find(requestURI)
        
        return matchResult?.groupValues?.get(1)
    }
    
    /**
     * Extract user ID from request (JWT token, session, etc.)
     */
    private fun extractUserId(request: HttpServletRequest): String? {
        // Implementation depends on authentication mechanism
        // This is a placeholder - actual implementation would extract from JWT or session
        
        // Check for user ID in custom header
        val userIdHeader = request.getHeader("X-User-ID")
        if (!userIdHeader.isNullOrBlank()) {
            return userIdHeader.trim()
        }
        
        // TODO: Extract from JWT token
        // val jwtToken = extractJwtFromRequest(request)
        // return extractUserIdFromJwt(jwtToken)
        
        return null
    }
}