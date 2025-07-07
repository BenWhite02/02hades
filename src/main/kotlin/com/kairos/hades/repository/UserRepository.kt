// =============================================================================
// File: src/main/kotlin/com/kairos/hades/repository/UserRepository.kt
// 🔥 HADES BACKEND - USER REPOSITORY
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// Purpose: Data access layer for User entities with tenant-aware queries
// =============================================================================

package com.kairos.hades.repository

import com.kairos.hades.entity.User
import com.kairos.hades.entity.UserRole
import com.kairos.hades.entity.UserStatus
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

/**
 * Repository interface for User entities with multi-tenant support
 * Provides tenant-aware data access methods and advanced querying capabilities
 */
@Repository
interface UserRepository : JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    
    // ==========================================================================
    // AUTHENTICATION QUERIES
    // ==========================================================================
    
    /**
     * Find user by email within a specific tenant (for authentication)
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.email = :email AND u.deletedAt IS NULL")
    fun findByTenantIdAndEmail(
        @Param("tenantId") tenantId: String,
        @Param("email") email: String
    ): User?
    
    /**
     * Find user by email (case insensitive) within tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    fun findByTenantIdAndEmailIgnoreCase(
        @Param("tenantId") tenantId: String,
        @Param("email") email: String
    ): User?
    
    /**
     * Check if email exists within tenant (for registration validation)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.tenantId = :tenantId AND LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    fun existsByTenantIdAndEmailIgnoreCase(
        @Param("tenantId") tenantId: String,
        @Param("email") email: String
    ): Boolean
    
    /**
     * Find user by external ID within tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.externalId = :externalId AND u.deletedAt IS NULL")
    fun findByTenantIdAndExternalId(
        @Param("tenantId") tenantId: String,
        @Param("externalId") externalId: String
    ): User?
    
    // ==========================================================================
    // PASSWORD RESET QUERIES
    // ==========================================================================
    
    /**
     * Find user by password reset token
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.passwordResetToken = :token 
        AND u.passwordResetExpiresAt > :now 
        AND u.deletedAt IS NULL
    """)
    fun findByValidPasswordResetToken(
        @Param("token") token: String,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): User?
    
    /**
     * Find user by email verification token
     */
    @Query("SELECT u FROM User u WHERE u.emailVerificationToken = :token AND u.deletedAt IS NULL")
    fun findByEmailVerificationToken(@Param("token") token: String): User?
    
    /**
     * Clear expired password reset tokens
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.passwordResetToken = NULL, 
            u.passwordResetExpiresAt = NULL, 
            u.updatedAt = :now
        WHERE u.passwordResetExpiresAt < :now
    """)
    fun clearExpiredPasswordResetTokens(@Param("now") now: LocalDateTime = LocalDateTime.now()): Int
    
    // ==========================================================================
    // TENANT-AWARE QUERIES
    // ==========================================================================
    
    /**
     * Find all active users within a tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.status = :status AND u.deletedAt IS NULL")
    fun findByTenantIdAndStatus(
        @Param("tenantId") tenantId: String,
        @Param("status") status: UserStatus,
        pageable: Pageable
    ): Page<User>
    
    /**
     * Find users by role within tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.role = :role AND u.deletedAt IS NULL")
    fun findByTenantIdAndRole(
        @Param("tenantId") tenantId: String,
        @Param("role") role: UserRole,
        pageable: Pageable
    ): Page<User>
    
    /**
     * Find users by status within tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.status IN :statuses AND u.deletedAt IS NULL")
    fun findByTenantIdAndStatusIn(
        @Param("tenantId") tenantId: String,
        @Param("statuses") statuses: List<UserStatus>,
        pageable: Pageable
    ): Page<User>
    
    /**
     * Search users by name or email within tenant
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.deletedAt IS NULL
        AND (
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(u.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
    """)
    fun searchByTenantIdAndNameOrEmail(
        @Param("tenantId") tenantId: String,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<User>
    
    // ==========================================================================
    // STATISTICS QUERIES
    // ==========================================================================
    
    /**
     * Count users by status within tenant
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = :status AND u.deletedAt IS NULL")
    fun countByTenantIdAndStatus(
        @Param("tenantId") tenantId: String,
        @Param("status") status: UserStatus
    ): Long
    
    /**
     * Count users by role within tenant
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.role = :role AND u.deletedAt IS NULL")
    fun countByTenantIdAndRole(
        @Param("tenantId") tenantId: String,
        @Param("role") role: UserRole
    ): Long
    
    /**
     * Count active users within tenant
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    fun countActiveUsersByTenantId(@Param("tenantId") tenantId: String): Long
    
    /**
     * Count new users registered in the last N days within tenant
     */
    @Query("""
        SELECT COUNT(u) FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.createdAt >= :since 
        AND u.deletedAt IS NULL
    """)
    fun countNewUsersSince(
        @Param("tenantId") tenantId: String,
        @Param("since") since: LocalDateTime
    ): Long
    
    /**
     * Count users who logged in within the last N days
     */
    @Query("""
        SELECT COUNT(u) FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.lastLoginAt >= :since 
        AND u.deletedAt IS NULL
    """)
    fun countActiveUsersSince(
        @Param("tenantId") tenantId: String,
        @Param("since") since: LocalDateTime
    ): Long
    
    // ==========================================================================
    // ADMIN QUERIES (Cross-tenant)
    // ==========================================================================
    
    /**
     * Find super admins (cross-tenant access)
     */
    @Query("SELECT u FROM User u WHERE u.role = 'SUPER_ADMIN' AND u.deletedAt IS NULL")
    fun findSuperAdmins(pageable: Pageable): Page<User>
    
    /**
     * Find users across all tenants by email (for super admin use)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    fun findByEmailAcrossAllTenants(@Param("email") email: String): List<User>
    
    /**
     * Count total active users across all tenants
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    fun countTotalActiveUsers(): Long
    
    /**
     * Find users with failed login attempts exceeding threshold
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.failedLoginAttempts >= :threshold 
        AND u.accountLockedUntil IS NULL
        AND u.deletedAt IS NULL
    """)
    fun findUsersWithExcessiveFailedLogins(
        @Param("tenantId") tenantId: String,
        @Param("threshold") threshold: Int
    ): List<User>
    
    /**
     * Find locked users within tenant
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.accountLockedUntil > :now 
        AND u.deletedAt IS NULL
    """)
    fun findLockedUsers(
        @Param("tenantId") tenantId: String,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): List<User>
    
    // ==========================================================================
    // BULK OPERATIONS
    // ==========================================================================
    
    /**
     * Update last login for user
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.lastLoginAt = :loginTime,
            u.lastLoginIp = :ip,
            u.loginCount = u.loginCount + 1,
            u.failedLoginAttempts = 0,
            u.updatedAt = :now
        WHERE u.id = :userId
    """)
    fun updateLastLogin(
        @Param("userId") userId: UUID,
        @Param("loginTime") loginTime: LocalDateTime,
        @Param("ip") ip: String,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): Int
    
    /**
     * Increment failed login attempts
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.failedLoginAttempts = u.failedLoginAttempts + 1,
            u.updatedAt = :now
        WHERE u.id = :userId
    """)
    fun incrementFailedLoginAttempts(
        @Param("userId") userId: UUID,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): Int
    
    /**
     * Lock user account
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.accountLockedUntil = :lockUntil,
            u.updatedAt = :now
        WHERE u.id = :userId
    """)
    fun lockUserAccount(
        @Param("userId") userId: UUID,
        @Param("lockUntil") lockUntil: LocalDateTime,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): Int
    
    /**
     * Unlock user accounts that have passed their lock time
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.accountLockedUntil = NULL,
            u.updatedAt = :now
        WHERE u.accountLockedUntil < :now
    """)
    fun unlockExpiredAccounts(@Param("now") now: LocalDateTime = LocalDateTime.now()): Int
    
    /**
     * Soft delete users by IDs within tenant
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.deletedAt = :now,
            u.deletedBy = :deletedBy,
            u.status = 'DELETED',
            u.updatedAt = :now
        WHERE u.id IN :userIds 
        AND u.tenantId = :tenantId
        AND u.role != 'SUPER_ADMIN'
    """)
    fun softDeleteUsers(
        @Param("userIds") userIds: List<UUID>,
        @Param("tenantId") tenantId: String,
        @Param("deletedBy") deletedBy: String,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): Int
    
    /**
     * Update user status in bulk
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.status = :status,
            u.updatedAt = :now
        WHERE u.id IN :userIds 
        AND u.tenantId = :tenantId
    """)
    fun updateUsersStatus(
        @Param("userIds") userIds: List<UUID>,
        @Param("tenantId") tenantId: String,
        @Param("status") status: UserStatus,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): Int
    
    // ==========================================================================
    // CUSTOM ANALYTICS QUERIES
    // ==========================================================================
    
    /**
     * Get user registration stats by day for the last N days
     */
    @Query("""
        SELECT DATE(u.createdAt) as date, COUNT(u) as count
        FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.createdAt >= :since
        AND u.deletedAt IS NULL
        GROUP BY DATE(u.createdAt)
        ORDER BY DATE(u.createdAt)
    """)
    fun getRegistrationStatsByDay(
        @Param("tenantId") tenantId: String,
        @Param("since") since: LocalDateTime
    ): List<Any>
    
    /**
     * Get user login activity stats
     */
    @Query("""
        SELECT DATE(u.lastLoginAt) as date, COUNT(u) as count
        FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.lastLoginAt >= :since
        AND u.deletedAt IS NULL
        GROUP BY DATE(u.lastLoginAt)
        ORDER BY DATE(u.lastLoginAt)
    """)
    fun getLoginActivityStatsByDay(
        @Param("tenantId") tenantId: String,
        @Param("since") since: LocalDateTime
    ): List<Any>
    
    /**
     * Get users who haven't logged in for N days (inactive users)
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.status = 'ACTIVE'
        AND (u.lastLoginAt IS NULL OR u.lastLoginAt < :since)
        AND u.deletedAt IS NULL
        ORDER BY u.lastLoginAt ASC NULLS FIRST
    """)
    fun findInactiveUsers(
        @Param("tenantId") tenantId: String,
        @Param("since") since: LocalDateTime,
        pageable: Pageable
    ): Page<User>
    
    /**
     * Find most active users by login count
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.status = 'ACTIVE'
        AND u.deletedAt IS NULL
        ORDER BY u.loginCount DESC, u.lastLoginAt DESC
    """)
    fun findMostActiveUsers(
        @Param("tenantId") tenantId: String,
        pageable: Pageable
    ): Page<User>
    
    /**
     * Find recently created users
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenantId = :tenantId 
        AND u.createdAt >= :since
        AND u.deletedAt IS NULL
        ORDER BY u.createdAt DESC
    """)
    fun findRecentlyCreatedUsers(
        @Param("tenantId") tenantId: String,
        @Param("since") since: LocalDateTime,
        pageable: Pageable
    ): Page<User>
}