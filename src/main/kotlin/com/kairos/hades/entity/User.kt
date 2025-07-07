// =============================================================================
// File: src/main/kotlin/com/kairos/hades/entity/User.kt
// 🔥 HADES BACKEND - USER ENTITY
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// Purpose: Core User entity with multi-tenant support and authentication
// =============================================================================

package com.kairos.hades.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.ParamDef
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.util.*

/**
 * User entity representing system users with multi-tenant support
 * Implements UserDetails for Spring Security integration
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_tenant_email", columnList = "tenant_id, email", unique = true),
        Index(name = "idx_users_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_external_id", columnList = "external_id"),
        Index(name = "idx_users_status", columnList = "status"),
        Index(name = "idx_users_role", columnList = "role"),
        Index(name = "idx_users_last_login", columnList = "last_login_at")
    ]
)
@FilterDef(
    name = "tenantFilter",
    parameters = [ParamDef(name = "tenantId", type = String::class)]
)
@Filter(
    name = "tenantFilter",
    condition = "tenant_id = :tenantId"
)
data class User(
    
    // ==========================================================================
    // PRIMARY IDENTIFICATION
    // ==========================================================================
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    val tenantId: String,
    
    @Column(name = "external_id", length = 255)
    val externalId: String? = null,
    
    // ==========================================================================
    // AUTHENTICATION FIELDS
    // ==========================================================================
    
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    @Column(name = "email", nullable = false, length = 255)
    val email: String,
    
    @field:JsonIgnore
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 60, max = 60, message = "Password hash must be 60 characters")
    @Column(name = "password_hash", nullable = false, length = 60)
    val passwordHash: String,
    
    @Column(name = "password_reset_token", length = 255)
    val passwordResetToken: String? = null,
    
    @Column(name = "password_reset_expires_at")
    val passwordResetExpiresAt: LocalDateTime? = null,
    
    @Column(name = "email_verification_token", length = 255)
    val emailVerificationToken: String? = null,
    
    @Column(name = "email_verified_at")
    val emailVerifiedAt: LocalDateTime? = null,
    
    // ==========================================================================
    // USER PROFILE
    // ==========================================================================
    
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name must not exceed 100 characters")
    @Column(name = "first_name", nullable = false, length = 100)
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name must not exceed 100 characters")
    @Column(name = "last_name", nullable = false, length = 100)
    val lastName: String,
    
    @Column(name = "display_name", length = 255)
    val displayName: String? = null,
    
    @Column(name = "avatar_url", length = 500)
    val avatarUrl: String? = null,
    
    @Column(name = "phone", length = 20)
    val phone: String? = null,
    
    @Column(name = "timezone", length = 50)
    val timezone: String? = "UTC",
    
    @Column(name = "locale", length = 10)
    val locale: String? = "en",
    
    // ==========================================================================
    // AUTHORIZATION & STATUS
    // ==========================================================================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: UserRole = UserRole.USER,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: UserStatus = UserStatus.ACTIVE,
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "user_permissions",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "permission")
    val permissions: Set<UserPermission> = setOf(),
    
    // ==========================================================================
    // ACTIVITY TRACKING
    // ==========================================================================
    
    @Column(name = "last_login_at")
    val lastLoginAt: LocalDateTime? = null,
    
    @Column(name = "last_login_ip", length = 45)
    val lastLoginIp: String? = null,
    
    @Column(name = "login_count", nullable = false)
    val loginCount: Long = 0,
    
    @Column(name = "failed_login_attempts", nullable = false)
    val failedLoginAttempts: Int = 0,
    
    @Column(name = "account_locked_until")
    val accountLockedUntil: LocalDateTime? = null,
    
    // ==========================================================================
    // SYSTEM FIELDS
    // ==========================================================================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "created_by", length = 100)
    val createdBy: String? = null,
    
    @Column(name = "updated_by", length = 100)
    val updatedBy: String? = null,
    
    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,
    
    @Column(name = "deleted_by", length = 100)
    val deletedBy: String? = null,
    
    // ==========================================================================
    // METADATA
    // ==========================================================================
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    val metadata: String? = null,
    
    @Column(name = "preferences", columnDefinition = "jsonb")
    val preferences: String? = null,
    
    @Column(name = "notes", columnDefinition = "text")
    val notes: String? = null

) : UserDetails {
    
    // ==========================================================================
    // USERDETAILS IMPLEMENTATION (Spring Security)
    // ==========================================================================
    
    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()
        
        // Add role-based authority
        authorities.add(SimpleGrantedAuthority("ROLE_${role.name}"))
        
        // Add permission-based authorities
        permissions.forEach { permission ->
            authorities.add(SimpleGrantedAuthority(permission.name))
        }
        
        return authorities
    }
    
    override fun getPassword(): String = passwordHash
    
    override fun getUsername(): String = email
    
    override fun isAccountNonExpired(): Boolean = deletedAt == null
    
    override fun isAccountNonLocked(): Boolean {
        return accountLockedUntil == null || accountLockedUntil!!.isBefore(LocalDateTime.now())
    }
    
    override fun isCredentialsNonExpired(): Boolean = true
    
    override fun isEnabled(): Boolean = status == UserStatus.ACTIVE && deletedAt == null
    
    // ==========================================================================
    // COMPUTED PROPERTIES
    // ==========================================================================
    
    val fullName: String
        get() = "$firstName $lastName"
    
    val isEmailVerified: Boolean
        get() = emailVerifiedAt != null
    
    val isPasswordResetValid: Boolean
        get() = passwordResetToken != null && 
                passwordResetExpiresAt != null && 
                passwordResetExpiresAt!!.isAfter(LocalDateTime.now())
    
    val isAdmin: Boolean
        get() = role in setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)
    
    val isLocked: Boolean
        get() = accountLockedUntil != null && accountLockedUntil!!.isAfter(LocalDateTime.now())
    
    val isDeletable: Boolean
        get() = role != UserRole.SUPER_ADMIN && status != UserStatus.SYSTEM
    
    // ==========================================================================
    // HELPER METHODS
    // ==========================================================================
    
    fun hasPermission(permission: UserPermission): Boolean {
        return permissions.contains(permission) || isAdmin
    }
    
    fun hasAnyPermission(vararg permissions: UserPermission): Boolean {
        return permissions.any { hasPermission(it) }
    }
    
    fun hasAllPermissions(vararg permissions: UserPermission): Boolean {
        return permissions.all { hasPermission(it) }
    }
    
    fun canAccessTenant(tenantId: String): Boolean {
        return this.tenantId == tenantId || role == UserRole.SUPER_ADMIN
    }
    
    fun shouldUpdatePassword(): Boolean {
        // Recommend password update after 90 days
        return updatedAt.isBefore(LocalDateTime.now().minusDays(90))
    }
    
    fun getDisplayNameOrFullName(): String {
        return displayName?.takeIf { it.isNotBlank() } ?: fullName
    }
    
    // ==========================================================================
    // COPY METHODS FOR IMMUTABILITY
    // ==========================================================================
    
    fun withPassword(newPasswordHash: String): User = 
        copy(passwordHash = newPasswordHash, updatedAt = LocalDateTime.now())
    
    fun withLastLogin(ip: String): User = copy(
        lastLoginAt = LocalDateTime.now(),
        lastLoginIp = ip,
        loginCount = loginCount + 1,
        failedLoginAttempts = 0,
        updatedAt = LocalDateTime.now()
    )
    
    fun withFailedLogin(): User = copy(
        failedLoginAttempts = failedLoginAttempts + 1,
        updatedAt = LocalDateTime.now()
    )
    
    fun withEmailVerified(): User = copy(
        emailVerifiedAt = LocalDateTime.now(),
        emailVerificationToken = null,
        updatedAt = LocalDateTime.now()
    )
    
    fun withPasswordReset(token: String, expiresAt: LocalDateTime): User = copy(
        passwordResetToken = token,
        passwordResetExpiresAt = expiresAt,
        updatedAt = LocalDateTime.now()
    )
    
    fun withStatus(newStatus: UserStatus): User = copy(
        status = newStatus,
        updatedAt = LocalDateTime.now()
    )
    
    fun withRole(newRole: UserRole): User = copy(
        role = newRole,
        updatedAt = LocalDateTime.now()
    )
    
    fun withLockUntil(lockUntil: LocalDateTime): User = copy(
        accountLockedUntil = lockUntil,
        updatedAt = LocalDateTime.now()
    )
    
    fun withMetadata(newMetadata: String): User = copy(
        metadata = newMetadata,
        updatedAt = LocalDateTime.now()
    )
    
    fun withPreferences(newPreferences: String): User = copy(
        preferences = newPreferences,
        updatedAt = LocalDateTime.now()
    )
    
    fun softDelete(deletedBy: String): User = copy(
        deletedAt = LocalDateTime.now(),
        deletedBy = deletedBy,
        status = UserStatus.DELETED,
        updatedAt = LocalDateTime.now()
    )
    
    // ==========================================================================
    // JSON SERIALIZATION HELPERS
    // ==========================================================================
    
    @get:JsonProperty("full_name")
    val jsonFullName: String get() = fullName
    
    @get:JsonProperty("display_name_or_full")
    val jsonDisplayNameOrFull: String get() = getDisplayNameOrFullName()
    
    @get:JsonProperty("is_email_verified")
    val jsonIsEmailVerified: Boolean get() = isEmailVerified
    
    @get:JsonProperty("is_admin")
    val jsonIsAdmin: Boolean get() = isAdmin
    
    @get:JsonProperty("is_locked")
    val jsonIsLocked: Boolean get() = isLocked
    
    @get:JsonProperty("can_be_deleted")
    val jsonCanBeDeleted: Boolean get() = isDeletable
}

// =============================================================================
// ENUMS
// =============================================================================

enum class UserRole {
    USER,           // Regular user with basic permissions
    ADMIN,          // Tenant administrator with management permissions
    SUPER_ADMIN,    // System administrator with cross-tenant access
    MANAGER,        // Campaign manager with extended permissions
    ANALYST,        // Data analyst with read-only analytics access
    DEVELOPER,      // Developer with API access and testing permissions
    VIEWER          // Read-only access to specific resources
}

enum class UserStatus {
    ACTIVE,         // User is active and can log in
    INACTIVE,       // User is temporarily disabled
    PENDING,        // User registered but not yet activated
    SUSPENDED,      // User is suspended due to policy violation
    DELETED,        // User is soft-deleted
    SYSTEM          // System user (cannot be deleted/modified)
}

enum class UserPermission {
    // User Management
    USER_READ,
    USER_WRITE,
    USER_DELETE,
    USER_INVITE,
    
    // Eligibility Atoms
    ATOM_READ,
    ATOM_WRITE,
    ATOM_DELETE,
    ATOM_EXECUTE,
    ATOM_TEST,
    
    // Moments and Campaigns
    MOMENT_READ,
    MOMENT_WRITE,
    MOMENT_DELETE,
    MOMENT_EXECUTE,
    
    // Analytics and Reporting
    ANALYTICS_READ,
    ANALYTICS_EXPORT,
    ANALYTICS_ADMIN,
    
    // System Administration
    TENANT_READ,
    TENANT_WRITE,
    TENANT_DELETE,
    SYSTEM_CONFIG,
    
    // API Access
    API_READ,
    API_WRITE,
    API_ADMIN,
    
    // Billing and Subscriptions
    BILLING_READ,
    BILLING_WRITE,
    
    // Advanced Features
    EXPERIMENT_READ,
    EXPERIMENT_WRITE,
    ML_MODEL_READ,
    ML_MODEL_WRITE
}