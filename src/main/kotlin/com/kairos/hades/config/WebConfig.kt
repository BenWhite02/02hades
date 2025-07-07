// =============================================================================
// File: src/main/kotlin/com/kairos/hades/config/WebConfig.kt
// 🔥 HADES Web Configuration
// Author: Sankhadeep Banerjee
// Web MVC configuration including CORS, interceptors, and converters
// =============================================================================

package com.kairos.hades.config

import com.kairos.hades.multitenancy.TenantInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Web MVC configuration for Hades backend
 * Configures CORS, interceptors, message converters, and other web settings
 */
@Configuration
@EnableWebMvc
class WebConfig @Autowired constructor(
    private val tenantInterceptor: TenantInterceptor
) : WebMvcConfigurer {
    
    @Value("\${hades.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private lateinit var allowedOrigins: String
    
    @Value("\${hades.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private lateinit var allowedMethods: String
    
    @Value("\${hades.security.cors.allowed-headers:*}")
    private lateinit var allowedHeaders: String
    
    @Value("\${hades.security.cors.allow-credentials:true}")
    private var allowCredentials: Boolean = true
    
    @Value("\${hades.security.cors.max-age:3600}")
    private var maxAge: Long = 3600
    
    // ==========================================================================
    // CORS CONFIGURATION
    // ==========================================================================
    
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(*allowedOrigins.split(",").map { it.trim() }.toTypedArray())
            .allowedMethods(*allowedMethods.split(",").map { it.trim() }.toTypedArray())
            .allowedHeaders(*allowedHeaders.split(",").map { it.trim() }.toTypedArray())
            .allowCredentials(allowCredentials)
            .maxAge(maxAge)
        
        // Additional CORS mapping for actuator endpoints
        registry.addMapping("/actuator/**")
            .allowedOriginPatterns(*allowedOrigins.split(",").map { it.trim() }.toTypedArray())
            .allowedMethods("GET", "POST")
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(maxAge)
    }
    
    // ==========================================================================
    // INTERCEPTOR CONFIGURATION
    // ==========================================================================
    
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tenantInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/health",
                "/api/actuator/**",
                "/api/swagger-ui/**",
                "/api/v3/api-docs/**"
            )
            .order(1)
    }
    
    // ==========================================================================
    // MESSAGE CONVERTER CONFIGURATION
    // ==========================================================================
    
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(mappingJackson2HttpMessageConverter())
    }
    
    @Bean
    fun mappingJackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter {
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = objectMapper()
        return converter
    }
    
    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // Register modules
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
            
            // Configure property naming
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            
            // Configure serialization
            configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, false)
            
            // Configure deserialization
            configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            
            // Configure mapper features
            configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        }
    }
    
    // ==========================================================================
    // RESOURCE HANDLING
    // ==========================================================================
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Swagger UI resources
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
            .setCachePeriod(0)
        
        // Static content
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(31536000) // 1 year cache
        
        // Favicon
        registry.addResourceHandler("/favicon.ico")
            .addResourceLocations("classpath:/static/favicon.ico")
            .setCachePeriod(31536000)
    }
    
    // ==========================================================================
    // VIEW CONTROLLER CONFIGURATION
    // ==========================================================================
    
    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Redirect root to API documentation
        registry.addRedirectViewController("/", "/swagger-ui/index.html")
        registry.addRedirectViewController("/docs", "/swagger-ui/index.html")
        
        // Health check endpoint
        registry.addViewController("/health").setViewName("forward:/actuator/health")
    }
    
    // ==========================================================================
    // CONTENT NEGOTIATION
    // ==========================================================================
    
    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer
            .favorParameter(false)
            .favorPathExtension(false)
            .ignoreAcceptHeader(false)
            .defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .mediaType("json", org.springframework.http.MediaType.APPLICATION_JSON)
            .mediaType("xml", org.springframework.http.MediaType.APPLICATION_XML)
    }
    
    // ==========================================================================
    // PATH MATCHING CONFIGURATION
    // ==========================================================================
    
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer
            .setUseTrailingSlashMatch(false)
            .setUseSuffixPatternMatch(false)
    }
}

// =============================================================================
// File: src/main/kotlin/com/kairos/hades/config/CacheConfig.kt
// 🔥 HADES Cache Configuration
// Author: Sankhadeep Banerjee
// Cache configuration for atoms, tenants, and other data
// =============================================================================

package com.kairos.hades.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Duration

/**
 * Cache configuration for Hades application
 * Configures in-memory caching for frequently accessed data
 */
@Configuration
@EnableCaching
class CacheConfig {
    
    @Value("\${hades.atoms.cache.enabled:true}")
    private var cacheEnabled: Boolean = true
    
    @Value("\${hades.atoms.cache.default-ttl:3600}")
    private var defaultTtl: Long = 3600
    
    /**
     * Primary cache manager for the application
     */
    @Bean
    @Primary
    fun cacheManager(): CacheManager {
        return if (cacheEnabled) {
            ConcurrentMapCacheManager().apply {
                setCacheNames(listOf(
                    "atoms",           // EligibilityAtom cache
                    "atom-results",    // Atom execution results
                    "tenants",         // Tenant information
                    "tenant-stats",    // Tenant statistics
                    "dependencies",    // Atom dependencies
                    "validations"      // Validation results
                ))
                setAllowNullValues(false)
            }
        } else {
            // No-op cache manager when caching is disabled
            NoOpCacheManager()
        }
    }
}

/**
 * No-operation cache manager for when caching is disabled
 */
class NoOpCacheManager : CacheManager {
    override fun getCache(name: String) = null
    override fun getCacheNames() = emptyList<String>()
}

// =============================================================================
// File: src/main/kotlin/com/kairos/hades/config/ThreadPoolConfig.kt
// 🔥 HADES Thread Pool Configuration
// Author: Sankhadeep Banerjee
// Thread pool configuration for async operations and atom execution
// =============================================================================

package com.kairos.hades.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.Executor

/**
 * Thread pool configuration for asynchronous operations
 */
@Configuration
@EnableAsync
class ThreadPoolConfig {
    
    @Value("\${hades.decision-engine.thread-pool-size:10}")
    private var corePoolSize: Int = 10
    
    @Value("\${hades.decision-engine.queue-capacity:1000}")
    private var queueCapacity: Int = 1000
    
    /**
     * Thread pool executor for atom execution
     */
    @Bean("atomExecutorService")
    fun atomExecutorService(): ThreadPoolExecutor {
        val executor = ThreadPoolTaskExecutor()
        
        executor.corePoolSize = corePoolSize
        executor.maxPoolSize = corePoolSize * 2
        executor.queueCapacity = queueCapacity
        executor.threadNamePrefix = "atom-exec-"
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()
        
        return executor.threadPoolExecutor
    }
    
    /**
     * General async task executor
     */
    @Bean("taskExecutor")
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        
        executor.corePoolSize = 5
        executor.maxPoolSize = 10
        executor.queueCapacity = 100
        executor.threadNamePrefix = "async-task-"
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        
        return executor
    }
}