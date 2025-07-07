// =============================================================================
// File: src/main/kotlin/com/kairos/hades/repository/TenantRepository.kt
// 🔥 HADES Tenant Repository
// Author: Sankhadeep Banerjee
// Data access layer for Tenant entities with subscription management
// =============================================================================

package com.kairos.hades.repository

import com.kairos.hades.entity.Tenant
import com.kairos.hades.enums.SubscriptionTier
import com.kairos.hades.enums.TenantStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Repository interface for Tenant entities
 * Provides data access methods for tenant management
 */
@Repository
interface TenantRepository : JpaRepository<Tenant, UUID> {
    
    // ==========================================================================
    // BASIC FIND METHODS
    // ==========================================================================
    
    /**
     * Find tenant by slug (unique identifier)
     */
    fun findBySlugAndDeletedFalse(slug: String): Tenant?
    
    /**
     * Find tenant by contact email
     */
    fun findByContactEmailAndDeletedFalse(contactEmail: String): Tenant?
    
    /**
     * Find all active tenants
     */
    fun findByStatusAndDeletedFalse(status: TenantStatus, pageable: Pageable): Page<Tenant>
    
    /**
     * Find tenants by subscription tier
     */
    fun findBySubscriptionTierAndDeletedFalse(
        subscriptionTier: SubscriptionTier, 
        pageable: Pageable
    ): Page<Tenant>
    
    /**
     * Find tenants by multiple statuses
     */
    fun findByStatusInAndDeletedFalse(
        statuses: List<TenantStatus>, 
        pageable: Pageable
    ): Page<Tenant>
    
    // ==========================================================================
    // SEARCH AND FILTER METHODS
    // ==========================================================================
    
    /**
     * Search tenants by name or contact email
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
             OR LOWER(t.contactEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY t.name ASC
    """)
    fun searchByNameEmailOrSlug(
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<Tenant>
    
    /**
     * Find tenants with filters
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND (:status IS NULL OR t.status = :status)
        AND (:tier IS NULL OR t.subscriptionTier = :tier)
        AND (:searchTerm IS NULL OR 
             LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR 
             LOWER(t.contactEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY t.createdAt DESC
    """)
    fun findWithFilters(
        @Param("status") status: TenantStatus?,
        @Param("tier") tier: SubscriptionTier?,
        @Param("searchTerm") searchTerm: String?,
        pageable: Pageable
    ): Page<Tenant>
    
    // ==========================================================================
    // SUBSCRIPTION AND BILLING
    // ==========================================================================
    
    /**
     * Find tenants with expiring trials
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status = 'TRIAL'
        AND t.trialEndDate BETWEEN :startDate AND :endDate
        ORDER BY t.trialEndDate ASC
    """)
    fun findTenantsWithExpiringTrials(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Tenant>
    
    /**
     * Find tenants with expired subscriptions
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.subscriptionEndDate < :currentDate
        AND t.status NOT IN ('CANCELLED', 'ARCHIVED')
        ORDER BY t.subscriptionEndDate ASC
    """)
    fun findTenantsWithExpiredSubscriptions(
        @Param("currentDate") currentDate: LocalDateTime
    ): List<Tenant>
    
    /**
     * Find tenants approaching storage limit
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status = 'ACTIVE'
        AND (t.currentStorageBytes * 1.0 / t.maxStorageBytes) >= :threshold
        ORDER BY (t.currentStorageBytes * 1.0 / t.maxStorageBytes) DESC
    """)
    fun findTenantsApproachingStorageLimit(
        @Param("threshold") threshold: Double
    ): List<Tenant>
    
    /**
     * Find tenants approaching API limit
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status = 'ACTIVE'
        AND (t.currentApiRequestsMonth * 1.0 / t.maxApiRequestsPerMonth) >= :threshold
        ORDER BY (t.currentApiRequestsMonth * 1.0 / t.maxApiRequestsPerMonth) DESC
    """)
    fun findTenantsApproachingApiLimit(
        @Param("threshold") threshold: Double
    ): List<Tenant>
    
    /**
     * Find tenants requiring payment
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status = 'PAYMENT_REQUIRED'
        AND t.subscriptionEndDate < :gracePeriodEnd
        ORDER BY t.subscriptionEndDate ASC
    """)
    fun findTenantsRequiringPayment(
        @Param("gracePeriodEnd") gracePeriodEnd: LocalDateTime
    ): List<Tenant>
    
    // ==========================================================================
    // STATISTICS AND ANALYTICS
    // ==========================================================================
    
    /**
     * Count tenants by status
     */
    fun countByStatusAndDeletedFalse(status: TenantStatus): Long
    
    /**
     * Count tenants by subscription tier
     */
    fun countBySubscriptionTierAndDeletedFalse(subscriptionTier: SubscriptionTier): Long
    
    /**
     * Get tenant statistics summary
     */
    @Query("""
        SELECT 
            COUNT(t) as totalTenants,
            COUNT(CASE WHEN t.status = 'ACTIVE' THEN 1 END) as activeTenants,
            COUNT(CASE WHEN t.status = 'TRIAL' THEN 1 END) as trialTenants,
            COUNT(CASE WHEN t.onboardingCompleted = true THEN 1 END) as completedOnboarding,
            AVG(t.currentStorageBytes) as avgStorageUsage,
            AVG(t.currentApiRequestsMonth) as avgApiUsage
        FROM Tenant t 
        WHERE t.deleted = false
    """)
    fun getTenantStatistics(): Map<String, Any>
    
    /**
     * Get subscription tier distribution
     */
    @Query("""
        SELECT t.subscriptionTier as tier, COUNT(t) as count
        FROM Tenant t 
        WHERE t.deleted = false
        GROUP BY t.subscriptionTier
        ORDER BY t.subscriptionTier
    """)
    fun getSubscriptionTierDistribution(): List<Map<String, Any>>
    
    /**
     * Get monthly tenant growth
     */
    @Query("""
        SELECT 
            EXTRACT(YEAR FROM t.createdAt) as year,
            EXTRACT(MONTH FROM t.createdAt) as month,
            COUNT(t) as newTenants
        FROM Tenant t 
        WHERE t.deleted = false
        AND t.createdAt >= :startDate
        GROUP BY EXTRACT(YEAR FROM t.createdAt), EXTRACT(MONTH FROM t.createdAt)
        ORDER BY year DESC, month DESC
    """)
    fun getMonthlyGrowth(@Param("startDate") startDate: LocalDateTime): List<Map<String, Any>>
    
    /**
     * Find most active tenants by API usage
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status = 'ACTIVE'
        AND t.currentApiRequestsMonth > 0
        ORDER BY t.currentApiRequestsMonth DESC
    """)
    fun findMostActiveTenants(pageable: Pageable): List<Tenant>
    
    /**
     * Find tenants with highest storage usage
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status = 'ACTIVE'
        AND t.currentStorageBytes > 0
        ORDER BY t.currentStorageBytes DESC
    """)
    fun findTenantsWithHighestStorageUsage(pageable: Pageable): List<Tenant>
    
    // ==========================================================================
    // ONBOARDING AND LIFECYCLE
    // ==========================================================================
    
    /**
     * Find tenants with incomplete onboarding
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.onboardingCompleted = false
        AND t.createdAt >= :cutoffDate
        ORDER BY t.createdAt DESC
    """)
    fun findTenantsWithIncompleteOnboarding(
        @Param("cutoffDate") cutoffDate: LocalDateTime
    ): List<Tenant>
    
    /**
     * Find inactive tenants (no recent activity)
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status IN ('ACTIVE', 'TRIAL')
        AND (t.lastActivityAt IS NULL OR t.lastActivityAt < :inactivityThreshold)
        ORDER BY t.lastActivityAt ASC NULLS FIRST
    """)
    fun findInactiveTenants(
        @Param("inactivityThreshold") inactivityThreshold: LocalDateTime,
        pageable: Pageable
    ): Page<Tenant>
    
    /**
     * Find tenants created in date range
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.createdAt BETWEEN :startDate AND :endDate
        ORDER BY t.createdAt DESC
    """)
    fun findTenantsCreatedBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Tenant>
    
    // ==========================================================================
    // MAINTENANCE AND UPDATES
    // ==========================================================================
    
    /**
     * Update last activity timestamp
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.lastActivityAt = :timestamp
        WHERE t.id = :tenantId
    """)
    fun updateLastActivity(
        @Param("tenantId") tenantId: UUID,
        @Param("timestamp") timestamp: LocalDateTime
    )
    
    /**
     * Update API usage for tenant
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.currentApiRequestsMonth = t.currentApiRequestsMonth + :increment
        WHERE t.id = :tenantId
    """)
    fun incrementApiUsage(
        @Param("tenantId") tenantId: UUID,
        @Param("increment") increment: Long
    )
    
    /**
     * Update storage usage for tenant
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.currentStorageBytes = GREATEST(0, t.currentStorageBytes + :increment)
        WHERE t.id = :tenantId
    """)
    fun updateStorageUsage(
        @Param("tenantId") tenantId: UUID,
        @Param("increment") increment: Long
    )
    
    /**
     * Reset monthly API usage for all tenants
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.currentApiRequestsMonth = 0
        WHERE t.deleted = false
    """)
    fun resetMonthlyApiUsageForAllTenants()
    
    /**
     * Update subscription tier
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.subscriptionTier = :newTier,
            t.maxStorageBytes = :maxStorage,
            t.maxApiRequestsPerMonth = :maxApiRequests,
            t.maxUsers = :maxUsers,
            t.subscriptionStartDate = :startDate,
            t.subscriptionEndDate = :endDate
        WHERE t.id = :tenantId
    """)
    fun updateSubscription(
        @Param("tenantId") tenantId: UUID,
        @Param("newTier") newTier: SubscriptionTier,
        @Param("maxStorage") maxStorage: Long,
        @Param("maxApiRequests") maxApiRequests: Long,
        @Param("maxUsers") maxUsers: Int,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    )
    
    /**
     * Update tenant status
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.status = :newStatus
        WHERE t.id = :tenantId
    """)
    fun updateTenantStatus(
        @Param("tenantId") tenantId: UUID,
        @Param("newStatus") newStatus: TenantStatus
    )
    
    /**
     * Mark onboarding as completed
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.onboardingCompleted = true,
            t.onboardingCompletedAt = :completedAt
        WHERE t.id = :tenantId
    """)
    fun completeOnboarding(
        @Param("tenantId") tenantId: UUID,
        @Param("completedAt") completedAt: LocalDateTime
    )
    
    /**
     * Soft delete tenants by IDs
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.deleted = true, 
            t.deletedAt = CURRENT_TIMESTAMP,
            t.status = 'ARCHIVED'
        WHERE t.id IN :ids
    """)
    fun softDeleteByIds(@Param("ids") ids: List<UUID>)
    
    /**
     * Restore soft deleted tenants
     */
    @Modifying
    @Query("""
        UPDATE Tenant t 
        SET t.deleted = false, 
            t.deletedAt = NULL,
            t.status = 'ACTIVE'
        WHERE t.id IN :ids
    """)
    fun restoreByIds(@Param("ids") ids: List<UUID>)
    
    // ==========================================================================
    // EXISTENCE CHECKS
    // ==========================================================================
    
    /**
     * Check if slug exists
     */
    fun existsBySlugAndDeletedFalse(slug: String): Boolean
    
    /**
     * Check if contact email exists
     */
    fun existsByContactEmailAndDeletedFalse(contactEmail: String): Boolean
    
    /**
     * Check if slug exists for different tenant
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM Tenant t 
        WHERE t.slug = :slug 
        AND t.id != :excludeId 
        AND t.deleted = false
    """)
    fun existsBySlugAndNotId(
        @Param("slug") slug: String,
        @Param("excludeId") excludeId: UUID
    ): Boolean
    
    /**
     * Check if contact email exists for different tenant
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM Tenant t 
        WHERE t.contactEmail = :email 
        AND t.id != :excludeId 
        AND t.deleted = false
    """)
    fun existsByContactEmailAndNotId(
        @Param("email") email: String,
        @Param("excludeId") excludeId: UUID
    ): Boolean
    
    // ==========================================================================
    // CLEANUP AND ARCHIVAL
    // ==========================================================================
    
    /**
     * Find tenants eligible for cleanup (cancelled and old)
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status IN ('CANCELLED', 'ARCHIVED')
        AND t.updatedAt < :cutoffDate
        ORDER BY t.updatedAt ASC
    """)
    fun findTenantsEligibleForCleanup(
        @Param("cutoffDate") cutoffDate: LocalDateTime
    ): List<Tenant>
    
    /**
     * Find tenants with no recent activity for archival
     */
    @Query("""
        SELECT t FROM Tenant t 
        WHERE t.deleted = false
        AND t.status NOT IN ('ACTIVE', 'TRIAL')
        AND (t.lastActivityAt IS NULL OR t.lastActivityAt < :inactivityThreshold)
        AND t.createdAt < :ageThreshold
        ORDER BY t.lastActivityAt ASC NULLS FIRST
    """)
    fun findTenantsForArchival(
        @Param("inactivityThreshold") inactivityThreshold: LocalDateTime,
        @Param("ageThreshold") ageThreshold: LocalDateTime
    ): List<Tenant>
}