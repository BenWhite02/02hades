// =============================================================================

// File: src/main/kotlin/com/kairos/hades/multitenancy/TenantSchemaResolver.kt
// 🔥 HADES Tenant Schema Resolver
// Resolves database schema based on tenant context

package com.kairos.hades.multitenancy

import com.kairos.hades.config.HadesProperties
import org.hibernate.cfg.AvailableSettings
import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Resolves the current tenant identifier for Hibernate multi-tenancy
 */
@Component
class TenantSchemaResolver @Autowired constructor(
    private val tenantContext: TenantContext,
    private val hadesProperties: HadesProperties
) : CurrentTenantIdentifierResolver<String> {
    
    override fun resolveCurrentTenantIdentifier(): String {
        val tenantId = tenantContext.getTenantId()
        
        return when (hadesProperties.multitenancy.isolationLevel) {
            "SCHEMA" -> "schema_$tenantId"
            "DATABASE" -> "db_$tenantId"
            else -> tenantId
        }
    }
    
    override fun validateExistingCurrentSessions(): Boolean {
        return false
    }
    
    override fun isRoot(tenantId: String?): Boolean {
        return tenantId == null || tenantId == TenantContext.DEFAULT_TENANT_ID
    }
}