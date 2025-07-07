// =============================================================================
// File: src/main/kotlin/com/kairos/hades/repository/EligibilityAtomRepository.kt
// 🔥 HADES EligibilityAtom Repository
// Author: Sankhadeep Banerjee
// Data access layer for EligibilityAtoms with tenant-aware queries
// =============================================================================

package com.kairos.hades.repository

import com.kairos.hades.atoms.EligibilityAtom
import com.kairos.hades.enums.AtomCategory
import com.kairos.hades.enums.AtomStatus
import com.kairos.hades.enums.AtomType
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
 * Repository interface for EligibilityAtom entities
 * Provides tenant-aware data access methods
 */
@Repository
interface EligibilityAtomRepository : JpaRepository<EligibilityAtom, UUID> {
    
    // ==========================================================================
    // BASIC FIND METHODS
    // ==========================================================================
    
    /**
     * Find atom by code within tenant
     */
    fun findByTenantIdAndCode(tenantId: String, code: String): EligibilityAtom?
    
    /**
     * Find atom by code and version within tenant
     */
    fun findByTenantIdAndCodeAndVersion(tenantId: String, code: String, version: Int): EligibilityAtom?
    
    /**
     * Find all atoms for a tenant
     */
    fun findByTenantIdAndDeletedFalse(tenantId: String, pageable: Pageable): Page<EligibilityAtom>
    
    /**
     * Find atoms by status
     */
    fun findByTenantIdAndStatusAndDeletedFalse(
        tenantId: String, 
        status: AtomStatus, 
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    /**
     * Find atoms by category
     */
    fun findByTenantIdAndCategoryAndDeletedFalse(
        tenantId: String, 
        category: AtomCategory, 
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    /**
     * Find atoms by type
     */
    fun findByTenantIdAndTypeAndDeletedFalse(
        tenantId: String, 
        type: AtomType, 
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    /**
     * Find atoms by multiple statuses
     */
    fun findByTenantIdAndStatusInAndDeletedFalse(
        tenantId: String, 
        statuses: List<AtomStatus>, 
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    // ==========================================================================
    // SEARCH AND FILTER METHODS
    // ==========================================================================
    
    /**
     * Search atoms by name or description
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
             OR LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY a.name ASC
    """)
    fun searchByNameOrDescription(
        @Param("tenantId") tenantId: String,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    /**
     * Find atoms with specific tags
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        JOIN a.tags t 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND LOWER(t) IN :tags
    """)
    fun findByTenantIdAndTagsIn(
        @Param("tenantId") tenantId: String,
        @Param("tags") tags: List<String>,
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    /**
     * Find atoms that depend on a specific atom
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        JOIN a.dependencies d 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND d = :dependencyCode
    """)
    fun findByDependency(
        @Param("tenantId") tenantId: String,
        @Param("dependencyCode") dependencyCode: String
    ): List<EligibilityAtom>
    
    /**
     * Find child atoms of a parent
     */
    fun findByTenantIdAndParentAtomCodeAndDeletedFalse(
        tenantId: String, 
        parentAtomCode: String
    ): List<EligibilityAtom>
    
    /**
     * Advanced search with multiple filters
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND (:category IS NULL OR a.category = :category)
        AND (:type IS NULL OR a.type = :type)
        AND (:status IS NULL OR a.status = :status)
        AND (:searchTerm IS NULL OR 
             LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR 
             LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY a.priority DESC, a.name ASC
    """)
    fun findWithFilters(
        @Param("tenantId") tenantId: String,
        @Param("category") category: AtomCategory?,
        @Param("type") type: AtomType?,
        @Param("status") status: AtomStatus?,
        @Param("searchTerm") searchTerm: String?,
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    // ==========================================================================
    // STATISTICS AND ANALYTICS
    // ==========================================================================
    
    /**
     * Count atoms by status
     */
    fun countByTenantIdAndStatusAndDeletedFalse(tenantId: String, status: AtomStatus): Long
    
    /**
     * Count atoms by category
     */
    fun countByTenantIdAndCategoryAndDeletedFalse(tenantId: String, category: AtomCategory): Long
    
    /**
     * Count total active atoms for tenant
     */
    fun countByTenantIdAndDeletedFalse(tenantId: String): Long
    
    /**
     * Get atoms with highest execution count
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND a.status = 'ACTIVE'
        ORDER BY a.executionCount DESC
    """)
    fun findMostExecutedAtoms(
        @Param("tenantId") tenantId: String,
        pageable: Pageable
    ): List<EligibilityAtom>
    
    /**
     * Get atoms with best performance
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND a.status = 'ACTIVE'
        AND a.executionCount > 10
        ORDER BY a.successRate DESC, a.avgExecutionTimeMs ASC
    """)
    fun findBestPerformingAtoms(
        @Param("tenantId") tenantId: String,
        pageable: Pageable
    ): List<EligibilityAtom>
    
    /**
     * Get atoms that need attention (high error rate)
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND a.status = 'ACTIVE'
        AND a.errorRate > :errorThreshold
        ORDER BY a.errorRate DESC
    """)
    fun findAtomsNeedingAttention(
        @Param("tenantId") tenantId: String,
        @Param("errorThreshold") errorThreshold: Double,
        pageable: Pageable
    ): List<EligibilityAtom>
    
    /**
     * Get execution statistics summary
     */
    @Query("""
        SELECT 
            COUNT(a) as totalAtoms,
            SUM(a.executionCount) as totalExecutions,
            AVG(a.avgExecutionTimeMs) as avgExecutionTime,
            AVG(a.successRate) as avgSuccessRate
        FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND a.status = 'ACTIVE'
    """)
    fun getExecutionStatistics(@Param("tenantId") tenantId: String): Map<String, Any>
    
    // ==========================================================================
    // MAINTENANCE AND CLEANUP
    // ==========================================================================
    
    /**
     * Find atoms not executed recently
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND (a.lastExecutedAt IS NULL OR a.lastExecutedAt < :cutoffDate)
        ORDER BY a.lastExecutedAt ASC NULLS FIRST
    """)
    fun findUnusedAtoms(
        @Param("tenantId") tenantId: String,
        @Param("cutoffDate") cutoffDate: LocalDateTime,
        pageable: Pageable
    ): Page<EligibilityAtom>
    
    /**
     * Find atoms with old versions
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND EXISTS (
            SELECT 1 FROM EligibilityAtom a2 
            WHERE a2.tenantId = a.tenantId 
            AND a2.code = a.code 
            AND a2.version > a.version 
            AND a2.deleted = false
        )
        ORDER BY a.code, a.version
    """)
    fun findOutdatedVersions(@Param("tenantId") tenantId: String): List<EligibilityAtom>
    
    /**
     * Update execution statistics
     */
    @Modifying
    @Query("""
        UPDATE EligibilityAtom a 
        SET a.avgExecutionTimeMs = :avgExecutionTime,
            a.executionCount = :executionCount,
            a.successRate = :successRate,
            a.errorRate = :errorRate,
            a.lastExecutedAt = :lastExecutedAt
        WHERE a.id = :atomId
    """)
    fun updateExecutionStatistics(
        @Param("atomId") atomId: UUID,
        @Param("avgExecutionTime") avgExecutionTime: Long,
        @Param("executionCount") executionCount: Long,
        @Param("successRate") successRate: Double,
        @Param("errorRate") errorRate: Double,
        @Param("lastExecutedAt") lastExecutedAt: LocalDateTime
    )
    
    /**
     * Soft delete atoms by IDs
     */
    @Modifying
    @Query("""
        UPDATE EligibilityAtom a 
        SET a.deleted = true, 
            a.deletedAt = CURRENT_TIMESTAMP,
            a.lastModifiedBy = :deletedBy
        WHERE a.id IN :ids 
        AND a.tenantId = :tenantId
    """)
    fun softDeleteByIds(
        @Param("ids") ids: List<UUID>,
        @Param("tenantId") tenantId: String,
        @Param("deletedBy") deletedBy: String
    )
    
    /**
     * Restore soft deleted atoms
     */
    @Modifying
    @Query("""
        UPDATE EligibilityAtom a 
        SET a.deleted = false, 
            a.deletedAt = NULL,
            a.lastModifiedBy = :restoredBy
        WHERE a.id IN :ids 
        AND a.tenantId = :tenantId
    """)
    fun restoreByIds(
        @Param("ids") ids: List<UUID>,
        @Param("tenantId") tenantId: String,
        @Param("restoredBy") restoredBy: String
    )
    
    // ==========================================================================
    // VERSION MANAGEMENT
    // ==========================================================================
    
    /**
     * Find latest version of atom by code
     */
    @Query("""
        SELECT a FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.code = :code 
        AND a.deleted = false
        AND a.version = (
            SELECT MAX(a2.version) FROM EligibilityAtom a2 
            WHERE a2.tenantId = :tenantId 
            AND a2.code = :code 
            AND a2.deleted = false
        )
    """)
    fun findLatestVersion(
        @Param("tenantId") tenantId: String,
        @Param("code") code: String
    ): EligibilityAtom?
    
    /**
     * Find all versions of an atom
     */
    fun findByTenantIdAndCodeAndDeletedFalseOrderByVersionDesc(
        tenantId: String, 
        code: String
    ): List<EligibilityAtom>
    
    /**
     * Get next version number for atom code
     */
    @Query("""
        SELECT COALESCE(MAX(a.version), 0) + 1 
        FROM EligibilityAtom a 
        WHERE a.tenantId = :tenantId 
        AND a.code = :code 
        AND a.deleted = false
    """)
    fun getNextVersionNumber(
        @Param("tenantId") tenantId: String,
        @Param("code") code: String
    ): Int
    
    // ==========================================================================
    // EXISTENCE CHECKS
    // ==========================================================================
    
    /**
     * Check if atom code exists within tenant
     */
    fun existsByTenantIdAndCodeAndDeletedFalse(tenantId: String, code: String): Boolean
    
    /**
     * Check if atom code and version exists within tenant
     */
    fun existsByTenantIdAndCodeAndVersionAndDeletedFalse(
        tenantId: String, 
        code: String, 
        version: Int
    ): Boolean
    
    /**
     * Check if atom has dependencies
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM EligibilityAtom a 
        JOIN a.dependencies d 
        WHERE a.tenantId = :tenantId 
        AND a.deleted = false
        AND d = :atomCode
    """)
    fun hasActiveDependents(
        @Param("tenantId") tenantId: String,
        @Param("atomCode") atomCode: String
    ): Boolean
}