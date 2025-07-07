// =============================================================================
// File: src/main/kotlin/com/kairos/hades/dto/UserDTOs.kt
// 🔥 HADES BACKEND - USER DATA TRANSFER OBJECTS
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// Purpose: DTOs for User API requests and responses with validation
// =============================================================================

package com.kairos.hades.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kairos.hades.entity.User
import com.kairos.hades.entity.UserRole
import com.kairos.hades.entity.UserStatus
import jakarta.validation.constraints.*
import java.time.LocalDateTime
import java.util.*

// =============================================================================
// REQUEST DTOs
// =============================================================================

/**
 * Request DTO for user registration (public endpoint)
 */
data class UserRegistrationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&].*$",
        message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character"
    )
    val password: String,
    
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name must not exceed 100 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z\\s'-]+$",
        message = "First name can only contain letters, spaces, apostrophes, and hyphens"
    )
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name must not exceed 100 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z\\s'-]+$",
        message = "Last name can only contain letters, spaces, apostrophes, and hyphens"
    )
    val lastName: String,
    
    @field:Size(max = 255, message = "Display name must not exceed 255 characters")
    val displayName: String? = null,
    
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be a valid international format"
    )
    val phone: String? = null,
    
    @field:Size(max = 50, message = "Timezone must not exceed 50 characters")
    val timezone: String? = null,
    
    @field:Size(max = 10, message = "Locale must not exceed 10 characters")
    @field:Pattern(
        regexp = "^[a-z]{2}(-[A-Z]{2})?$",
        message = "Locale must be in format 'en' or 'en-US'"
    )
    val locale: String? = null,
    
    val tenantId: String? = null,
    
    val metadata: String? = null
)

/**
 * Request DTO for creating users (admin endpoint)
 */
data class CreateUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val password: String,
    
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name must not exceed 100 characters")
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name must not exceed 100 characters")
    val lastName: String,
    
    @field:Size(max = 255, message = "Display name must not exceed 255 characters")
    val displayName: String? = null,
    
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be a valid international format"
    )
    val phone: String? = null,
    
    @field:Size(max = 50, message = "Timezone must not exceed 50 characters")
    val timezone: String? = null,
    
    @field:Size(max = 10, message = "Locale must not exceed 10 characters")
    val locale: String? = null,
    
    val role: UserRole? = null,
    
    val autoActivate: Boolean? = false,
    
    val metadata: String? = null
)

/**
 * Request DTO for updating user information
 */
data class UpdateUserRequest(
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String? = null,
    
    @field:Size(max = 100, message = "First name must not exceed 100 characters")
    val firstName: String? = null,
    
    @field:Size(max = 100, message = "Last name must not exceed 100 characters")
    val lastName: String? = null,
    
    @field:Size(max = 255, message = "Display name must not exceed 255 characters")
    val displayName: String? = null,
    
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be a valid international format"
    )
    val phone: String? = null,
    
    @field:Size(max = 50, message = "Timezone must not exceed 50 characters")
    val timezone: String? = null,
    
    @field:Size(max = 10, message = "Locale must not exceed 10 characters")
    val locale: String? = null,
    
    val role: UserRole? = null,
    
    val status: UserStatus? = null,
    
    val metadata: String? = null
)

/**
 * Request DTO for password updates
 */
data class UpdatePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&].*$",
        message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character"
    )
    val newPassword: String,
    
    @field:NotBlank(message = "Password confirmation is required")
    val confirmPassword: String
) {
    init {
        require(newPassword == confirmPassword) { "Password confirmation does not match" }
    }
}

/**
 * Request DTO for email verification
 */
data class EmailVerificationRequest(
    @field:NotBlank(message = "Verification token is required")
    @field:Size(min = 32, max = 128, message = "Invalid token format")
    val token: String
)

/**
 * Request DTO for resending email verification
 */
data class ResendVerificationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String
)

/**
 * Request DTO for password reset initiation
 */
data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String
)

/**
 * Request DTO for password reset completion
 */
data class ResetPasswordRequest(
    @field:NotBlank(message = "Reset token is required")
    @field:Size(min = 32, max = 128, message = "Invalid token format")
    val token: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&].*$",
        message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character"
    )
    val newPassword: String,
    
    @field:NotBlank(message = "Password confirmation is required")
    val confirmPassword: String
) {
    init {
        require(newPassword == confirmPassword) { "Password confirmation does not match" }
    }
}

/**
 * Request DTO for user deactivation
 */
data class DeactivateUserRequest(
    @field:Size(max = 500, message = "Reason must not exceed 500 characters")
    val reason: String? = null
)

/**
 * Request DTO for bulk user status updates
 */
data class BulkUserStatusRequest(
    @field:NotEmpty(message = "User IDs list cannot be empty")
    @field:Size(max = 100, message = "Cannot update more than 100 users at once")
    val userIds: List<UUID>,
    
    @field:NotNull(message = "Status is required")
    val status: UserStatus,
    
    @field:Size(max = 500, message = "Reason must not exceed 500 characters")
    val reason: String? = null
)

// =============================================================================
// RESPONSE DTOs
// =============================================================================

/**
 * Response DTO for user information
 */
data class UserResponse(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val displayName: String?,
    val fullName: String,
    val displayNameOrFull: String,
    val phone: String?,
    val timezone: String?,
    val locale: String?,
    val role: UserRole,
    val status: UserStatus,
    val isEmailVerified: Boolean,
    val isAdmin: Boolean,
    val isLocked: Boolean,
    val canBeDeleted: Boolean,
    val lastLoginAt: LocalDateTime?,
    val lastLoginIp: String?,
    val loginCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val metadata: String?
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                displayName = user.displayName,
                fullName = user.fullName,
                displayNameOrFull = user.getDisplayNameOrFullName(),
                phone = user.phone,
                timezone = user.timezone,
                locale = user.locale,
                role = user.role,
                status = user.status,
                isEmailVerified = user.isEmailVerified,
                isAdmin = user.isAdmin,
                isLocked = user.isLocked,
                canBeDeleted = user.isDeletable,
                lastLoginAt = user.lastLoginAt,
                lastLoginIp = user.lastLoginIp,
                loginCount = user.loginCount,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                metadata = user.metadata
            )
        }
    }
}

/**
 * Simplified user response for lists and references
 */
data class UserSummaryResponse(
    val id: UUID,
    val email: String,
    val fullName: String,
    val displayNameOrFull: String,
    val role: UserRole,
    val status: UserStatus,
    val isEmailVerified: Boolean,
    val lastLoginAt: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserSummaryResponse {
            return UserSummaryResponse(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                displayNameOrFull = user.getDisplayNameOrFullName(),
                role = user.role,
                status = user.status,
                isEmailVerified = user.isEmailVerified,
                lastLoginAt = user.lastLoginAt,
                createdAt = user.createdAt
            )
        }
    }
}

/**
 * User statistics response
 */
data class UserStatistics(
    val totalActiveUsers: Long,
    val newUsersLast30Days: Long,
    val activeUsersLast30Days: Long,
    val totalInactiveUsers: Long = 0,
    val totalPendingUsers: Long = 0,
    val averageLoginFrequency: Double = 0.0,
    val topActiveUsers: List<UserSummaryResponse> = emptyList()
)

/**
 * User activity response
 */
data class UserActivityResponse(
    val userId: UUID,
    val email: String,
    val fullName: String,
    val lastLoginAt: LocalDateTime?,
    val loginCount: Long,
    val failedLoginAttempts: Int,
    val isLocked: Boolean,
    val accountLockedUntil: LocalDateTime?
)

/**
 * User profile response with sensitive information for current user
 */
data class UserProfileResponse(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val displayName: String?,
    val fullName: String,
    val phone: String?,
    val timezone: String?,
    val locale: String?,
    val role: UserRole,
    val status: UserStatus,
    val isEmailVerified: Boolean,
    val emailVerifiedAt: LocalDateTime?,
    val lastLoginAt: LocalDateTime?,
    val lastLoginIp: String?,
    val loginCount: Long,
    val failedLoginAttempts: Int,
    val isLocked: Boolean,
    val accountLockedUntil: LocalDateTime?,
    val shouldUpdatePassword: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val preferences: String?,
    val metadata: String?
) {
    companion object {
        fun from(user: User): UserProfileResponse {
            return UserProfileResponse(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                displayName = user.displayName,
                fullName = user.fullName,
                phone = user.phone,
                timezone = user.timezone,
                locale = user.locale,
                role = user.role,
                status = user.status,
                isEmailVerified = user.isEmailVerified,
                emailVerifiedAt = user.emailVerifiedAt,
                lastLoginAt = user.lastLoginAt,
                lastLoginIp = user.lastLoginIp,
                loginCount = user.loginCount,
                failedLoginAttempts = user.failedLoginAttempts,
                isLocked = user.isLocked,
                accountLockedUntil = user.accountLockedUntil,
                shouldUpdatePassword = user.shouldUpdatePassword(),
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                preferences = user.preferences,
                metadata = user.metadata
            )
        }
    }
}

// =============================================================================
// AUTHENTICATION DTOs
// =============================================================================

/**
 * Login request DTO
 */
data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    val password: String,
    
    val rememberMe: Boolean = false,
    
    val tenantId: String? = null
)

/**
 * Login response DTO
 */
data class LoginResponse(
    val user: UserResponse,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshExpiresIn: Long
)

/**
 * Token refresh request DTO
 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

/**
 * Token refresh response DTO
 */
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshExpiresIn: Long
)

// =============================================================================
// VALIDATION DTOs
// =============================================================================

/**
 * Email availability check request
 */
data class EmailAvailabilityRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
    
    val excludeUserId: UUID? = null
)

/**
 * Email availability check response
 */
data class EmailAvailabilityResponse(
    val email: String,
    val isAvailable: Boolean,
    val suggestions: List<String> = emptyList()
)

/**
 * Password strength check request
 */
data class PasswordStrengthRequest(
    @field:NotBlank(message = "Password is required")
    val password: String
)

/**
 * Password strength check response
 */
data class PasswordStrengthResponse(
    val score: Int, // 0-4 (0=very weak, 4=very strong)
    val isValid: Boolean,
    val feedback: List<String>,
    val estimatedCrackTime: String
)