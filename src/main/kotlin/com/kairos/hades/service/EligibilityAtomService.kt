// =============================================================================
// File: src/main/kotlin/com/kairos/hades/service/EligibilityAtomService.kt
// 🔥 HADES EligibilityAtom Service
// Author: Sankhadeep Banerjee
// Business logic layer for EligibilityAtom management and execution
// =============================================================================

package com.kairos.hades.service

import com.kairos.hades.atoms.EligibilityAtom
import com.kairos.hades.enums.*
import com.kairos.hades.exception.*
import com.kairos.hades.multitenancy.TenantContext
import com.kairos.hades.repository.EligibilityAtomRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Service class for managing EligibilityAtoms
 * Handles CRUD operations, validation, and business logic
 */
@Service
@Transactional
class EligibilityAtomService @Autowired constructor(
    private val atomRepository: EligibilityAtomRepository,
    private val tenantContext: TenantContext,
    private val atomExecutionService: AtomExecutionService,
    private val atomValidationService: AtomValidationService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(EligibilityAtomService::class.java)
    }
    
    // ==========================================================================
    // CRUD OPERATIONS
    // ==========================================================================
    
    /**
     * Create a new eligibility atom
     */
    fun createAtom(request: CreateAtomRequest): EligibilityAtom {
        val tenantId = tenantContext.getTenantId()
        logger.info("Creating new atom '{}' for tenant '{}'", request.code, tenantId)
        
        // Validate request
        validateCreateRequest(request)
        
        // Check if code already exists
        if (atomRepository.existsByTenantIdAndCodeAndDeletedFalse(tenantId, request.code)) {
            throw AtomAlreadyExistsException("Atom with code '${request.code}' already exists")
        }
        
        // Create atom entity
        val atom = EligibilityAtom().apply {
            this.tenantId = tenantId
            code = request.code
            name = request.name
            description = request.description
            category = request.category
            type = request.type
            status = AtomStatus.DRAFT
            logicDefinition = request.logicDefinition
            inputParameters = request.inputParameters
            outputSchema = request.outputSchema
            validationRules = request.validationRules
            priority = request.priority ?: 5
            cacheEnabled = request.cacheEnabled ?: true
            cacheTtlSeconds = request.cacheTtlSeconds ?: 3600L
            expectedExecutionTimeMs = request.expectedExecutionTimeMs ?: 100L
            documentation = request.documentation
            example = request.example
            testCases = request.testCases
            typeConfig = request.typeConfig
            
            // Add tags
            request.tags?.let { tags.addAll(it) }
            
            // Add dependencies
            request.dependencies?.let { dependencies.addAll(it) }
        }
        
        // Validate atom configuration
        val validationErrors = atomValidationService.validateAtom(atom)
        if (validationErrors.isNotEmpty()) {
            throw AtomValidationException("Atom validation failed: ${validationErrors.joinToString(", ")}")
        }
        
        // Save atom
        val savedAtom = atomRepository.save(atom)
        logger.info("Created atom '{}' with ID '{}'", savedAtom.code, savedAtom.id)
        
        return savedAtom
    }
    
    /**
     * Update an existing eligibility atom
     */
    fun updateAtom(atomId: UUID, request: UpdateAtomRequest): EligibilityAtom {
        val tenantId = tenantContext.getTenantId()
        logger.info("Updating atom '{}' for tenant '{}'", atomId, tenantId)
        
        // Find existing atom
        val atom = getAtomById(atomId)
        
        // Check if atom can be updated
        if (atom.status == AtomStatus.ARCHIVED) {
            throw AtomUpdateNotAllowedException("Cannot update archived atom")
        }
        
        // Update fields
        request.name?.let { atom.name = it }
        request.description?.let { atom.description = it }
        request.category?.let { atom.category = it }
        request.logicDefinition?.let { atom.logicDefinition = it }
        request.inputParameters?.let { atom.inputParameters = it }
        request.outputSchema?.let { atom.outputSchema = it }
        request.validationRules?.let { atom.validationRules = it }
        request.priority?.let { atom.priority = it }
        request.cacheEnabled?.let { atom.cacheEnabled = it }
        request.cacheTtlSeconds?.let { atom.cacheTtlSeconds = it }
        request.expectedExecutionTimeMs?.let { atom.expectedExecutionTimeMs = it }
        request.documentation?.let { atom.documentation = it }
        request.example?.let { atom.example = it }
        request.testCases?.let { atom.testCases = it }
        request.typeConfig?.let { atom.typeConfig = it }
        
        // Update tags
        request.tags?.let { 
            atom.tags.clear()
            atom.tags.addAll(it)
        }
        
        // Update dependencies
        request.dependencies?.let {
            atom.dependencies.clear()
            atom.dependencies.addAll(it)
        }
        
        // Validate updated atom
        val validationErrors = atomValidationService.validateAtom(atom)
        if (validationErrors.isNotEmpty()) {
            throw AtomValidationException("Atom validation failed: ${validationErrors.joinToString(", ")}")
        }
        
        // Save updated atom
        val savedAtom = atomRepository.save(atom)
        logger.info("Updated atom '{}'", savedAtom.code)
        
        return savedAtom
    }
    
    /**
     * Get atom by ID
     */
    @Transactional(readOnly = true)
    fun getAtomById(atomId: UUID): EligibilityAtom {
        val tenantId = tenantContext.getTenantId()
        return atomRepository.findById(atomId)
            .filter { it.tenantId == tenantId && !it.deleted }
            .orElseThrow { AtomNotFoundException("Atom not found: $atomId") }
    }
    
    /**
     * Get atom by code
     */
    @Transactional(readOnly = true)
    fun getAtomByCode(code: String): EligibilityAtom {
        val tenantId = tenantContext.getTenantId()
        return atomRepository.findByTenantIdAndCode(tenantId, code)
            ?: throw AtomNotFoundException("Atom not found: $code")
    }
    
    /**
     * Get latest version of atom by code
     */
    @Transactional(readOnly = true)
    fun getLatestAtomVersion(code: String): EligibilityAtom {
        val tenantId = tenantContext.getTenantId()
        return atomRepository.findLatestVersion(tenantId, code)
            ?: throw AtomNotFoundException("Atom not found: $code")
    }
    
    /**
     * Get all atoms with pagination
     */
    @Transactional(readOnly = true)
    fun getAllAtoms(pageable: Pageable): Page<EligibilityAtom> {
        val tenantId = tenantContext.getTenantId()
        return atomRepository.findByTenantIdAndDeletedFalse(tenantId, pageable)
    }
    
    /**
     * Search atoms with filters
     */
    @Transactional(readOnly = true)
    fun searchAtoms(searchRequest: AtomSearchRequest, pageable: Pageable): Page<EligibilityAtom> {
        val tenantId = tenantContext.getTenantId()
        
        return atomRepository.findWithFilters(
            tenantId = tenantId,
            category = searchRequest.category,
            type = searchRequest.type,
            status = searchRequest.status,
            searchTerm = searchRequest.searchTerm,
            pageable = pageable
        )
    }
    
    /**
     * Delete atom (soft delete)
     */
    fun deleteAtom(atomId: UUID) {
        val tenantId = tenantContext.getTenantId()
        val atom = getAtomById(atomId)
        
        // Check if atom has dependencies
        if (atomRepository.hasActiveDependents(tenantId, atom.code)) {
            throw AtomDeletionNotAllowedException("Cannot delete atom that has active dependents")
        }
        
        // Soft delete
        atom.markAsDeleted(tenantContext.getUserId())
        atomRepository.save(atom)
        
        logger.info("Deleted atom '{}' ({})", atom.code, atomId)
    }
    
    // ==========================================================================
    // ATOM LIFECYCLE MANAGEMENT
    // ==========================================================================
    
    /**
     * Activate an atom (move to ACTIVE status)
     */
    fun activateAtom(atomId: UUID): EligibilityAtom {
        val atom = getAtomById(atomId)
        
        // Validate atom can be activated
        if (atom.status == AtomStatus.ARCHIVED) {
            throw AtomLifecycleException("Cannot activate archived atom")
        }
        
        // Validate atom configuration before activation
        val validationErrors = atomValidationService.validateAtomForActivation(atom)
        if (validationErrors.isNotEmpty()) {
            throw AtomValidationException("Cannot activate atom: ${validationErrors.joinToString(", ")}")
        }
        
        // Activate atom
        atom.activate()
        val savedAtom = atomRepository.save(atom)
        
        logger.info("Activated atom '{}'", atom.code)
        return savedAtom
    }
    
    /**
     * Deprecate an atom
     */
    fun deprecateAtom(atomId: UUID): EligibilityAtom {
        val atom = getAtomById(atomId)
        
        atom.deprecate()
        val savedAtom = atomRepository.save(atom)
        
        logger.info("Deprecated atom '{}'", atom.code)
        return savedAtom
    }
    
    /**
     * Move atom to testing status
     */
    fun moveAtomToTesting(atomId: UUID): EligibilityAtom {
        val atom = getAtomById(atomId)
        
        // Validate atom configuration before testing
        val validationErrors = atomValidationService.validateAtom(atom)
        if (validationErrors.isNotEmpty()) {
            throw AtomValidationException("Cannot move to testing: ${validationErrors.joinToString(", ")}")
        }
        
        atom.moveToTesting()
        val savedAtom = atomRepository.save(atom)
        
        logger.info("Moved atom '{}' to testing", atom.code)
        return savedAtom
    }
    
    /**
     * Create new version of an atom
     */
    fun createAtomVersion(atomId: UUID, request: CreateVersionRequest): EligibilityAtom {
        val originalAtom = getAtomById(atomId)
        val tenantId = tenantContext.getTenantId()
        
        // Get next version number
        val nextVersion = atomRepository.getNextVersionNumber(tenantId, originalAtom.code)
        
        // Create new version
        val newAtom = EligibilityAtom().apply {
            this.tenantId = tenantId
            code = originalAtom.code
            name = request.name ?: originalAtom.name
            description = request.description ?: originalAtom.description
            category = originalAtom.category
            type = originalAtom.type
            status = AtomStatus.DRAFT
            version = nextVersion
            logicDefinition = request.logicDefinition ?: originalAtom.logicDefinition
            inputParameters = request.inputParameters ?: originalAtom.inputParameters
            outputSchema = request.outputSchema ?: originalAtom.outputSchema
            validationRules = request.validationRules ?: originalAtom.validationRules
            priority = request.priority ?: originalAtom.priority
            cacheEnabled = request.cacheEnabled ?: originalAtom.cacheEnabled
            cacheTtlSeconds = request.cacheTtlSeconds ?: originalAtom.cacheTtlSeconds
            expectedExecutionTimeMs = request.expectedExecutionTimeMs ?: originalAtom.expectedExecutionTimeMs
            documentation = request.documentation ?: originalAtom.documentation
            example = request.example ?: originalAtom.example
            testCases = request.testCases ?: originalAtom.testCases
            typeConfig = request.typeConfig ?: originalAtom.typeConfig
            
            // Copy tags and dependencies
            tags.addAll(originalAtom.tags)
            dependencies.addAll(originalAtom.dependencies)
        }
        
        // Validate new version
        val validationErrors = atomValidationService.validateAtom(newAtom)
        if (validationErrors.isNotEmpty()) {
            throw AtomValidationException("Version validation failed: ${validationErrors.joinToString(", ")}")
        }
        
        val savedAtom = atomRepository.save(newAtom)
        logger.info("Created version {} of atom '{}'", nextVersion, originalAtom.code)
        
        return savedAtom
    }
    
    // ==========================================================================
    // ATOM EXECUTION
    // ==========================================================================
    
    /**
     * Execute an atom with given input
     */
    fun executeAtom(atomId: UUID, input: Map<String, Any>): AtomExecutionResult {
        val atom = getAtomById(atomId)
        
        if (!atom.isActive()) {
            throw AtomExecutionException("Cannot execute non-active atom: ${atom.status}")
        }
        
        return atomExecutionService.executeAtom(atom, input)
    }
    
    /**
     * Execute atom by code
     */
    fun executeAtomByCode(code: String, input: Map<String, Any>): AtomExecutionResult {
        val atom = getLatestAtomVersion(code)
        return executeAtom(atom.id!!, input)
    }
    
    /**
     * Test atom execution with test cases
     */
    fun testAtom(atomId: UUID): AtomTestResult {
        val atom = getAtomById(atomId)
        return atomExecutionService.testAtom(atom)
    }
    
    // ==========================================================================
    // STATISTICS AND ANALYTICS
    // ==========================================================================
    
    /**
     * Get atom execution statistics
     */
    @Transactional(readOnly = true)
    fun getAtomStatistics(atomId: UUID): AtomStatistics {
        val atom = getAtomById(atomId)
        
        return AtomStatistics(
            atomId = atom.id!!,
            code = atom.code,
            executionCount = atom.executionCount,
            avgExecutionTimeMs = atom.avgExecutionTimeMs,
            successRate = atom.successRate,
            errorRate = atom.errorRate,
            lastExecutedAt = atom.lastExecutedAt
        )
    }
    
    /**
     * Get tenant atom statistics summary
     */
    @Transactional(readOnly = true)
    fun getTenantAtomStatistics(): TenantAtomStatistics {
        val tenantId = tenantContext.getTenantId()
        
        val totalAtoms = atomRepository.countByTenantIdAndDeletedFalse(tenantId)
        val activeAtoms = atomRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, AtomStatus.ACTIVE)
        val draftAtoms = atomRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, AtomStatus.DRAFT)
        val testingAtoms = atomRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, AtomStatus.TESTING)
        
        return TenantAtomStatistics(
            totalAtoms = totalAtoms,
            activeAtoms = activeAtoms,
            draftAtoms = draftAtoms,
            testingAtoms = testingAtoms,
            categoryCounts = getAtomCountsByCategory()
        )
    }
    
    /**
     * Get atom counts by category
     */
    @Transactional(readOnly = true)
    fun getAtomCountsByCategory(): Map<AtomCategory, Long> {
        val tenantId = tenantContext.getTenantId()
        return AtomCategory.values().associateWith { category ->
            atomRepository.countByTenantIdAndCategoryAndDeletedFalse(tenantId, category)
        }
    }
    
    // ==========================================================================
    // VALIDATION HELPERS
    // ==========================================================================
    
    private fun validateCreateRequest(request: CreateAtomRequest) {
        if (request.code.isBlank()) throw InvalidRequestException("Code is required")
        if (request.name.isBlank()) throw InvalidRequestException("Name is required")
        if (request.logicDefinition.isBlank()) throw InvalidRequestException("Logic definition is required")
        
        // Validate code format
        if (!request.code.matches(Regex("^[A-Z][A-Z0-9_]*$"))) {
            throw InvalidRequestException("Code must start with uppercase letter and contain only uppercase letters, numbers, and underscores")
        }
    }
}

// ==========================================================================
// DATA CLASSES FOR REQUESTS AND RESPONSES
// ==========================================================================

data class CreateAtomRequest(
    val code: String,
    val name: String,
    val description: String? = null,
    val category: AtomCategory,
    val type: AtomType,
    val logicDefinition: String,
    val inputParameters: String? = null,
    val outputSchema: String? = null,
    val validationRules: String? = null,
    val priority: Int? = null,
    val cacheEnabled: Boolean? = null,
    val cacheTtlSeconds: Long? = null,
    val expectedExecutionTimeMs: Long? = null,
    val tags: Set<String>? = null,
    val dependencies: Set<String>? = null,
    val documentation: String? = null,
    val example: String? = null,
    val testCases: String? = null,
    val typeConfig: String? = null
)

data class UpdateAtomRequest(
    val name: String? = null,
    val description: String? = null,
    val category: AtomCategory? = null,
    val logicDefinition: String? = null,
    val inputParameters: String? = null,
    val outputSchema: String? = null,
    val validationRules: String? = null,
    val priority: Int? = null,
    val cacheEnabled: Boolean? = null,
    val cacheTtlSeconds: Long? = null,
    val expectedExecutionTimeMs: Long? = null,
    val tags: Set<String>? = null,
    val dependencies: Set<String>? = null,
    val documentation: String? = null,
    val example: String? = null,
    val testCases: String? = null,
    val typeConfig: String? = null
)

data class CreateVersionRequest(
    val name: String? = null,
    val description: String? = null,
    val logicDefinition: String? = null,
    val inputParameters: String? = null,
    val outputSchema: String? = null,
    val validationRules: String? = null,
    val priority: Int? = null,
    val cacheEnabled: Boolean? = null,
    val cacheTtlSeconds: Long? = null,
    val expectedExecutionTimeMs: Long? = null,
    val documentation: String? = null,
    val example: String? = null,
    val testCases: String? = null,
    val typeConfig: String? = null
)

data class AtomSearchRequest(
    val searchTerm: String? = null,
    val category: AtomCategory? = null,
    val type: AtomType? = null,
    val status: AtomStatus? = null,
    val tags: List<String>? = null
)

data class AtomExecutionResult(
    val atomId: UUID,
    val success: Boolean,
    val result: Any? = null,
    val error: String? = null,
    val executionTimeMs: Long,
    val cacheHit: Boolean = false
)

data class AtomTestResult(
    val atomId: UUID,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val testResults: List<TestCaseResult>
)

data class TestCaseResult(
    val testName: String,
    val success: Boolean,
    val expected: Any?,
    val actual: Any?,
    val error: String? = null
)

data class AtomStatistics(
    val atomId: UUID,
    val code: String,
    val executionCount: Long,
    val avgExecutionTimeMs: Long,
    val successRate: Double,
    val errorRate: Double,
    val lastExecutedAt: LocalDateTime?
)

data class TenantAtomStatistics(
    val totalAtoms: Long,
    val activeAtoms: Long,
    val draftAtoms: Long,
    val testingAtoms: Long,
    val categoryCounts: Map<AtomCategory, Long>
)