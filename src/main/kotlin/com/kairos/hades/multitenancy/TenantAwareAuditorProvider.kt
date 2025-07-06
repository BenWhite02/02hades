// =============================================================================

// File: src/main/kotlin/com/kairos/hades/multitenancy/TenantAwareAuditorProvider.kt
// 🔥 HADES Tenant-Aware Auditor Provider
// Provides current user for JPA auditing with tenant context

package com.kairos.hades.multitenancy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component
import java.util.*

/**
 * Provides the current auditor (user) for JPA auditing
 * Integrates with tenant context to provide proper user identification
 */
@Component
class TenantAwareAuditorProvider @Autowired constructor(
    private val tenantContext: TenantContext
) : AuditorAware<String> {
    
    override fun getCurrentAuditor(): Optional<String> {
        // Try to get user ID from tenant context
        val userId = tenantContext.getUserId()
        
        return if (userId != null) {
            Optional.of("${tenantContext.getTenantId()}:$userId")
        } else {
            // Fallback to system user
            Optional.of("${tenantContext.getTenantId()}:system")
        }
    }
}