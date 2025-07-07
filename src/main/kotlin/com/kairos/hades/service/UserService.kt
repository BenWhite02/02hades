// =============================================================================
// File: src/main/kotlin/com/kairos/hades/service/UserService.kt
// 🔥 HADES BACKEND - USER SERVICE LAYER
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// Purpose: Business logic layer for User management with multi-tenant support
// =============================================================================

package com.kairos.hades.service

import com.kairos.hades.entity.*
import com.kairos.hades.repository.UserRepository
import com.kairos.hades.exception.*
import com.kairos.hades.dto.*
import com.kairos.hades.security.SecurityContext
import com.kairos.hades.util.PasswordUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

/**
 * Service class for User management with comprehensive business logic
 * Implements UserDetailsService for Spring Security integration
 */
@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val securityContext: SecurityContext,
    private val emailService: EmailService,
    private val auditService: AuditService
) : UserDetailsService {
    
    companion object {
        private val logger = LoggerFactory.getLogger(UserService::class.java)
        private const val DEFAULT_LOCK_DURATION_MINUTES = 30L
        private const val MAX_FAILED_LOGIN_ATTEMPTS = 5
        private const val PASSWORD_RESET_TOKEN_VALIDITY_HOURS = 24L
        private const val EMAIL_VERIFICATION_TOKEN_VALIDITY_HOURS = 72L
    }
    
    // ==========================================================================
    // USERDETAILS SERVICE IMPLEMENTATION
    // ==========================================================================
    
    @Transactional(readOnly = true)
    override fun loadUserByUsername(email: String): UserDetails {
        val tenantId = securityContext.getCurrentTenantId()
            ?: throw UsernameNotFoundException("No tenant context available")
        
        logger.debug("Loading user by email: {} for tenant: {}", email, tenantId)
        
        return userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
            ?: throw UsernameNotFoundException("User not found: $email")
    }
    
    // ==========================================================================
    // USER CREATION & REGISTRATION
    // ==========================================================================
    
    /**
     * Create a new user with validation and tenant assignment
     */
    fun createUser(createUserRequest: CreateUserRequest): User {
        val tenantId = securityContext.getCurrentTenantId()
            ?: throw IllegalStateException("No tenant context available")
        
        logger.info("Creating user: {} for tenant: {}", createUserRequest.email, tenantId)
        
        // Validate email uniqueness within tenant
        if (userRepository.existsByTenantIdAndEmailIgnoreCase(tenantId, createUserRequest.email)) {
            throw UserAlreadyExistsException("User with email ${createUserRequest.email} already exists")
        }
        
        // Validate password strength
        PasswordUtil.validatePasswordStrength(createUserRequest.password)
        
        // Hash password
        val passwordHash = passwordEncoder.encode(createUserRequest.password)
        
        // Generate email verification token
        val emailVerificationToken = generateSecureToken()
        
        // Create user entity
        val user = User(
            tenantId = tenantId,
            email = createUserRequest.email,
            passwordHash = passwordHash,
            firstName = createUserRequest.firstName,
            lastName = createUserRequest.lastName,
            displayName = createUserRequest.displayName,
            phone = createUserRequest.phone,
            timezone = createUserRequest.timezone ?: "UTC",
            locale = createUserRequest.locale ?: "en",
            role = createUserRequest.role ?: UserRole.USER,
            status = if (createUserRequest.autoActivate == true) UserStatus.ACTIVE else UserStatus.PENDING,
            emailVerificationToken = if (createUserRequest.autoActivate == true) null else emailVerificationToken,
            emailVerifiedAt = if (createUserRequest.autoActivate == true) LocalDateTime.now() else null,
            createdBy = securityContext.getCurrentUserId(),
            metadata = createUserRequest.metadata
        )
        
        val savedUser = userRepository.save(user)
        
        // Send email verification if not auto-activated
        if (createUserRequest.autoActivate != true && emailVerificationToken != null) {
            emailService.sendEmailVerification(savedUser.email, emailVerificationToken)
        }
        
        // Send welcome email if auto-activated
        if (createUserRequest.autoActivate == true) {
            emailService.sendWelcomeEmail(savedUser.email, savedUser.firstName)
        }
        
        // Audit log
        auditService.logUserCreation(savedUser.id, savedUser.email, tenantId)
        
        logger.info("User created successfully: {} with ID: {}", savedUser.email, savedUser.id)
        return savedUser
    }
    
    /**
     * Register a new user (public registration)
     */
    fun registerUser(registrationRequest: UserRegistrationRequest): User {
        val tenantId = registrationRequest.tenantId
            ?: throw IllegalArgumentException("Tenant ID is required for registration")
        
        logger.info("Registering user: {} for tenant: {}", registrationRequest.email, tenantId)
        
        // Set tenant context for this operation
        securityContext.setTenantId(tenantId)
        
        val createRequest = CreateUserRequest(
            email = registrationRequest.email,
            password = registrationRequest.password,
            firstName = registrationRequest.firstName,
            lastName = registrationRequest.lastName,
            displayName = registrationRequest.displayName,
            phone = registrationRequest.phone,
            timezone = registrationRequest.timezone,
            locale = registrationRequest.locale,
            role = UserRole.USER, // Default role for public registration
            autoActivate = false,
            metadata = registrationRequest.metadata
        )
        
        return createUser(createRequest)
    }
    
    // ==========================================================================
    // USER RETRIEVAL
    // ==========================================================================
    
    @Transactional(readOnly = true)
    fun findById(userId: UUID): User? {
        val tenantId = securityContext.getCurrentTenantId()
            ?: throw IllegalStateException("No tenant context available")
        
        return userRepository.findById(userId)
            .filter { it.tenantId == tenantId && it.deletedAt == null }
            .orElse(null)
    }
    
    @Transactional(readOnly = true)
    fun findByIdOrThrow(userId: UUID): User {
        return findById(userId) ?: throw UserNotFoundException("User not found: $userId")
    }
    
    @Transactional(readOnly = true)
    fun findByEmail(email: String): User? {
        val tenantId = securityContext.getCurrentTenantId()
            ?: throw IllegalStateException("No tenant context available")
        
        return userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
    }
    
    @Transactional(readOnly = true)
    fun findByEmailOrThrow(email: String): User {
        return findByEmail(email) ?: throw UserNotFoundException("User not found: $email")
    }
    
    @Transactional(readOnly = true)
    fun findUsers(pageable: Pageable): Page<User> {
        val tenantId = securityContext.getCurrentTenantId()
            ?: throw IllegalStateException("No tenant context available")
        
        return userRepository.findByTenantIdAndStatus(tenantId, UserStatus.ACTIVE, pageable)
    }
    
    @Transactional(readOnly = true)
    fun searchUsers(searchTerm: String, pageable: Pageable): Page<User> {
        val tenantId = securityContext.getCurrentTenantId()
            ?: throw IllegalStateException("No tenant context available")
        
        return userRepository.searchByTenantIdAndNameOrEmail(tenantId, searchTerm, pageable)
    }
    
    // ==========================================================================
    // USER UPDATES
    // ==========================================================================
    
    fun updateUser(userId: UUID, updateRequest: UpdateUserRequest): User {
        val existingUser = findByIdOrThrow(userId)
        
        logger.info("Updating user: {} ({})", existingUser.email, userId)
        
        // Check if email is being changed and validate uniqueness
        if (updateRequest.email != null && updateRequest.email != existingUser.email) {
            val tenantId = securityContext.getCurrentTenantId()!!
            if (userRepository.existsByTenantIdAndEmailIgnoreCase(tenantId, updateRequest.email)) {
                throw UserAlreadyExistsException("User with email ${updateRequest.email} already exists")
            }
        }
        
        val updatedUser = existingUser.copy(
            email = updateRequest.email ?: existingUser.email,
            firstName = updateRequest.firstName ?: existingUser.firstName,
            lastName = updateRequest.lastName ?: existingUser.lastName,
            displayName = updateRequest.displayName ?: existingUser.displayName,
            phone = updateRequest.phone ?: existingUser.phone,
            timezone = updateRequest.timezone ?: existingUser.timezone,
            locale = updateRequest.locale ?: existingUser.locale,
            role = updateRequest.role ?: existingUser.role,
            status = updateRequest.status ?: existingUser.status,
            metadata = updateRequest.metadata ?: existingUser.metadata,
            updatedAt = LocalDateTime.now(),
            updatedBy = securityContext.getCurrentUserId()
        )
        
        val savedUser = userRepository.save(updatedUser)
        
        // Audit log
        auditService.logUserUpdate(userId, existingUser.email, updateRequest)
        
        logger.info("User updated successfully: {}", savedUser.email)
        return savedUser
    }
    
    fun updatePassword(userId: UUID, currentPassword: String, newPassword: String): User {
        val user = findByIdOrThrow(userId)
        
        logger.info("Updating password for user: {}", user.email)
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            throw InvalidPasswordException("Current password is incorrect")
        }
        
        // Validate new password strength
        PasswordUtil.validatePasswordStrength(newPassword)
        
        // Ensure new password is different from current
        if (passwordEncoder.matches(newPassword, user.passwordHash)) {
            throw InvalidPasswordException("New password must be different from current password")
        }
        
        val newPasswordHash = passwordEncoder.encode(newPassword)
        val updatedUser = user.withPassword(newPasswordHash)
        
        val savedUser = userRepository.save(updatedUser)
        
        // Clear any password reset tokens
        if (user.passwordResetToken != null) {
            val clearedUser = savedUser.copy(
                passwordResetToken = null,
                passwordResetExpiresAt = null,
                updatedAt = LocalDateTime.now()
            )
            userRepository.save(clearedUser)
        }
        
        // Audit log
        auditService.logPasswordChange(userId, user.email)
        
        logger.info("Password updated successfully for user: {}", user.email)
        return savedUser
    }
    
    // ==========================================================================
    // AUTHENTICATION & SECURITY
    // ==========================================================================
    
    fun recordSuccessfulLogin(email: String, ipAddress: String): User {
        val user = findByEmailOrThrow(email)
        
        logger.debug("Recording successful login for user: {}", email)
        
        val updatedUser = user.withLastLogin(ipAddress)
        val savedUser = userRepository.save(updatedUser)
        
        // Audit log
        auditService.logUserLogin(savedUser.id, email, ipAddress, true)
        
        return savedUser
    }
    
    fun recordFailedLogin(email: String, ipAddress: String) {
        val user = findByEmail(email) ?: return
        
        logger.warn("Recording failed login for user: {}", email)
        
        val updatedUser = user.withFailedLogin()
        
        // Auto-lock account if too many failed attempts
        val finalUser = if (updatedUser.failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            val lockUntil = LocalDateTime.now().plusMinutes(DEFAULT_LOCK_DURATION_MINUTES)
            updatedUser.withLockUntil(lockUntil)
        } else {
            updatedUser
        }
        
        userRepository.save(finalUser)
        
        // Audit log
        auditService.logUserLogin(user.id, email, ipAddress, false)
        
        if (finalUser.isLocked) {
            logger.warn("User account locked due to excessive failed login attempts: {}", email)
            emailService.sendAccountLockedNotification(user.email, finalUser.accountLockedUntil!!)
        }
    }
    
    // ==========================================================================
    // PASSWORD RESET
    // ==========================================================================
    
    fun initiatePasswordReset(email: String): Boolean {
        val user = findByEmail(email) ?: return false // Don't reveal if email exists
        
        logger.info("Initiating password reset for user: {}", email)
        
        val resetToken = generateSecureToken()
        val expiresAt = LocalDateTime.now().plusHours(PASSWORD_RESET_TOKEN_VALIDITY_HOURS)
        
        val updatedUser = user.withPasswordReset(resetToken, expiresAt)
        userRepository.save(updatedUser)
        
        emailService.sendPasswordResetEmail(user.email, resetToken)
        
        // Audit log
        auditService.logPasswordResetRequest(user.id, email)
        
        return true
    }
    
    fun resetPassword(token: String, newPassword: String): User {
        logger.info("Resetting password with token: {}", token.take(8) + "...")
        
        val user = userRepository.findByValidPasswordResetToken(token)
            ?: throw InvalidTokenException("Invalid or expired password reset token")
        
        // Validate new password strength
        PasswordUtil.validatePasswordStrength(newPassword)
        
        val newPasswordHash = passwordEncoder.encode(newPassword)
        
        val updatedUser = user.copy(
            passwordHash = newPasswordHash,
            passwordResetToken = null,
            passwordResetExpiresAt = null,
            failedLoginAttempts = 0,
            accountLockedUntil = null,
            updatedAt = LocalDateTime.now()
        )
        
        val savedUser = userRepository.save(updatedUser)
        
        emailService.sendPasswordResetConfirmation(user.email)
        
        // Audit log
        auditService.logPasswordReset(user.id, user.email)
        
        logger.info("Password reset successfully for user: {}", user.email)
        return savedUser
    }
    
    // ==========================================================================
    // EMAIL VERIFICATION
    // ==========================================================================
    
    fun verifyEmail(token: String): User {
        logger.info("Verifying email with token: {}", token.take(8) + "...")
        
        val user = userRepository.findByEmailVerificationToken(token)
            ?: throw InvalidTokenException("Invalid email verification token")
        
        val updatedUser = user.withEmailVerified()
        val savedUser = userRepository.save(updatedUser)
        
        emailService.sendEmailVerificationConfirmation(user.email)
        
        // Audit log
        auditService.logEmailVerification(user.id, user.email)
        
        logger.info("Email verified successfully for user: {}", user.email)
        return savedUser
    }
    
    fun resendEmailVerification(email: String): Boolean {
        val user = findByEmail(email) ?: return false
        
        if (user.isEmailVerified) {
            throw IllegalStateException("Email is already verified")
        }
        
        logger.info("Resending email verification for user: {}", email)
        
        val newToken = generateSecureToken()
        val updatedUser = user.copy(
            emailVerificationToken = newToken,
            updatedAt = LocalDateTime.now()
        )
        
        userRepository.save(updatedUser)
        emailService.sendEmailVerification(user.email, newToken)
        
        return true
    }
    
    // ==========================================================================
    // USER STATUS MANAGEMENT
    // ==========================================================================
    
    fun activateUser(userId: UUID): User {
        val user = findByIdOrThrow(userId)
        
        logger.info("Activating user: {} ({})", user.email, userId)
        
        val updatedUser = user.withStatus(UserStatus.ACTIVE)
        val savedUser = userRepository.save(updatedUser)
        
        emailService.sendAccountActivatedNotification(user.email)
        
        // Audit log
        auditService.logUserStatusChange(userId, UserStatus.ACTIVE)
        
        return savedUser
    }
    
    fun deactivateUser(userId: UUID, reason: String?): User {
        val user = findByIdOrThrow(userId)
        
        if (user.role == UserRole.SUPER_ADMIN) {
            throw IllegalOperationException("Cannot deactivate super admin user")
        }
        
        logger.info("Deactivating user: {} ({})", user.email, userId)
        
        val updatedUser = user.withStatus(UserStatus.INACTIVE)
        val savedUser = userRepository.save(updatedUser)
        
        emailService.sendAccountDeactivatedNotification(user.email, reason)
        
        // Audit log
        auditService.logUserStatusChange(userId, UserStatus.INACTIVE, reason)
        
        return savedUser
    }
    
    fun deleteUser(userId: UUID): User {
        val user = findByIdOrThrow(userId)
        
        if (!user.isDeletable) {
            throw IllegalOperationException("User cannot be deleted")
        }
        
        logger.info("Soft deleting user: {} ({})", user.email, userId)
        
        val deletedBy = securityContext.getCurrentUserId() ?: "system"
        val deletedUser = user.softDelete(deletedBy)
        val savedUser = userRepository.save(deletedUser)
        
        // Audit log
        auditService.logUserDeletion(userId, user.email)
        
        return savedUser
    }
    
    // ==========================================================================
    // UTILITY METHODS
    // ==========================================================================
    
    private fun generateSecureToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..64)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    @Transactional(readOnly = true)
    fun getUserStatistics(): UserStatistics {
        val tenantId = securityContext.getCurrentTenantId()
            ?: throw IllegalStateException("No tenant context available")
        
        val totalUsers = userRepository.countByTenantIdAndStatus(tenantId, UserStatus.ACTIVE)
        val newUsersLast30Days = userRepository.countNewUsersSince(
            tenantId, 
            LocalDateTime.now().minusDays(30)
        )
        val activeUsersLast30Days = userRepository.countActiveUsersSince(
            tenantId, 
            LocalDateTime.now().minusDays(30)
        )
        
        return UserStatistics(
            totalActiveUsers = totalUsers,
            newUsersLast30Days = newUsersLast30Days,
            activeUsersLast30Days = activeUsersLast30Days
        )
    }
    
    fun cleanupExpiredTokens() {
        logger.info("Cleaning up expired tokens")
        
        val clearedTokens = userRepository.clearExpiredPasswordResetTokens()
        val unlockedAccounts = userRepository.unlockExpiredAccounts()
        
        logger.info("Cleanup completed: {} expired tokens cleared, {} accounts unlocked", 
                   clearedTokens, unlockedAccounts)
    }
}