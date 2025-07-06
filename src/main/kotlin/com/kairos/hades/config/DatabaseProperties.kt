// =============================================================================

// File: src/main/kotlin/com/kairos/hades/config/DatabaseProperties.kt
// 🔥 HADES Database Configuration Properties
// Database-specific configuration settings

package com.kairos.hades.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.stereotype.Component
import jakarta.validation.constraints.*

/**
 * Database configuration properties
 */
@Component
@ConfigurationProperties(prefix = "hades.database")
data class DatabaseProperties(
    
    @NestedConfigurationProperty
    val pool: ConnectionPoolProperties = ConnectionPoolProperties(),
    
    @NestedConfigurationProperty
    val cache: DatabaseCacheProperties = DatabaseCacheProperties(),
    
    @NestedConfigurationProperty
    val performance: PerformanceProperties = PerformanceProperties(),
    
    @NestedConfigurationProperty
    val monitoring: MonitoringProperties = MonitoringProperties()
)

/**
 * Connection pool configuration
 */
data class ConnectionPoolProperties(
    @Min(1)
    @Max(100)
    val maxSize: Int = 20,
    
    @Min(0)
    @Max(50)
    val minIdle: Int = 5,
    
    @Min(30000) // 30 seconds minimum
    @Max(1800000) // 30 minutes maximum
    val idleTimeout: Long = 300000, // 5 minutes
    
    @Min(600000) // 10 minutes minimum
    @Max(7200000) // 2 hours maximum
    val maxLifetime: Long = 1800000, // 30 minutes
    
    @Min(1000) // 1 second minimum
    @Max(60000) // 1 minute maximum
    val connectionTimeout: Long = 30000, // 30 seconds
    
    @Min(0) // 0 = disabled
    @Max(300000) // 5 minutes maximum
    val leakDetectionThreshold: Long = 60000 // 1 minute
)

/**
 * Database cache configuration
 */
data class DatabaseCacheProperties(
    val secondLevelCacheEnabled: Boolean = true,
    val queryCacheEnabled: Boolean = true,
    
    @Min(100)
    @Max(100000)
    val maxEntries: Int = 10000,
    
    @Min(300) // 5 minutes minimum
    @Max(86400) // 24 hours maximum
    val ttlSeconds: Long = 3600, // 1 hour
    
    val statisticsEnabled: Boolean = true
)

/**
 * Database performance configuration
 */
data class PerformanceProperties(
    @Min(1)
    @Max(1000)
    val batchSize: Int = 25,
    
    val batchVersionedData: Boolean = true,
    val orderInserts: Boolean = true,
    val orderUpdates: Boolean = true,
    
    @Min(1)
    @Max(1000)
    val defaultBatchFetchSize: Int = 16,
    
    val useScrollableResultSet: Boolean = true,
    val useGetGeneratedKeys: Boolean = true
)

/**
 * Database monitoring configuration
 */
data class MonitoringProperties(
    val slowQueryThresholdMs: Long = 1000, // 1 second
    val logSlowQueries: Boolean = true,
    val logStatistics: Boolean = false,
    val metricsEnabled: Boolean = true,
    
    @Min(10)
    @Max(3600)
    val statisticsIntervalSeconds: Int = 60 // 1 minute
)