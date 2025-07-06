// =============================================================================

// File: src/main/kotlin/com/kairos/hades/config/MultiTenancyConfig.kt
// 🔥 HADES Multi-Tenancy Configuration
// Spring configuration for multi-tenant setup

package com.kairos.hades.config

import com.kairos.hades.multitenancy.TenantAwareAuditorProvider
import com.kairos.hades.multitenancy.TenantInterceptor
import com.kairos.hades.multitenancy.TenantSchemaResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Configuration for multi-tenancy support
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "tenantAwareAuditorProvider")
class MultiTenancyConfig @Autowired constructor(
    private val tenantInterceptor: TenantInterceptor,
    private val hadesProperties: HadesProperties
) : WebMvcConfigurer {
    
    override fun addInterceptors(registry: InterceptorRegistry) {
        if (hadesProperties.multitenancy.enabled) {
            registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/v*/health",
                    "/api/v*/actuator/**",
                    "/api/v*/swagger-ui/**",
                    "/api/v*/v3/api-docs/**"
                )
        }
    }
    
    @Bean
    fun tenantAwareAuditorProvider(): AuditorAware<String> {
        return TenantAwareAuditorProvider(tenantContext())
    }
    
    @Bean
    fun tenantContext(): com.kairos.hades.multitenancy.TenantContext {
        return com.kairos.hades.multitenancy.TenantContext()
    }
}