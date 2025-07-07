// =============================================================================
// 🔥 HADES BACKEND - REPOSITORY LAYER
// =============================================================================
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// File: src/main/kotlin/com/kairos/hades/repository/Repositories.kt
// Purpose: Data access layer with multi-tenant support
// =============================================================================

package com.kairos.hades.repository

import com.kairos.hades.atoms.EligibilityAtom
import com.kairos.hades.atoms.AtomCategory
import com.kairos.hades.atoms.AtomStatus
import com.kairos.hades.entity.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

// =============================================================================
// TENANT REPOSITORY
// =============================================================================

@Repository
interface TenantRepository : JpaRepository<Tenant, UUID>, JpaSpecificationExecutor<Tenant> {
    
    fun findBySubdomain(subdomain: String): Tenant?
    
    fun findBySubdomainIgnoreCase(subdomain: String): Tenant?
    
    fun findByContactEmailIgnoreCase(email: String): Tenant?
    
    fun findByBillingEmailIgnoreCase(email: String): Tenant?
    
    @Query("SELECT t FROM Tenant t WHERE t.status = :status")
    fun findByStatus(@Param("status") status: TenantStatus): List<Tenant>
    
    @Query("SELECT t FROM Tenant t WHERE t.subscriptionTier = :tier")
    fun findBySubscriptionTier(@Param("tier") tier: SubscriptionTier): List<Tenant>
    
    @Query("SELECT t FROM Tenant t WHERE t.trialEndsAt < :date AND t.status = 'TRIAL'")
    fun findExpiredTrials(@Param("date") date: LocalDateTime): List<Tenant>
    
    @Query("SELECT t FROM Tenant t WHERE t.subscriptionEndsAt < :date AND t.status = 'ACTIVE'")
    fun findExpiredSubscriptions(@Param("date") date: LocalDateTime): List<Tenant>
    
    @Query("SELECT t FROM Tenant t WHERE t.isActive = true AND t.status IN ('ACTIVE', 'TRIAL')")
    fun findActiveTenants(): List<Tenant>
    
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.subscriptionTier = :tier")
    fun countBySubscriptionTier(@Param("tier") tier: SubscriptionTier): Long
    
    @Query("SELECT t.subscriptionTier, COUNT(t) FROM Tenant t GROUP BY t.subscriptionTier")
    fun getTenantCountsByTier(): List<Array<Any>>
    
    @Modifying
    @Query("UPDATE Tenant t SET t.lastLoginAt = :loginTime WHERE t.id = :tenantId")
    fun updateLastLoginTime(@Param("tenantId") tenantId: UUID, @Param("loginTime") loginTime: LocalDateTime)
    
    @Query("""
        SELECT t FROM Tenant t 
        WHERE (:searchTerm IS NULL OR 
               LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(t.subdomain) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(t.contactEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR t.status = :status)
        AND (:tier IS NULL OR t.subscriptionTier = :tier)
    """)
    fun findTenantsWithFilters(
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: TenantStatus?,
        @Param("tier") tier: SubscriptionTier?,
        pageable: Pageable
    ): Page<Tenant>
}

// =============================================================================
// APPLICATION REPOSITORY
// =============================================================================

@Repository
interface ApplicationRepository : JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {
    
    fun findBySubdomain(subdomain: String): Application?
    
    fun findByApiKey(apiKey: String): Application?
    
    fun findByTenantId(tenantId: UUID): List<Application>
    
    fun findByTenantIdAndStatus(tenantId: UUID, status: ApplicationStatus): List<Application>
    
    @Query("SELECT a FROM Application a WHERE a.tenant.id = :tenantId AND a.status = 'ACTIVE'")
    fun findActiveApplicationsByTenant(@Param("tenantId") tenantId: UUID): List<Application>
    
    @Query("SELECT COUNT(a) FROM Application a WHERE a.tenant.id = :tenantId")
    fun countByTenantId(@Param("tenantId") tenantId: UUID): Long
    
    @Query("SELECT COUNT(a) FROM Application a WHERE a.status = :status")
    fun countByStatus(@Param("status") status: ApplicationStatus): Long
    
    @Query("""
        SELECT a FROM Application a 
        WHERE a.tenant.id = :tenantId
        AND (:searchTerm IS NULL OR 
             LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
             LOWER(a.subdomain) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR a.status = :status)
    """)
    fun findApplicationsWithFilters(
        @Param("tenantId") tenantId: UUID,
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: ApplicationStatus?,
        pageable: Pageable
    ): Page<Application>
    
    @Modifying
    @Query("UPDATE Application a SET a.status = :status WHERE a.tenant.id = :tenantId")
    fun updateStatusByTenantId(@Param("tenantId") tenantId: UUID, @Param("status") status: ApplicationStatus)
}

// =============================================================================
// USER REPOSITORY
// =============================================================================

@Repository
interface UserRepository : JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    
    fun findByEmail(email: String): User?
    
    fun findByEmailIgnoreCase(email: String): User?
    
    fun findByEmailVerificationToken(token: String): User?
    
    fun findByPasswordResetToken(token: String): User?
    
    fun findByTenantId(tenantId: UUID): List<User>
    
    fun findByTenantIdAndRole(tenantId: UUID, role: UserRole): List<User>
    
    fun findByTenantIdAndStatus(tenantId: UUID, status: UserStatus): List<User>
    
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.status = 'ACTIVE'")
    fun findActiveUsersByTenant(@Param("tenantId") tenantId: UUID): List<User>
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId")
    fun countByTenantId(@Param("tenantId") tenantId: UUID): Long
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId AND u.role = :role")
    fun countByTenantIdAndRole(@Param("tenantId") tenantId: UUID, @Param("role") role: UserRole): Long
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    fun countByStatus(@Param("status") status: UserStatus): Long
    
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenant.id = :tenantId
        AND (:searchTerm IS NULL OR 
             LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
             LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
             LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:role IS NULL OR u.role = :role)
        AND (:status IS NULL OR u.status = :status)
    """)
    fun findUsersWithFilters(
        @Param("tenantId") tenantId: UUID,
        @Param("searchTerm") searchTerm: String?,
        @Param("role") role: UserRole?,
        @Param("status") status: UserStatus?,
        pageable: Pageable
    ): Page<User>
    
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.role IN ('OWNER', 'ADMIN')")
    fun findAdminUsersByTenant(@Param("tenantId") tenantId: UUID): List<User>
    
    @Query("""
        SELECT u FROM User u 
        WHERE u.passwordResetExpiresAt < :now 
        AND u.passwordResetToken IS NOT NULL
    """)
    fun findUsersWithExpiredPasswordResetTokens(@Param("now") now: LocalDateTime): List<User>
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    fun updateLastLoginTime(@Param("userId") userId: UUID, @Param("loginTime") loginTime: LocalDateTime)
    
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.passwordResetToken = null, u.passwordResetExpiresAt = null 
        WHERE u.passwordResetExpiresAt < :now
    """)
    fun clearExpiredPasswordResetTokens(@Param("now") now: LocalDateTime)
}

// =============================================================================
// ELIGIBILITY ATOM REPOSITORY
// =============================================================================

@Repository
interface EligibilityAtomRepository : JpaRepository<EligibilityAtom, UUID>, JpaSpecificationExecutor<EligibilityAtom> {
    
    fun findByTenantId(tenantId: UUID): List<EligibilityAtom>
    
    fun findByTenantIdAndStatus(tenantId: UUID, status: AtomStatus): List<EligibilityAtom>
    
    fun findByTenantIdAndCategory(tenantId: UUID, category: AtomCategory): List<EligibilityAtom>
    
    fun findByTenantIdAndCategoryAndStatus(
        tenantId: UUID, 
        category: AtomCategory, 
        status: AtomStatus
    ): List<EligibilityAtom>
    
    @Query("SELECT a FROM EligibilityAtom a WHERE a.tenant.id = :tenantId AND a.status = 'ACTIVE'")
    fun findActiveAtomsByTenant(@Param("tenantId") tenantId: UUID): List<EligibilityAtom>
    
    @Query("SELECT a FROM EligibilityAtom a WHERE a.name = :name AND a.tenant.id = :tenantId")
    fun findByNameAndTenantId(@Param("name") name: String, @Param("tenantId") tenantId: UUID): List<EligibilityAtom>
    
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.name = :name 
        AND a.tenant.id = :tenantId 
        ORDER BY a.version DESC
    """)
    fun findLatestVersionByNameAndTenant(
        @Param("name") name: String, 
        @Param("tenantId") tenantId: UUID
    ): EligibilityAtom?
    
    @Query("SELECT MAX(a.version) FROM EligibilityAtom a WHERE a.name = :name AND a.tenant.id = :tenantId")
    fun findMaxVersionByNameAndTenant(@Param("name") name: String, @Param("tenantId") tenantId: UUID): Int?
    
    @Query("SELECT COUNT(a) FROM EligibilityAtom a WHERE a.tenant.id = :tenantId")
    fun countByTenantId(@Param("tenantId") tenantId: UUID): Long
    
    @Query("SELECT COUNT(a) FROM EligibilityAtom a WHERE a.tenant.id = :tenantId AND a.status = :status")
    fun countByTenantIdAndStatus(@Param("tenantId") tenantId: UUID, @Param("status") status: AtomStatus): Long
    
    @Query("SELECT COUNT(a) FROM EligibilityAtom a WHERE a.tenant.id = :tenantId AND a.category = :category")
    fun countByTenantIdAndCategory(@Param("tenantId") tenantId: UUID, @Param("category") category: AtomCategory): Long
    
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenant.id = :tenantId
        AND (:searchTerm IS NULL OR 
             LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
             LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:category IS NULL OR a.category = :category)
        AND (:status IS NULL OR a.status = :status)
        AND (:tag IS NULL OR :tag MEMBER OF a.tags)
    """)
    fun findAtomsWithFilters(
        @Param("tenantId") tenantId: UUID,
        @Param("searchTerm") searchTerm: String?,
        @Param("category") category: AtomCategory?,
        @Param("status") status: AtomStatus?,
        @Param("tag") tag: String?,
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenant.id = :tenantId 
        AND a.status = 'ACTIVE'
        ORDER BY a.usageCount DESC
    """)
    fun findMostUsedAtomsByTenant(@Param("tenantId") tenantId: UUID, pageable: Pageable): Page<EligibilityAtom>
    
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenant.id = :tenantId 
        AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    fun findRecentAtomsByTenant(
        @Param("tenantId") tenantId: UUID, 
        @Param("since") since: LocalDateTime,
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    @Modifying
    @Query("UPDATE EligibilityAtom a SET a.usageCount = a.usageCount + 1 WHERE a.id = :atomId")
    fun incrementUsageCount(@Param("atomId") atomId: UUID)
    
    @Modifying
    @Query("""
        UPDATE EligibilityAtom a 
        SET a.averageExecutionTimeMs = :avgTime, a.successRate = :successRate 
        WHERE a.id = :atomId
    """)
    fun updatePerformanceMetrics(
        @Param("atomId") atomId: UUID,
        @Param("avgTime") avgTime: Long,
        @Param("successRate") successRate: Double
    )
    
    @Query("""
        SELECT a.category, COUNT(a) 
        FROM EligibilityAtom a 
        WHERE a.tenant.id = :tenantId 
        GROUP BY a.category
    """)
    fun getAtomCountsByCategory(@Param("tenantId") tenantId: UUID): List<Array<Any>>
    
    @Query("""
        SELECT a.status, COUNT(a) 
        FROM EligibilityAtom a 
        WHERE a.tenant.id = :tenantId 
        GROUP BY a.status
    """)
    fun getAtomCountsByStatus(@Param("tenantId") tenantId: UUID): List<Array<Any>>
    
    @Query("""
        SELECT tag, COUNT(a) 
        FROM EligibilityAtom a 
        JOIN a.tags tag 
        WHERE a.tenant.id = :tenantId 
        GROUP BY tag 
        ORDER BY COUNT(a) DESC
    """)
    fun getMostUsedTags(@Param("tenantId") tenantId: UUID, pageable: Pageable): List<Array<Any>>
}

// =============================================================================
// CUSTOM REPOSITORY IMPLEMENTATIONS
// =============================================================================

// Custom repository interface for complex queries
interface CustomTenantRepository {
    fun findTenantsWithUsageStats(): List<Map<String, Any>>
    fun getTenantHealthMetrics(tenantId: UUID): Map<String, Any>
    fun findTenantsNearingLimits(): List<Tenant>
}

// Custom repository interface for analytics
interface CustomEligibilityAtomRepository {
    fun getAtomPerformanceAnalytics(tenantId: UUID, days: Int): List<Map<String, Any>>
    fun getAtomUsageTrends(tenantId: UUID, days: Int): List<Map<String, Any>>
    fun findUnderutilizedAtoms(tenantId: UUID, threshold: Long): List<EligibilityAtom>
}

// =============================================================================
// REPOSITORY IMPLEMENTATIONS
// =============================================================================

@Repository
class CustomTenantRepositoryImpl(
    private val tenantRepository: TenantRepository
) : CustomTenantRepository {
    
    override fun findTenantsWithUsageStats(): List<Map<String, Any>> {
        // Implementation would use JPQL or native queries for complex analytics
        return emptyList() // Placeholder
    }
    
    override fun getTenantHealthMetrics(tenantId: UUID): Map<String, Any> {
        // Implementation would aggregate various metrics
        return emptyMap() // Placeholder
    }
    
    override fun findTenantsNearingLimits(): List<Tenant> {
        // Implementation would check usage against subscription limits
        return emptyList() // Placeholder
    }
}

@Repository
class CustomEligibilityAtomRepositoryImpl(
    private val atomRepository: EligibilityAtomRepository
) : CustomEligibilityAtomRepository {
    
    override fun getAtomPerformanceAnalytics(tenantId: UUID, days: Int): List<Map<String, Any>> {
        // Implementation would analyze performance trends
        return emptyList() // Placeholder
    }
    
    override fun getAtomUsageTrends(tenantId: UUID, days: Int): List<Map<String, Any>> {
        // Implementation would analyze usage patterns
        return emptyList() // Placeholder
    }
    
    override fun findUnderutilizedAtoms(tenantId: UUID, threshold: Long): List<EligibilityAtom> {
        // Implementation would find atoms with low usage
        return emptyList() // Placeholder
    }
}