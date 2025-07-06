// File: src/main/kotlin/com/kairos/hades/entity/BaseEntity.kt
// 🔥 HADES Core Entity Base Class
// Base entity with tenant awareness, auditing, and common fields
// All entities extend this for consistency and multi-tenancy support

package com.kairos.hades.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.ParamDef
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

/**
 * Base entity class for all Hades entities
 * Provides common fields, tenant isolation, and auditing capabilities
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@FilterDef(
    name = "tenantFilter",
    parameters = [ParamDef(name = "tenantId", type = String::class)]
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    open var id: UUID? = null
    
    /**
     * Tenant ID for multi-tenancy support
     * Every entity belongs to a tenant for data isolation
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    @JsonIgnore // Don't expose in API responses
    open var tenantId: String = ""
    
    /**
     * Soft delete flag
     * Allows logical deletion without removing data
     */
    @Column(name = "deleted", nullable = false)
    open var deleted: Boolean = false
    
    /**
     * Creation timestamp - automatically set on entity creation
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now()
    
    /**
     * Last update timestamp - automatically updated on entity modification
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now()
    
    /**
     * Soft delete timestamp - set when entity is soft deleted
     */
    @Column(name = "deleted_at")
    open var deletedAt: LocalDateTime? = null
    
    /**
     * User who created this entity
     */
    @CreatedBy
    @Column(name = "created_by", length = 100)
    open var createdBy: String? = null
    
    /**
     * User who last modified this entity
     */
    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    open var lastModifiedBy: String? = null
    
    /**
     * Version for optimistic locking
     */
    @Version
    @Column(name = "version", nullable = false)
    open var version: Long = 0L
    
    /**
     * Additional metadata as JSON
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    open var metadata: String? = null
    
    /**
     * Mark entity as deleted (soft delete)
     */
    open fun markAsDeleted(deletedBy: String? = null) {
        this.deleted = true
        this.deletedAt = LocalDateTime.now()
        this.lastModifiedBy = deletedBy
    }
    
    /**
     * Restore soft deleted entity
     */
    open fun restore(restoredBy: String? = null) {
        this.deleted = false
        this.deletedAt = null
        this.lastModifiedBy = restoredBy
    }
    
    /**
     * Check if entity is new (not persisted yet)
     */
    open fun isNew(): Boolean = id == null
    
    /**
     * Pre-persist callback to ensure tenant ID is set
     */
    @PrePersist
    protected open fun prePersist() {
        if (tenantId.isBlank()) {
            // This should be set by TenantContext, but fail-safe
            throw IllegalStateException("Tenant ID must be set before persisting entity")
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        
        other as BaseEntity
        
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
    
    override fun toString(): String {
        return "${this::class.simpleName}(id=$id, tenantId='$tenantId', deleted=$deleted)"
    }
}