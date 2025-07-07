// =============================================================================
// File: src/main/kotlin/com/kairos/hades/controller/UserController.kt
// 🔥 HADES BACKEND - USER REST CONTROLLER
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// Purpose: REST API endpoints for User management with multi-tenant support
// =============================================================================

package com.kairos.hades.controller

import com.kairos.hades.dto.*
import com.kairos.hades.entity.User
import com.kairos.hades.entity.UserRole
import com.kairos.hades.entity.UserStatus
import com.kairos.hades.service.UserService
import com.kairos.hades.security.SecurityContext
import com.kairos.hades.util.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponseAnnotation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST Controller for User management operations
 * Provides comprehensive user CRUD operations with security and validation
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User management operations including registration, authentication, and profile management")
@SecurityRequirement(name = "Bearer Authentication")
class UserController(
    private val userService: UserService,
    private val securityContext: SecurityContext
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(UserController::class.java)
    }
    
    // ==========================================================================
    // USER REGISTRATION & CREATION
    // ==========================================================================
    
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Public endpoint for user registration. Creates a new user account."
    )
    @ApiResponseAnnotation(responseCode = "201", description = "User registered successfully")
    @ApiResponseAnnotation(responseCode = "400", description = "Invalid registration data")
    @ApiResponseAnnotation(responseCode = "409", description = "User already exists")
    fun registerUser(
        @Valid @RequestBody registrationRequest: UserRegistrationRequest,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        logger.info("User registration attempt: {}", registrationRequest.email)
        
        try {
            val user = userService.registerUser(registrationRequest)
            val userResponse = UserResponse.from(user)
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userResponse, "User registered successfully"))
        } catch (e: Exception) {
            logger.error("Registration failed for email: {}", registrationRequest.email, e)
            throw e
        }
    }
    
    @PostMapping
    @Operation(
        summary = "Create a new user",
        description = "Admin endpoint to create a new user with specific roles and permissions"
    )
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @ApiResponseAnnotation(responseCode = "201", description = "User created successfully")
    @ApiResponseAnnotation(responseCode = "400", description = "Invalid user data")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    @ApiResponseAnnotation(responseCode = "409", description = "User already exists")
    fun createUser(
        @Valid @RequestBody createUserRequest: CreateUserRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        logger.info("Creating user: {} by admin", createUserRequest.email)
        
        val user = userService.createUser(createUserRequest)
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(userResponse, "User created successfully"))
    }
    
    // ==========================================================================
    // USER RETRIEVAL
    // ==========================================================================
    
    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieve a paginated list of users within the current tenant"
    )
    @PreAuthorize("hasAuthority('USER_READ')")
    @ApiResponseAnnotation(responseCode = "200", description = "Users retrieved successfully")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    fun getUsers(
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") sortDir: String,
        @Parameter(description = "Search term") @RequestParam(required = false) search: String?
    ): ResponseEntity<ApiResponse<Page<UserResponse>>> {
        logger.debug("Retrieving users: page={}, size={}, sortBy={}", page, size, sortBy)
        
        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable: Pageable = PageRequest.of(page, size, sort)
        
        val users = if (search.isNullOrBlank()) {
            userService.findUsers(pageable)
        } else {
            userService.searchUsers(search, pageable)
        }
        
        val userResponses = users.map { UserResponse.from(it) }
        
        return ResponseEntity.ok(ApiResponse.success(userResponses, "Users retrieved successfully"))
    }
    
    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve a specific user by their ID"
    )
    @PreAuthorize("hasAuthority('USER_READ') or @securityContext.getCurrentUserId() == #userId")
    @ApiResponseAnnotation(responseCode = "200", description = "User retrieved successfully")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    @ApiResponseAnnotation(responseCode = "404", description = "User not found")
    fun getUserById(
        @Parameter(description = "User ID") @PathVariable userId: UUID
    ): ResponseEntity<ApiResponse<UserResponse>> {
        logger.debug("Retrieving user by ID: {}", userId)
        
        val user = userService.findByIdOrThrow(userId)
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User retrieved successfully"))
    }
    
    @GetMapping("/profile")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieve the profile of the currently authenticated user"
    )
    @ApiResponseAnnotation(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponseAnnotation(responseCode = "401", description = "User not authenticated")
    fun getCurrentUserProfile(): ResponseEntity<ApiResponse<UserResponse>> {
        val currentUserId = securityContext.getCurrentUserId()
            ?: throw IllegalStateException("No authenticated user")
        
        logger.debug("Retrieving current user profile: {}", currentUserId)
        
        val user = userService.findByIdOrThrow(UUID.fromString(currentUserId))
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.ok(ApiResponse.success(userResponse, "Profile retrieved successfully"))
    }
    
    @GetMapping("/by-email/{email}")
    @Operation(
        summary = "Get user by email",
        description = "Retrieve a user by their email address"
    )
    @PreAuthorize("hasAuthority('USER_READ')")
    @ApiResponseAnnotation(responseCode = "200", description = "User retrieved successfully")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    @ApiResponseAnnotation(responseCode = "404", description = "User not found")
    fun getUserByEmail(
        @Parameter(description = "User email") @PathVariable email: String
    ): ResponseEntity<ApiResponse<UserResponse>> {
        logger.debug("Retrieving user by email: {}", email)
        
        val user = userService.findByEmailOrThrow(email)
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User retrieved successfully"))
    }
    
    // ==========================================================================
    // USER UPDATES
    // ==========================================================================
    
    @PutMapping("/{userId}")
    @Operation(
        summary = "Update user",
        description = "Update user information"
    )
    @PreAuthorize("hasAuthority('USER_WRITE') or @securityContext.getCurrentUserId() == #userId")
    @ApiResponseAnnotation(responseCode = "200", description = "User updated successfully")
    @ApiResponseAnnotation(responseCode = "400", description = "Invalid update data")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    @ApiResponseAnnotation(responseCode = "404", description = "User not found")
    @ApiResponseAnnotation(responseCode = "409", description = "Email already exists")
    fun updateUser(
        @Parameter(description = "User ID") @PathVariable userId: UUID,
        @Valid @RequestBody updateRequest: UpdateUserRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        logger.info("Updating user: {}", userId)
        
        val user = userService.updateUser(userId, updateRequest)
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User updated successfully"))
    }
    
    @PutMapping("/profile")
    @Operation(
        summary = "Update current user profile",
        description = "Update the profile of the currently authenticated user"
    )
    @ApiResponseAnnotation(responseCode = "200", description = "Profile updated successfully")
    @ApiResponseAnnotation(responseCode = "400", description = "Invalid profile data")
    @ApiResponseAnnotation(responseCode = "401", description = "User not authenticated")
    fun updateCurrentUserProfile(
        @Valid @RequestBody updateRequest: UpdateUserRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val currentUserId = securityContext.getCurrentUserId()
            ?: throw IllegalStateException("No authenticated user")
        
        logger.info("Updating current user profile: {}", currentUserId)
        
        val user = userService.updateUser(UUID.fromString(currentUserId), updateRequest)
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.ok(ApiResponse.success(userResponse, "Profile updated successfully"))
    }
    
    @PutMapping("/{userId}/password")
    @Operation(
        summary = "Update user password",
        description = "Update user password with current password verification"
    )
    @PreAuthorize("@securityContext.getCurrentUserId() == #userId")
    @ApiResponseAnnotation(responseCode = "200", description = "Password updated successfully")
    @ApiResponseAnnotation(responseCode = "400", description = "Invalid password data")
    @ApiResponseAnnotation(responseCode = "401", description = "Current password incorrect")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    fun updatePassword(
        @Parameter(description = "User ID") @PathVariable userId: UUID,
        @Valid @RequestBody passwordRequest: UpdatePasswordRequest
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("Updating password for user: {}", userId)
        
        userService.updatePassword(userId, passwordRequest.currentPassword, passwordRequest.newPassword)
        
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"))
    }
    
    // ==========================================================================
    // USER STATUS MANAGEMENT
    // ==========================================================================
    
    @PutMapping("/{userId}/activate")
    @Operation(
        summary = "Activate user",
        description = "Activate a user account"
    )
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @ApiResponseAnnotation(responseCode = "200", description = "User activated successfully")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    @ApiResponseAnnotation(responseCode = "404", description = "User not found")
    fun activateUser(
        @Parameter(description = "User ID") @PathVariable userId: UUID
    ): ResponseEntity<ApiResponse<UserResponse>> {
        logger.info("Activating user: {}", userId)
        
        val user = userService.activateUser(userId)
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User activated successfully"))
    }
    
    @PutMapping("/{userId}/deactivate")
    @Operation(
        summary = "Deactivate user",
        description = "Deactivate a user account"
    )
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @ApiResponseAnnotation(responseCode = "200", description = "User deactivated successfully")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    @ApiResponseAnnotation(responseCode = "404", description = "User not found")
    fun deactivateUser(
        @Parameter(description = "User ID") @PathVariable userId: UUID,
        @RequestBody(required = false) request: DeactivateUserRequest?
    ): ResponseEntity<ApiResponse<UserResponse>> {
        logger.info("Deactivating user: {}", userId)
        
        val user = userService.deactivateUser(userId, request?.reason)
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User deactivated successfully"))
    }
    
    @DeleteMapping("/{userId}")
    @Operation(
        summary = "Delete user",
        description = "Soft delete a user account"
    )
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @ApiResponseAnnotation(responseCode = "200", description = "User deleted successfully")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    @ApiResponseAnnotation(responseCode = "404", description = "User not found")
    @ApiResponseAnnotation(responseCode = "409", description = "User cannot be deleted")
    fun deleteUser(
        @Parameter(description = "User ID") @PathVariable userId: UUID
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("Deleting user: {}", userId)
        
        userService.deleteUser(userId)
        
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"))
    }
    
    // ==========================================================================
    // EMAIL VERIFICATION
    // ==========================================================================
    
    @PostMapping("/verify-email")
    @Operation(
        summary = "Verify email address",
        description = "Verify user email address with verification token"
    )
    @ApiResponseAnnotation(responseCode = "200", description = "Email verified successfully")
    @ApiResponseAnnotation(responseCode = "400", description = "Invalid verification token")
    fun verifyEmail(
        @Valid @RequestBody verificationRequest: EmailVerificationRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        logger.info("Verifying email with token: {}...", verificationRequest.token.take(8))
        
        val user = userService.verifyEmail(verificationRequest.token)
        val userResponse = UserResponse.from(user)
        
        return ResponseEntity.ok(ApiResponse.success(userResponse, "Email verified successfully"))
    }
    
    @PostMapping("/resend-verification")
    @Operation(
        summary = "Resend email verification",
        description = "Resend email verification link to user"
    )
    @ApiResponseAnnotation(responseCode = "200", description = "Verification email sent")
    @ApiResponseAnnotation(responseCode = "400", description = "Email already verified")
    fun resendEmailVerification(
        @Valid @RequestBody resendRequest: ResendVerificationRequest
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("Resending email verification for: {}", resendRequest.email)
        
        val sent = userService.resendEmailVerification(resendRequest.email)
        
        return if (sent) {
            ResponseEntity.ok(ApiResponse.success("Verification email sent"))
        } else {
            ResponseEntity.ok(ApiResponse.success("If the email exists, verification will be sent"))
        }
    }
    
    // ==========================================================================
    // PASSWORD RESET
    // ==========================================================================
    
    @PostMapping("/forgot-password")
    @Operation(
        summary = "Initiate password reset",
        description = "Send password reset email to user"
    )
    @ApiResponseAnnotation(responseCode = "200", description = "Password reset email sent")
    fun forgotPassword(
        @Valid @RequestBody forgotPasswordRequest: ForgotPasswordRequest
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("Password reset requested for: {}", forgotPasswordRequest.email)
        
        userService.initiatePasswordReset(forgotPasswordRequest.email)
        
        return ResponseEntity.ok(
            ApiResponse.success("If the email exists, password reset instructions will be sent")
        )
    }
    
    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password",
        description = "Reset user password with reset token"
    )
    @ApiResponseAnnotation(responseCode = "200", description = "Password reset successfully")
    @ApiResponseAnnotation(responseCode = "400", description = "Invalid reset token or password")
    fun resetPassword(
        @Valid @RequestBody resetRequest: ResetPasswordRequest
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("Resetting password with token: {}...", resetRequest.token.take(8))
        
        userService.resetPassword(resetRequest.token, resetRequest.newPassword)
        
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"))
    }
    
    // ==========================================================================
    // USER STATISTICS & ANALYTICS
    // ==========================================================================
    
    @GetMapping("/statistics")
    @Operation(
        summary = "Get user statistics",
        description = "Retrieve user statistics for the current tenant"
    )
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @ApiResponseAnnotation(responseCode = "200", description = "Statistics retrieved successfully")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    fun getUserStatistics(): ResponseEntity<ApiResponse<UserStatistics>> {
        logger.debug("Retrieving user statistics")
        
        val statistics = userService.getUserStatistics()
        
        return ResponseEntity.ok(ApiResponse.success(statistics, "Statistics retrieved successfully"))
    }
    
    @GetMapping("/recent")
    @Operation(
        summary = "Get recently created users",
        description = "Retrieve recently created users within the current tenant"
    )
    @PreAuthorize("hasAuthority('USER_READ')")
    @ApiResponseAnnotation(responseCode = "200", description = "Recent users retrieved successfully")
    fun getRecentUsers(
        @Parameter(description = "Number of days to look back") @RequestParam(defaultValue = "30") days: Int,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<UserResponse>>> {
        logger.debug("Retrieving recent users for last {} days", days)
        
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val since = java.time.LocalDateTime.now().minusDays(days.toLong())
        
        // This would need to be implemented in the service
        val users = userService.findUsers(pageable) // Simplified for now
        val userResponses = users.map { UserResponse.from(it) }
        
        return ResponseEntity.ok(ApiResponse.success(userResponses, "Recent users retrieved successfully"))
    }
    
    // ==========================================================================
    // BULK OPERATIONS
    // ==========================================================================
    
    @PostMapping("/bulk/status")
    @Operation(
        summary = "Bulk update user status",
        description = "Update status for multiple users"
    )
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @ApiResponseAnnotation(responseCode = "200", description = "Users updated successfully")
    @ApiResponseAnnotation(responseCode = "403", description = "Insufficient permissions")
    fun bulkUpdateUserStatus(
        @Valid @RequestBody bulkRequest: BulkUserStatusRequest
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("Bulk updating status for {} users", bulkRequest.userIds.size)
        
        bulkRequest.userIds.forEach { userId ->
            try {
                when (bulkRequest.status) {
                    UserStatus.ACTIVE -> userService.activateUser(userId)
                    UserStatus.INACTIVE -> userService.deactivateUser(userId, bulkRequest.reason)
                    else -> throw IllegalArgumentException("Invalid status for bulk operation")
                }
            } catch (e: Exception) {
                logger.error("Failed to update status for user: {}", userId, e)
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("Bulk status update completed"))
    }
    
    // ==========================================================================
    // HEALTH CHECK
    // ==========================================================================
    
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check the health of the user service"
    )
    fun healthCheck(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val health = mapOf(
            "status" to "UP",
            "timestamp" to java.time.LocalDateTime.now(),
            "service" to "UserService"
        )
        
        return ResponseEntity.ok(ApiResponse.success(health, "Service is healthy"))
    }
}