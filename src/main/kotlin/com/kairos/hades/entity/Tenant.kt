// =============================================================================
// 🔥 HADES BACKEND - MULTI-TENANT FOUNDATION
// =============================================================================
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// File: src/main/kotlin/com/kairos/hades/entity/Tenant.kt
// Purpose: Core multi-tenant entities and infrastructure
// =============================================================================

package com.kairos.hades.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

// =============================================================================
// SUBSCRIPTION TIER ENUM
// =============================================================================

@JvmInline
value class TenantId(val value: UUID = UUID.randomUUID())

@JvmInline
value class ApplicationId(val value: UUID = UUID.randomUUID())

enum class SubscriptionTier(
    val displayName: String,
    val maxApplications: Int,
    val maxUsers: Int,
    val maxMomentsPerMonth: Long,
    val maxAtomsPerApplication: Int,
    val hasAdvancedAnalytics: Boolean,
    val hasAiOptimization: Boolean,
    val hasApiAccess: Boolean,
    val maxApiCallsPerDay: Long
) {
    STARTER(
        displayName = "Starter",
        maxApplications = 1,
        maxUsers = 3,
        maxMomentsPerMonth = 10_000,
        maxAtomsPerApplication = 50,
        hasAdvancedAnalytics = false,
        hasAiOptimization = false,
        hasApiAccess = false,
        maxApiCallsPerDay = 0
    ),
    
    PROFESSIONAL(
        displayName = "Professional",
        maxApplications = 5,
        maxUsers = 15,
        maxMomentsPerMonth = 100_000,
        maxAtomsPerApplication = 200,
        hasAdvancedAnalytics = true,
        hasAiOptimization = false,
        hasApiAccess = true,
        maxApiCallsPerDay = 50_000
    ),
    
    ENTERPRISE(
        displayName = "Enterprise",
        maxApplications = 25,
        maxUsers = 100,
        maxMomentsPerMonth = 1_000_000,
        maxAtomsPerApplication = 1000,
        hasAdvancedAnalytics = true,
        hasAiOptimization = true,
        hasApiAccess = true,
        maxApiCallsPerDay = 500_000
    ),
    
    UNLIMITED(
        displayName = "Unlimited",
        maxApplications = Int.MAX_VALUE,
        maxUsers = Int.MAX_VALUE,
        maxMomentsPerMonth = Long.MAX_VALUE,
        maxAtomsPerApplication = Int.MAX_VALUE,
        hasAdvancedAnalytics = true,
        hasAiOptimization = true,
        hasApiAccess = true,
        maxApiCallsPerDay = Long.MAX_VALUE
    )
}

enum class TenantStatus {
    ACTIVE,
    SUSPENDED,
    TRIAL,
    EXPIRED,
    CANCELLED
}

// =============================================================================
// BASE ENTITY CLASS
// =============================================================================

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID()
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set
    
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set
}

// =============================================================================
// TENANT-AWARE BASE ENTITY
// =============================================================================

@MappedSuperclass
abstract class TenantAwareEntity : BaseEntity() {
    
    @Column(name = "tenant_id", nullable = false)
    @JsonIgnore
    lateinit var tenantId: UUID
        protected set
    
    fun setTenant(tenantId: UUID) {
        this.tenantId = tenantId
    }
    
    fun setTenant(tenant: Tenant) {
        this.tenantId = tenant.id
    }
}

// =============================================================================
// TENANT ENTITY
// =============================================================================

@Entity
@Table(
    name = "tenants",
    indexes = [
        Index(name = "idx_tenant_subdomain", columnList = "subdomain", unique = true),
        Index(name = "idx_tenant_status", columnList = "status"),
        Index(name = "idx_tenant_subscription_tier", columnList = "subscription_tier"),
        Index(name = "idx_tenant_trial_ends_at", columnList = "trial_ends_at")
    ]
)
@FilterDef(name = "tenantFilter", parameters = [ParamDef(name = "tenantId", type = UUID::class)])
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
data class Tenant(
    
    @Column(name = "name", nullable = false, length = 100)
    @field:NotBlank(message = "Tenant name cannot be blank")
    @field:Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
    var name: String,
    
    @Column(name = "subdomain", nullable = false, length = 50, unique = true)
    @field:NotBlank(message = "Subdomain cannot be blank")
    @field:Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters")
    @field:Pattern(
        regexp = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$",
        message = "Subdomain must contain only lowercase letters, numbers, and hyphens"
    )
    var subdomain: String,
    
    @Column(name = "description", length = 500)
    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    var description: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false)
    var subscriptionTier: SubscriptionTier = SubscriptionTier.STARTER,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TenantStatus = TenantStatus.TRIAL,
    
    @Column(name = "trial_ends_at")
    var trialEndsAt: LocalDateTime? = null,
    
    @Column(name = "subscription_starts_at")
    var subscriptionStartsAt: LocalDateTime? = null,
    
    @Column(name = "subscription_ends_at")
    var subscriptionEndsAt: LocalDateTime? = null,
    
    @Column(name = "billing_email", length = 255)
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    var billingEmail: String? = null,
    
    @Column(name = "contact_email", length = 255)
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    var contactEmail: String? = null,
    
    @Column(name = "phone", length = 20)
    @field:Size(max = 20, message = "Phone cannot exceed 20 characters")
    var phone: String? = null,
    
    @Column(name = "website", length = 255)
    @field:Size(max = 255, message = "Website cannot exceed 255 characters")
    var website: String? = null,
    
    @Column(name = "logo_url", length = 500)
    @field:Size(max = 500, message = "Logo URL cannot exceed 500 characters")
    var logoUrl: String? = null,
    
    @Column(name = "primary_color", length = 7)
    @field:Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "Primary color must be a valid hex color code"
    )
    var primaryColor: String = "#0ea5e9",
    
    @Column(name = "secondary_color", length = 7)
    @field:Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "Secondary color must be a valid hex color code"
    )
    var secondaryColor: String = "#64748b",
    
    @Column(name = "timezone", length = 50)
    @field:Size(max = 50, message = "Timezone cannot exceed 50 characters")
    var timezone: String = "UTC",
    
    @Column(name = "locale", length = 10)
    @field:Size(max = 10, message = "Locale cannot exceed 10 characters")
    var locale: String = "en_US",
    
    @Column(name = "currency", length = 3)
    @field:Size(max = 3, message = "Currency cannot exceed 3 characters")
    var currency: String = "USD",
    
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    
    @Column(name = "features", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter::class)
    var enabledFeatures: MutableList<String> = mutableListOf(),
    
    @Column(name = "settings", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    var settings: MutableMap<String, Any> = mutableMapOf(),
    
    @Column(name = "usage_stats", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    var usageStats: MutableMap<String, Any> = mutableMapOf(),
    
    @OneToMany(mappedBy = "tenant", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    var applications: MutableSet<Application> = mutableSetOf(),
    
    @OneToMany(mappedBy = "tenant", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    var users: MutableSet<User> = mutableSetOf()
    
) : BaseEntity() {
    
    init {
        if (status == TenantStatus.TRIAL && trialEndsAt == null) {
            trialEndsAt = LocalDateTime.now().plusDays(14) // 14-day trial
        }
    }
    
    fun isTrialActive(): Boolean {
        return status == TenantStatus.TRIAL && 
               trialEndsAt != null && 
               LocalDateTime.now().isBefore(trialEndsAt)
    }
    
    fun isSubscriptionActive(): Boolean {
        return status == TenantStatus.ACTIVE &&
               subscriptionEndsAt != null &&
               LocalDateTime.now().isBefore(subscriptionEndsAt)
    }
    
    fun canCreateApplication(): Boolean {
        return applications.size < subscriptionTier.maxApplications
    }
    
    fun canAddUser(): Boolean {
        return users.size < subscriptionTier.maxUsers
    }
    
    fun hasFeature(feature: String): Boolean {
        return enabledFeatures.contains(feature)
    }
    
    fun enableFeature(feature: String) {
        if (!enabledFeatures.contains(feature)) {
            enabledFeatures.add(feature)
        }
    }
    
    fun disableFeature(feature: String) {
        enabledFeatures.remove(feature)
    }
    
    fun getSetting(key: String): Any? {
        return settings[key]
    }
    
    fun setSetting(key: String, value: Any) {
        settings[key] = value
    }
    
    fun incrementUsage(metric: String, amount: Long = 1) {
        val currentUsage = (usageStats[metric] as? Number)?.toLong() ?: 0L
        usageStats[metric] = currentUsage + amount
    }
    
    fun getUsage(metric: String): Long {
        return (usageStats[metric] as? Number)?.toLong() ?: 0L
    }
    
    fun resetMonthlyUsage() {
        usageStats.clear()
    }
}

// =============================================================================
// APPLICATION ENTITY
// =============================================================================

enum class ApplicationStatus {
    ACTIVE,
    INACTIVE,
    MAINTENANCE,
    SUSPENDED
}

@Entity
@Table(
    name = "applications",
    indexes = [
        Index(name = "idx_application_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_application_subdomain", columnList = "subdomain", unique = true),
        Index(name = "idx_application_status", columnList = "status"),
        Index(name = "idx_application_api_key", columnList = "api_key", unique = true)
    ]
)
@FilterDef(name = "tenantFilter", parameters = [ParamDef(name = "tenantId", type = UUID::class)])
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
data class Application(
    
    @Column(name = "name", nullable = false, length = 100)
    @field:NotBlank(message = "Application name cannot be blank")
    @field:Size(min = 2, max = 100, message = "Application name must be between 2 and 100 characters")
    var name: String,
    
    @Column(name = "subdomain", nullable = false, length = 50, unique = true)
    @field:NotBlank(message = "Subdomain cannot be blank")
    @field:Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters")
    @field:Pattern(
        regexp = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$",
        message = "Subdomain must contain only lowercase letters, numbers, and hyphens"
    )
    var subdomain: String,
    
    @Column(name = "description", length = 500)
    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    var description: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ApplicationStatus = ApplicationStatus.ACTIVE,
    
    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    var apiKey: String = generateApiKey(),
    
    @Column(name = "webhook_url", length = 500)
    @field:Size(max = 500, message = "Webhook URL cannot exceed 500 characters")
    var webhookUrl: String? = null,
    
    @Column(name = "webhook_secret", length = 64)
    var webhookSecret: String? = null,
    
    @Column(name = "cors_origins", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter::class)
    var corsOrigins: MutableList<String> = mutableListOf(),
    
    @Column(name = "rate_limit_per_minute")
    var rateLimitPerMinute: Int = 1000,
    
    @Column(name = "settings", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    var settings: MutableMap<String, Any> = mutableMapOf(),
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: MutableMap<String, Any> = mutableMapOf(),
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    var tenant: Tenant,
    
) : TenantAwareEntity() {
    
    init {
        setTenant(tenant)
    }
    
    fun regenerateApiKey(): String {
        apiKey = generateApiKey()
        return apiKey
    }
    
    fun regenerateWebhookSecret(): String {
        webhookSecret = generateWebhookSecret()
        return webhookSecret!!
    }
    
    fun addCorsOrigin(origin: String) {
        if (!corsOrigins.contains(origin)) {
            corsOrigins.add(origin)
        }
    }
    
    fun removeCorsOrigin(origin: String) {
        corsOrigins.remove(origin)
    }
    
    companion object {
        fun generateApiKey(): String {
            return "kh_" + UUID.randomUUID().toString().replace("-", "")
        }
        
        fun generateWebhookSecret(): String {
            return UUID.randomUUID().toString().replace("-", "")
        }
    }
}

// =============================================================================
// USER ENTITY
// =============================================================================

enum class UserRole {
    OWNER,
    ADMIN,
    EDITOR,
    VIEWER,
    API_USER
}

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION
}

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_user_email", columnList = "email", unique = true),
        Index(name = "idx_user_status", columnList = "status"),
        Index(name = "idx_user_role", columnList = "role")
    ]
)
@FilterDef(name = "tenantFilter", parameters = [ParamDef(name = "tenantId", type = UUID::class)])
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
data class User(
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    @field:NotBlank(message = "Email cannot be blank")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    var email: String,
    
    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonIgnore
    var passwordHash: String,
    
    @Column(name = "first_name", nullable = false, length = 50)
    @field:NotBlank(message = "First name cannot be blank")
    @field:Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    var firstName: String,
    
    @Column(name = "last_name", nullable = false, length = 50)
    @field:NotBlank(message = "Last name cannot be blank")
    @field:Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    var lastName: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole = UserRole.VIEWER,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UserStatus = UserStatus.PENDING_VERIFICATION,
    
    @Column(name = "avatar_url", length = 500)
    @field:Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
    var avatarUrl: String? = null,
    
    @Column(name = "phone", length = 20)
    @field:Size(max = 20, message = "Phone cannot exceed 20 characters")
    var phone: String? = null,
    
    @Column(name = "timezone", length = 50)
    @field:Size(max = 50, message = "Timezone cannot exceed 50 characters")
    var timezone: String = "UTC",
    
    @Column(name = "locale", length = 10)
    @field:Size(max = 10, message = "Locale cannot exceed 10 characters")
    var locale: String = "en_US",
    
    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,
    
    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,
    
    @Column(name = "email_verification_token", length = 64)
    @JsonIgnore
    var emailVerificationToken: String? = null,
    
    @Column(name = "password_reset_token", length = 64)
    @JsonIgnore
    var passwordResetToken: String? = null,
    
    @Column(name = "password_reset_expires_at")
    @JsonIgnore
    var passwordResetExpiresAt: LocalDateTime? = null,
    
    @Column(name = "preferences", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    var preferences: MutableMap<String, Any> = mutableMapOf(),
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    var tenant: Tenant
    
) : TenantAwareEntity() {
    
    init {
        setTenant(tenant)
    }
    
    val fullName: String
        get() = "$firstName $lastName"
    
    val initials: String
        get() = "${firstName.firstOrNull()?.uppercase()}${lastName.firstOrNull()?.uppercase()}"
    
    fun isActive(): Boolean = status == UserStatus.ACTIVE
    
    fun canManageUsers(): Boolean = role in listOf(UserRole.OWNER, UserRole.ADMIN)
    
    fun canEditContent(): Boolean = role in listOf(UserRole.OWNER, UserRole.ADMIN, UserRole.EDITOR)
    
    fun canViewAnalytics(): Boolean = role in listOf(UserRole.OWNER, UserRole.ADMIN, UserRole.EDITOR)
    
    fun canUseApi(): Boolean = role in listOf(UserRole.OWNER, UserRole.ADMIN, UserRole.API_USER)
    
    fun setPreference(key: String, value: Any) {
        preferences[key] = value
    }
    
    fun getPreference(key: String): Any? = preferences[key]
    
    fun generateEmailVerificationToken(): String {
        emailVerificationToken = UUID.randomUUID().toString().replace("-", "")
        return emailVerificationToken!!
    }
    
    fun generatePasswordResetToken(): String {
        passwordResetToken = UUID.randomUUID().toString().replace("-", "")
        passwordResetExpiresAt = LocalDateTime.now().plusHours(24)
        return passwordResetToken!!
    }
    
    fun isPasswordResetTokenValid(): Boolean {
        return passwordResetToken != null && 
               passwordResetExpiresAt != null && 
               LocalDateTime.now().isBefore(passwordResetExpiresAt)
    }
}

// =============================================================================
// CONVERTER CLASSES
// =============================================================================

@Converter
class StringListConverter : AttributeConverter<MutableList<String>, String> {
    
    override fun convertToDatabaseColumn(attribute: MutableList<String>?): String? {
        return attribute?.joinToString(",")
    }
    
    override fun convertToEntityAttribute(dbData: String?): MutableList<String> {
        return if (dbData.isNullOrBlank()) {
            mutableListOf()
        } else {
            dbData.split(",").toMutableList()
        }
    }
}