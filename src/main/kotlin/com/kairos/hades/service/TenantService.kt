// =============================================================================
// File: src/main/kotlin/com/kairos/hades/service/TenantService.kt
// 🔥 HADES Tenant Service
// Author: Sankhadeep Banerjee
// Business logic layer for tenant management and subscription handling
// =============================================================================

package com.kairos.hades.service

import com.kairos.hades.entity.Tenant
import com.kairos.hades.enums.SubscriptionTier
import com.kairos.hades.enums.TenantStatus
import com.kairos.hades.exception.*
import com.kairos.hades.repository.TenantRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Service class for managing tenants
 * Handles CRUD operations, subscription management, and business logic
 */
@Service
@Transactional
class TenantService @Autowired constructor(
    private val tenantRepository: TenantRepository,
    private val passwordEncoder: PasswordEncoder
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantService::class.java)
    }
    
    // ==========================================================================
    // CRUD OPERATIONS
    // ==========================================================================
    
    /**
     * Create a new tenant
     */
    fun createTenant(request: CreateTenantRequest): Tenant {
        logger.info("Creating new tenant with slug: {}", request.slug)
        
        // Validate request
        validateCreateRequest(request)
        
        // Check if slug already exists
        if (tenantRepository.existsBySlugAndDeletedFalse(request.slug)) {
            throw TenantAlreadyExistsException("Tenant with slug '${request.slug}' already exists")
        }
        
        // Check if email already exists
        if (tenantRepository.existsByContactEmailAndDeletedFalse(request.contactEmail)) {
            throw TenantAlreadyExistsException("Tenant with email '${request.contactEmail}' already exists")
        }
        
        // Create tenant entity
        val tenant = Tenant().apply {
            // Set tenant ID to its own UUID for self-reference
            tenantId = UUID.randomUUID().toString()
            name = request.name
            slug = request.slug
            description = request.description
            contactEmail = request.contactEmail
            contactPhone = request.contactPhone
            websiteUrl = request.websiteUrl
            
            // Set subscription details
            subscriptionTier = request.subscriptionTier
            maxStorageBytes = request.subscriptionTier.getMaxStorageBytes()
            maxApiRequestsPerMonth = request.subscriptionTier.maxApiRequestsPerMonth
            maxUsers = request.subscriptionTier.maxUsers
            
            // Set initial status
            status = if (request.subscriptionTier == SubscriptionTier.FREE) {
                TenantStatus.ACTIVE
            } else {
                TenantStatus.TRIAL
            }
            
            // Set trial period if applicable
            if (status == TenantStatus.TRIAL) {
                trialStartDate = LocalDateTime.now()
                trialEndDate = LocalDateTime.now().plusDays(14) // 14-day trial
            }
            
            // Set subscription dates
            subscriptionStartDate = LocalDateTime.now()
            subscriptionEndDate = LocalDateTime.now().plusYears(1) // 1-year initial subscription
        }
        
        // Save tenant
        val savedTenant = tenantRepository.save(tenant)
        logger.info("Created tenant '{}' with ID '{}'", savedTenant.slug, savedTenant.id)
        
        return savedTenant
    }
    
    /**
     * Get tenant by ID
     */
    @Transactional(readOnly = true)
    fun getTenantById(tenantId: UUID): Tenant {
        return tenantRepository.findById(tenantId)
            .filter { !it.deleted }
            .orElseThrow { TenantNotFoundException("Tenant not found: $tenantId") }
    }
    
    /**
     * Get tenant by slug
     */
    @Transactional(readOnly = true)
    fun getTenantBySlug(slug: String): Tenant {
        return tenantRepository.findBySlugAndDeletedFalse(slug)
            ?: throw TenantNotFoundException("Tenant not found: $slug")
    }
    
    /**
     * Update tenant
     */
    fun updateTenant(tenantId: UUID, request: UpdateTenantRequest): Tenant {
        logger.info("Updating tenant: {}", tenantId)
        
        val tenant = getTenantById(tenantId)
        
        // Update fields
        request.name?.let { tenant.name = it }
        request.description?.let { tenant.description = it }
        request.contactPhone?.let { tenant.contactPhone = it }
        request.websiteUrl?.let { tenant.websiteUrl = it }
        request.logoUrl?.let { tenant.logoUrl = it }
        
        // Handle email change
        request.contactEmail?.let { newEmail ->
            if (newEmail != tenant.contactEmail) {
                if (tenantRepository.existsByContactEmailAndNotId(newEmail, tenantId)) {
                    throw TenantAlreadyExistsException("Email '$newEmail' is already in use")
                }
                tenant.contactEmail = newEmail
            }
        }
        
        val savedTenant = tenantRepository.save(tenant)
        logger.info("Updated tenant '{}'", savedTenant.slug)
        
        return savedTenant
    }
    
    /**
     * Search tenants with filters
     */
    @Transactional(readOnly = true)
    fun searchTenants(searchRequest: TenantSearchRequest, pageable: Pageable): Page<Tenant> {
        return tenantRepository.findWithFilters(
            status = searchRequest.status,
            tier = searchRequest.subscriptionTier,
            searchTerm = searchRequest.searchTerm,
            pageable = pageable
        )
    }
    
    // ==========================================================================
    // SUBSCRIPTION MANAGEMENT
    // ==========================================================================
    
    /**
     * Update tenant subscription tier
     */
    fun updateSubscription(
        tenantId: UUID, 
        newTier: SubscriptionTier, 
        subscriptionEndDate: LocalDateTime? = null
    ): Tenant {
        logger.info("Updating subscription for tenant {} to tier {}", tenantId, newTier)
        
        val tenant = getTenantById(tenantId)
        val oldTier = tenant.subscriptionTier
        
        // Update subscription details
        tenant.subscriptionTier = newTier
        tenant.maxStorageBytes = newTier.getMaxStorageBytes()
        tenant.maxApiRequestsPerMonth = newTier.maxApiRequestsPerMonth
        tenant.maxUsers = newTier.maxUsers
        
        // Update subscription dates
        tenant.subscriptionStartDate = LocalDateTime.now()
        tenant.subscriptionEndDate = subscriptionEndDate 
            ?: LocalDateTime.now().plusYears(1)
        
        // Update status based on tier
        if (newTier == SubscriptionTier.FREE) {
            tenant.status = TenantStatus.ACTIVE
            tenant.trialStartDate = null
            tenant.trialEndDate = null
        } else if (oldTier == SubscriptionTier.FREE) {
            // Moving from free to paid - start trial if not already active
            if (tenant.status != TenantStatus.ACTIVE) {
                tenant.status = TenantStatus.TRIAL
                tenant.trialStartDate = LocalDateTime.now()
                tenant.trialEndDate = LocalDateTime.now().plusDays(14)
            }
        }
        
        val savedTenant = tenantRepository.save(tenant)
        logger.info("Updated subscription for tenant '{}' from {} to {}", 
            savedTenant.slug, oldTier, newTier)
        
        return savedTenant
    }
    
    /**
     * Activate tenant (end trial, start paid subscription)
     */
    fun activateTenant(tenantId: UUID): Tenant {
        logger.info("Activating tenant: {}", tenantId)
        
        val tenant = getTenantById(tenantId)
        
        if (tenant.status == TenantStatus.ACTIVE) {
            logger.info("Tenant '{}' is already active", tenant.slug)
            return tenant
        }
        
        tenant.activate()
        tenant.trialEndDate = LocalDateTime.now() // End trial
        
        val savedTenant = tenantRepository.save(tenant)
        logger.info("Activated tenant '{}'", savedTenant.slug)
        
        return savedTenant
    }
    
    /**
     * Suspend tenant
     */
    fun suspendTenant(tenantId: UUID, reason: String? = null): Tenant {
        logger.info("Suspending tenant: {} - Reason: {}", tenantId, reason)
        
        val tenant = getTenantById(tenantId)
        tenant.suspend()
        
        val savedTenant = tenantRepository.save(tenant)
        logger.info("Suspended tenant '{}'", savedTenant.slug)
        
        return savedTenant
    }
    
    /**
     * Complete tenant onboarding
     */
    fun completeOnboarding(tenantId: UUID): Tenant {
        logger.info("Completing onboarding for tenant: {}", tenantId)
        
        val tenant = getTenantById(tenantId)
        
        if (tenant.onboardingCompleted) {
            logger.info("Onboarding already completed for tenant '{}'", tenant.slug)
            return tenant
        }
        
        tenant.completeOnboarding()
        
        val savedTenant = tenantRepository.save(tenant)
        logger.info("Completed onboarding for tenant '{}'", savedTenant.slug)
        
        return savedTenant
    }
    
    // ==========================================================================
    // USAGE TRACKING
    // ==========================================================================
    
    /**
     * Update API usage for tenant
     */
    fun updateApiUsage(tenantId: UUID, requestCount: Long = 1L): Tenant {
        val tenant = getTenantById(tenantId)
        
        // Check if tenant has exceeded API limit
        if (tenant.currentApiRequestsMonth + requestCount > tenant.maxApiRequestsPerMonth) {
            throw TenantLimitExceededException(
                "API request limit exceeded",
                "api_requests",
                tenant.currentApiRequestsMonth + requestCount,
                tenant.maxApiRequestsPerMonth
            )
        }
        
        tenant.incrementApiUsage(requestCount)
        tenant.lastActivityAt = LocalDateTime.now()
        
        return tenantRepository.save(tenant)
    }
    
    /**
     * Update storage usage for tenant
     */
    fun updateStorageUsage(tenantId: UUID, bytesChange: Long): Tenant {
        val tenant = getTenantById(tenantId)
        
        val newStorageUsage = tenant.currentStorageBytes + bytesChange
        
        // Check if tenant has exceeded storage limit
        if (newStorageUsage > tenant.maxStorageBytes) {
            throw TenantLimitExceededException(
                "Storage limit exceeded",
                "storage",
                newStorageUsage,
                tenant.maxStorageBytes
            )
        }
        
        tenant.updateStorageUsage(bytesChange)
        
        return tenantRepository.save(tenant)
    }
    
    /**
     * Reset monthly API usage for all tenants (scheduled job)
     */
    fun resetMonthlyApiUsage() {
        logger.info("Resetting monthly API usage for all tenants")
        
        tenantRepository.resetMonthlyApiUsageForAllTenants()
        
        logger.info("Reset monthly API usage completed")
    }
    
    // ==========================================================================
    // STATISTICS AND ANALYTICS
    // ==========================================================================
    
    /**
     * Get tenant statistics
     */
    @Transactional(readOnly = true)
    fun getTenantStatistics(tenantId: UUID): TenantStatistics {
        val tenant = getTenantById(tenantId)
        
        return TenantStatistics(
            tenantId = tenant.id!!,
            name = tenant.name,
            slug = tenant.slug,
            status = tenant.status,
            subscriptionTier = tenant.subscriptionTier,
            apiUsage = TenantUsageStats(
                current = tenant.currentApiRequestsMonth,
                limit = tenant.maxApiRequestsPerMonth,
                percentage = tenant.getApiUsagePercentage()
            ),
            storageUsage = TenantUsageStats(
                current = tenant.currentStorageBytes,
                limit = tenant.maxStorageBytes,
                percentage = tenant.getStorageUsagePercentage()
            ),
            userCount = 0, // TODO: Implement user counting
            trialInfo = if (tenant.isInTrial()) {
                TrialInfo(
                    startDate = tenant.trialStartDate!!,
                    endDate = tenant.trialEndDate!!,
                    daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDateTime.now(), 
                        tenant.trialEndDate
                    ).toInt()
                )
            } else null,
            subscriptionInfo = SubscriptionInfo(
                tier = tenant.subscriptionTier,
                startDate = tenant.subscriptionStartDate!!,
                endDate = tenant.subscriptionEndDate!!,
                isActive = tenant.isActive(),
                daysUntilRenewal = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDateTime.now(), 
                    tenant.subscriptionEndDate
                ).toInt()
            ),
            onboardingCompleted = tenant.onboardingCompleted,
            lastActivityAt = tenant.lastActivityAt
        )
    }
    
    /**
     * Get platform-wide tenant statistics
     */
    @Transactional(readOnly = true)
    fun getPlatformStatistics(): PlatformStatistics {
        val stats = tenantRepository.getTenantStatistics()
        val tierDistribution = tenantRepository.getSubscriptionTierDistribution()
        val monthlyGrowth = tenantRepository.getMonthlyGrowth(LocalDateTime.now().minusYears(1))
        
        return PlatformStatistics(
            totalTenants = stats["totalTenants"] as? Long ?: 0L,
            activeTenants = stats["activeTenants"] as? Long ?: 0L,
            trialTenants = stats["trialTenants"] as? Long ?: 0L,
            completedOnboarding = stats["completedOnboarding"] as? Long ?: 0L,
            avgStorageUsage = stats["avgStorageUsage"] as? Double ?: 0.0,
            avgApiUsage = stats["avgApiUsage"] as? Double ?: 0.0,
            tierDistribution = tierDistribution.associate { 
                (it["tier"] as SubscriptionTier) to (it["count"] as Long)
            },
            monthlyGrowth = monthlyGrowth.map { 
                MonthlyGrowthData(
                    year = (it["year"] as Number).toInt(),
                    month = (it["month"] as Number).toInt(),
                    newTenants = (it["newTenants"] as Number).toLong()
                )
            }
        )
    }
    
    // ==========================================================================
    // MAINTENANCE OPERATIONS
    // ==========================================================================
    
    /**
     * Find tenants with expiring trials
     */
    @Transactional(readOnly = true)
    fun findTenantsWithExpiringTrials(daysAhead: Long = 3): List<Tenant> {
        val startDate = LocalDateTime.now()
        val endDate = LocalDateTime.now().plusDays(daysAhead)
        
        return tenantRepository.findTenantsWithExpiringTrials(startDate, endDate)
    }
    
    /**
     * Find tenants with expired subscriptions
     */
    @Transactional(readOnly = true)
    fun findTenantsWithExpiredSubscriptions(): List<Tenant> {
        return tenantRepository.findTenantsWithExpiredSubscriptions(LocalDateTime.now())
    }
    
    /**
     * Find tenants approaching resource limits
     */
    @Transactional(readOnly = true)
    fun findTenantsApproachingLimits(threshold: Double = 0.8): TenantsApproachingLimits {
        val storageLimit = tenantRepository.findTenantsApproachingStorageLimit(threshold)
        val apiLimit = tenantRepository.findTenantsApproachingApiLimit(threshold)
        
        return TenantsApproachingLimits(
            storageLimit = storageLimit,
            apiLimit = apiLimit
        )
    }
    
    /**
     * Process expired subscriptions (scheduled job)
     */
    fun processExpiredSubscriptions() {
        logger.info("Processing expired subscriptions")
        
        val expiredTenants = findTenantsWithExpiredSubscriptions()
        
        expiredTenants.forEach { tenant ->
            try {
                // Move to payment required status
                tenant.status = TenantStatus.PAYMENT_REQUIRED
                tenantRepository.save(tenant)
                
                logger.info("Marked tenant '{}' as payment required", tenant.slug)
                
                // TODO: Send notification email
                
            } catch (e: Exception) {
                logger.error("Failed to process expired subscription for tenant '{}': {}", 
                    tenant.slug, e.message, e)
            }
        }
        
        logger.info("Processed {} expired subscriptions", expiredTenants.size)
    }
    
    /**
     * Process expiring trials (scheduled job)
     */
    fun processExpiringTrials() {
        logger.info("Processing expiring trials")
        
        val expiringTrials = findTenantsWithExpiringTrials()
        
        expiringTrials.forEach { tenant ->
            try {
                // TODO: Send trial expiration warning email
                logger.info("Trial expiring for tenant '{}' on {}", 
                    tenant.slug, tenant.trialEndDate)
                    
            } catch (e: Exception) {
                logger.error("Failed to process expiring trial for tenant '{}': {}", 
                    tenant.slug, e.message, e)
            }
        }
        
        logger.info("Processed {} expiring trials", expiringTrials.size)
    }
    
    // ==========================================================================
    // VALIDATION HELPERS
    // ==========================================================================
    
    private fun validateCreateRequest(request: CreateTenantRequest) {
        if (request.name.isBlank()) {
            throw InvalidRequestException("Tenant name is required")
        }
        
        if (request.slug.isBlank()) {
            throw InvalidRequestException("Tenant slug is required")
        }
        
        if (!request.slug.matches(Regex("^[a-z0-9-]+$"))) {
            throw InvalidRequestException("Slug must contain only lowercase letters, numbers, and hyphens")
        }
        
        if (request.slug.length < 3 || request.slug.length > 100) {
            throw InvalidRequestException("Slug must be between 3 and 100 characters")
        }
        
        if (request.contactEmail.isBlank()) {
            throw InvalidRequestException("Contact email is required")
        }
        
        if (!isValidEmail(request.contactEmail)) {
            throw InvalidRequestException("Invalid email format")
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
}

// ==========================================================================
// DATA CLASSES FOR STATISTICS AND RESPONSES
// ==========================================================================

data class TenantStatistics(
    val tenantId: UUID,
    val name: String,
    val slug: String,
    val status: TenantStatus,
    val subscriptionTier: SubscriptionTier,
    val apiUsage: TenantUsageStats,
    val storageUsage: TenantUsageStats,
    val userCount: Int,
    val trialInfo: TrialInfo?,
    val subscriptionInfo: SubscriptionInfo,
    val onboardingCompleted: Boolean,
    val lastActivityAt: LocalDateTime?
)

data class TenantUsageStats(
    val current: Long,
    val limit: Long,
    val percentage: Double
)

data class TrialInfo(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val daysRemaining: Int
)

data class SubscriptionInfo(
    val tier: SubscriptionTier,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val isActive: Boolean,
    val daysUntilRenewal: Int
)

data class PlatformStatistics(
    val totalTenants: Long,
    val activeTenants: Long,
    val trialTenants: Long,
    val completedOnboarding: Long,
    val avgStorageUsage: Double,
    val avgApiUsage: Double,
    val tierDistribution: Map<SubscriptionTier, Long>,
    val monthlyGrowth: List<MonthlyGrowthData>
)

data class MonthlyGrowthData(
    val year: Int,
    val month: Int,
    val newTenants: Long
)

data class TenantsApproachingLimits(
    val storageLimit: List<Tenant>,
    val apiLimit: List<Tenant>
)

// ==========================================================================
// MISSING SERVICE IMPLEMENTATIONS
// ==========================================================================

/**
 * Service for tenant configuration and settings management
 */
@Service
class TenantConfigurationService @Autowired constructor(
    private val tenantRepository: TenantRepository,
    private val tenantContext: TenantContext
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantConfigurationService::class.java)
    }
    
    /**
     * Update tenant configuration settings
     */
    fun updateTenantConfiguration(
        tenantId: UUID,
        configuration: TenantConfiguration
    ): Tenant {
        val tenant = tenantRepository.findById(tenantId)
            .filter { !it.deleted }
            .orElseThrow { TenantNotFoundException("Tenant not found: $tenantId") }
        
        // Update configuration fields
        configuration.logoUrl?.let { tenant.logoUrl = it }
        configuration.websiteUrl?.let { tenant.websiteUrl = it }
        configuration.contactPhone?.let { tenant.contactPhone = it }
        
        return tenantRepository.save(tenant)
    }
    
    /**
     * Get tenant feature flags based on subscription tier
     */
    fun getTenantFeatures(tenantId: UUID): Set<TenantFeature> {
        val tenant = tenantRepository.findById(tenantId)
            .filter { !it.deleted }
            .orElseThrow { TenantNotFoundException("Tenant not found: $tenantId") }
        
        return TenantFeature.values().filter { feature ->
            feature.isAvailableForTier(tenant.subscriptionTier)
        }.toSet()
    }
    
    /**
     * Check if tenant has specific feature
     */
    fun hasFeature(tenantId: UUID, feature: TenantFeature): Boolean {
        val tenant = tenantRepository.findById(tenantId)
            .filter { !it.deleted }
            .orElseThrow { TenantNotFoundException("Tenant not found: $tenantId") }
        
        return feature.isAvailableForTier(tenant.subscriptionTier)
    }
}

/**
 * Service for tenant billing and subscription management
 */
@Service
class TenantBillingService @Autowired constructor(
    private val tenantRepository: TenantRepository,
    private val tenantService: TenantService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantBillingService::class.java)
    }
    
    /**
     * Process billing for a tenant
     */
    fun processBilling(tenantId: UUID): BillingResult {
        val tenant = tenantRepository.findById(tenantId)
            .filter { !it.deleted }
            .orElseThrow { TenantNotFoundException("Tenant not found: $tenantId") }
        
        val usage = calculateUsage(tenant)
        val charges = calculateCharges(tenant, usage)
        
        return BillingResult(
            tenantId = tenantId,
            billingPeriod = getCurrentBillingPeriod(),
            baseCharge = tenant.subscriptionTier.monthlyPriceUSD,
            usageCharges = charges,
            totalAmount = tenant.subscriptionTier.monthlyPriceUSD + charges.values.sum(),
            usage = usage
        )
    }
    
    /**
     * Calculate usage for billing period
     */
    private fun calculateUsage(tenant: Tenant): UsageMetrics {
        return UsageMetrics(
            apiRequests = tenant.currentApiRequestsMonth,
            storageBytes = tenant.currentStorageBytes,
            users = 1, // TODO: Implement user counting
            atomExecutions = 0L // TODO: Implement atom execution counting
        )
    }
    
    /**
     * Calculate overage charges
     */
    private fun calculateCharges(tenant: Tenant, usage: UsageMetrics): Map<String, Double> {
        val charges = mutableMapOf<String, Double>()
        
        // API overage charges
        if (usage.apiRequests > tenant.maxApiRequestsPerMonth) {
            val overage = usage.apiRequests - tenant.maxApiRequestsPerMonth
            charges["api_overage"] = overage * 0.001 // $0.001 per extra request
        }
        
        // Storage overage charges
        if (usage.storageBytes > tenant.maxStorageBytes) {
            val overageGB = (usage.storageBytes - tenant.maxStorageBytes) / (1024 * 1024 * 1024)
            charges["storage_overage"] = overageGB * 0.10 // $0.10 per extra GB
        }
        
        return charges
    }
    
    private fun getCurrentBillingPeriod(): BillingPeriod {
        val now = LocalDateTime.now()
        return BillingPeriod(
            startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
            endDate = now.withDayOfMonth(1).plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59)
        )
    }
}

/**
 * Service for tenant lifecycle and onboarding
 */
@Service
class TenantOnboardingService @Autowired constructor(
    private val tenantRepository: TenantRepository,
    private val tenantService: TenantService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantOnboardingService::class.java)
    }
    
    /**
     * Get onboarding status for tenant
     */
    fun getOnboardingStatus(tenantId: UUID): OnboardingStatus {
        val tenant = tenantRepository.findById(tenantId)
            .filter { !it.deleted }
            .orElseThrow { TenantNotFoundException("Tenant not found: $tenantId") }
        
        if (tenant.onboardingCompleted) {
            return OnboardingStatus.COMPLETED
        }
        
        // TODO: Implement logic to determine current onboarding step
        return OnboardingStatus.PROFILE_SETUP
    }
    
    /**
     * Complete an onboarding step
     */
    fun completeOnboardingStep(
        tenantId: UUID, 
        step: OnboardingStatus
    ): OnboardingStepResult {
        val tenant = tenantRepository.findById(tenantId)
            .filter { !it.deleted }
            .orElseThrow { TenantNotFoundException("Tenant not found: $tenantId") }
        
        logger.info("Completing onboarding step {} for tenant {}", step, tenant.slug)
        
        // Mark step as completed
        // TODO: Implement step tracking in database
        
        // Check if onboarding is complete
        val nextStep = step.getNext()
        if (nextStep == null) {
            tenant.completeOnboarding()
            tenantRepository.save(tenant)
        }
        
        return OnboardingStepResult(
            completedStep = step,
            nextStep = nextStep,
            isComplete = nextStep == null,
            progress = calculateOnboardingProgress(step)
        )
    }
    
    private fun calculateOnboardingProgress(currentStep: OnboardingStatus): Double {
        val totalSteps = OnboardingStatus.values().size - 1 // Exclude NOT_STARTED
        val currentStepOrder = currentStep.order
        return (currentStepOrder.toDouble() / totalSteps.toDouble()) * 100.0
    }
}

// ==========================================================================
// ADDITIONAL DATA CLASSES
// ==========================================================================

data class TenantConfiguration(
    val logoUrl: String? = null,
    val websiteUrl: String? = null,
    val contactPhone: String? = null,
    val customDomain: String? = null,
    val branding: BrandingConfig? = null,
    val notifications: NotificationConfig? = null
)

data class BrandingConfig(
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val logoUrl: String? = null,
    val faviconUrl: String? = null,
    val customCss: String? = null
)

data class NotificationConfig(
    val emailNotifications: Boolean = true,
    val webhookUrl: String? = null,
    val slackWebhook: String? = null
)

data class BillingResult(
    val tenantId: UUID,
    val billingPeriod: BillingPeriod,
    val baseCharge: Double,
    val usageCharges: Map<String, Double>,
    val totalAmount: Double,
    val usage: UsageMetrics
)

data class BillingPeriod(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

data class UsageMetrics(
    val apiRequests: Long,
    val storageBytes: Long,
    val users: Int,
    val atomExecutions: Long
)

data class OnboardingStepResult(
    val completedStep: OnboardingStatus,
    val nextStep: OnboardingStatus?,
    val isComplete: Boolean,
    val progress: Double
)