// =============================================================================
// File: src/main/kotlin/com/kairos/hades/controller/EligibilityAtomController.kt
// 🔥 HADES EligibilityAtom REST Controller
// Author: Sankhadeep Banerjee
// REST API endpoints for EligibilityAtom management
// =============================================================================

package com.kairos.hades.controller

import com.kairos.hades.atoms.EligibilityAtom
import com.kairos.hades.enums.AtomCategory
import com.kairos.hades.enums.AtomStatus
import com.kairos.hades.enums.AtomType
import com.kairos.hades.service.*
import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST Controller for EligibilityAtom operations
 * Provides CRUD operations and atom management endpoints
 */
@RestController
@RequestMapping("/api/v1/atoms")
@Tag(name = "EligibilityAtoms", description = "EligibilityAtom management operations")
@CrossOrigin(origins = ["\${hades.security.cors.allowed-origins}"])
class EligibilityAtomController @Autowired constructor(
    private val atomService: EligibilityAtomService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(EligibilityAtomController::class.java)
    }
    
    // ==========================================================================
    // CRUD OPERATIONS
    // ==========================================================================
    
    @Operation(
        summary = "Create a new eligibility atom",
        description = "Creates a new EligibilityAtom in DRAFT status"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Atom created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "409", description = "Atom with code already exists")
        ]
    )
    @PostMapping
    fun createAtom(
        @Valid @RequestBody request: CreateAtomRequest
    ): ResponseEntity<ApiResponse<EligibilityAtom>> {
        logger.info("Creating new atom with code: {}", request.code)
        
        val atom = atomService.createAtom(request)
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(atom, "Atom created successfully"))
    }
    
    @Operation(
        summary = "Get atom by ID",
        description = "Retrieves an EligibilityAtom by its unique identifier"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Atom found"),
            ApiResponse(responseCode = "404", description = "Atom not found")
        ]
    )
    @GetMapping("/{atomId}")
    fun getAtomById(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID
    ): ResponseEntity<ApiResponse<EligibilityAtom>> {
        logger.debug("Getting atom by ID: {}", atomId)
        
        val atom = atomService.getAtomById(atomId)
        
        return ResponseEntity.ok(ApiResponse.success(atom))
    }
    
    @Operation(
        summary = "Get atom by code",
        description = "Retrieves the latest version of an EligibilityAtom by its code"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Atom found"),
            ApiResponse(responseCode = "404", description = "Atom not found")
        ]
    )
    @GetMapping("/code/{code}")
    fun getAtomByCode(
        @Parameter(description = "Atom code", required = true)
        @PathVariable code: String
    ): ResponseEntity<ApiResponse<EligibilityAtom>> {
        logger.debug("Getting atom by code: {}", code)
        
        val atom = atomService.getLatestAtomVersion(code)
        
        return ResponseEntity.ok(ApiResponse.success(atom))
    }
    
    @Operation(
        summary = "Get all atoms",
        description = "Retrieves all EligibilityAtoms with pagination and filtering"
    )
    @GetMapping
    fun getAllAtoms(
        @Parameter(description = "Search term for name/description")
        @RequestParam(required = false) search: String?,
        
        @Parameter(description = "Filter by category")
        @RequestParam(required = false) category: AtomCategory?,
        
        @Parameter(description = "Filter by type")
        @RequestParam(required = false) type: AtomType?,
        
        @Parameter(description = "Filter by status")
        @RequestParam(required = false) status: AtomStatus?,
        
        @Parameter(description = "Filter by tags (comma-separated)")
        @RequestParam(required = false) tags: String?,
        
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC)
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<EligibilityAtom>>> {
        logger.debug("Getting all atoms with filters - search: {}, category: {}, type: {}, status: {}", 
            search, category, type, status)
        
        val searchRequest = AtomSearchRequest(
            searchTerm = search,
            category = category,
            type = type,
            status = status,
            tags = tags?.split(",")?.map { it.trim() }
        )
        
        val atoms = atomService.searchAtoms(searchRequest, pageable)
        
        return ResponseEntity.ok(ApiResponse.success(atoms))
    }
    
    @Operation(
        summary = "Update an atom",
        description = "Updates an existing EligibilityAtom"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Atom updated successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "404", description = "Atom not found"),
            ApiResponse(responseCode = "409", description = "Update not allowed for current status")
        ]
    )
    @PutMapping("/{atomId}")
    fun updateAtom(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID,
        
        @Valid @RequestBody request: UpdateAtomRequest
    ): ResponseEntity<ApiResponse<EligibilityAtom>> {
        logger.info("Updating atom: {}", atomId)
        
        val atom = atomService.updateAtom(atomId, request)
        
        return ResponseEntity.ok(ApiResponse.success(atom, "Atom updated successfully"))
    }
    
    @Operation(
        summary = "Delete an atom",
        description = "Soft deletes an EligibilityAtom"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Atom deleted successfully"),
            ApiResponse(responseCode = "404", description = "Atom not found"),
            ApiResponse(responseCode = "409", description = "Cannot delete atom with dependencies")
        ]
    )
    @DeleteMapping("/{atomId}")
    fun deleteAtom(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID
    ): ResponseEntity<ApiResponse<Unit>> {
        logger.info("Deleting atom: {}", atomId)
        
        atomService.deleteAtom(atomId)
        
        return ResponseEntity.ok(ApiResponse.success(Unit, "Atom deleted successfully"))
    }
    
    // ==========================================================================
    // LIFECYCLE OPERATIONS
    // ==========================================================================
    
    @Operation(
        summary = "Activate an atom",
        description = "Moves an atom to ACTIVE status, making it available for execution"
    )
    @PostMapping("/{atomId}/activate")
    fun activateAtom(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID
    ): ResponseEntity<ApiResponse<EligibilityAtom>> {
        logger.info("Activating atom: {}", atomId)
        
        val atom = atomService.activateAtom(atomId)
        
        return ResponseEntity.ok(ApiResponse.success(atom, "Atom activated successfully"))
    }
    
    @Operation(
        summary = "Deprecate an atom",
        description = "Moves an atom to DEPRECATED status"
    )
    @PostMapping("/{atomId}/deprecate")
    fun deprecateAtom(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID
    ): ResponseEntity<ApiResponse<EligibilityAtom>> {
        logger.info("Deprecating atom: {}", atomId)
        
        val atom = atomService.deprecateAtom(atomId)
        
        return ResponseEntity.ok(ApiResponse.success(atom, "Atom deprecated successfully"))
    }
    
    @Operation(
        summary = "Move atom to testing",
        description = "Moves an atom to TESTING status for validation"
    )
    @PostMapping("/{atomId}/test")
    fun moveAtomToTesting(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID
    ): ResponseEntity<ApiResponse<EligibilityAtom>> {
        logger.info("Moving atom to testing: {}", atomId)
        
        val atom = atomService.moveAtomToTesting(atomId)
        
        return ResponseEntity.ok(ApiResponse.success(atom, "Atom moved to testing"))
    }
    
    @Operation(
        summary = "Create new version",
        description = "Creates a new version of an existing atom"
    )
    @PostMapping("/{atomId}/versions")
    fun createAtomVersion(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID,
        
        @Valid @RequestBody request: CreateVersionRequest
    ): ResponseEntity<ApiResponse<EligibilityAtom>> {
        logger.info("Creating new version for atom: {}", atomId)
        
        val atom = atomService.createAtomVersion(atomId, request)
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(atom, "New version created successfully"))
    }
    
    // ==========================================================================
    // EXECUTION OPERATIONS
    // ==========================================================================
    
    @Operation(
        summary = "Execute atom",
        description = "Executes an EligibilityAtom with provided input data"
    )
    @PostMapping("/{atomId}/execute")
    fun executeAtom(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID,
        
        @RequestBody input: Map<String, Any>
    ): ResponseEntity<ApiResponse<AtomExecutionResult>> {
        logger.debug("Executing atom: {} with input keys: {}", atomId, input.keys)
        
        val result = atomService.executeAtom(atomId, input)
        
        return ResponseEntity.ok(ApiResponse.success(result))
    }
    
    @Operation(
        summary = "Execute atom by code",
        description = "Executes the latest version of an atom by its code"
    )
    @PostMapping("/code/{code}/execute")
    fun executeAtomByCode(
        @Parameter(description = "Atom code", required = true)
        @PathVariable code: String,
        
        @RequestBody input: Map<String, Any>
    ): ResponseEntity<ApiResponse<AtomExecutionResult>> {
        logger.debug("Executing atom by code: {} with input keys: {}", code, input.keys)
        
        val result = atomService.executeAtomByCode(code, input)
        
        return ResponseEntity.ok(ApiResponse.success(result))
    }
    
    @Operation(
        summary = "Test atom",
        description = "Runs all test cases for an atom and returns results"
    )
    @PostMapping("/{atomId}/test-cases")
    fun testAtom(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID
    ): ResponseEntity<ApiResponse<AtomTestResult>> {
        logger.info("Testing atom: {}", atomId)
        
        val result = atomService.testAtom(atomId)
        
        return ResponseEntity.ok(ApiResponse.success(result))
    }
    
    // ==========================================================================
    // STATISTICS AND ANALYTICS
    // ==========================================================================
    
    @Operation(
        summary = "Get atom statistics",
        description = "Retrieves execution statistics for an atom"
    )
    @GetMapping("/{atomId}/statistics")
    fun getAtomStatistics(
        @Parameter(description = "Atom ID", required = true)
        @PathVariable atomId: UUID
    ): ResponseEntity<ApiResponse<AtomStatistics>> {
        logger.debug("Getting statistics for atom: {}", atomId)
        
        val stats = atomService.getAtomStatistics(atomId)
        
        return ResponseEntity.ok(ApiResponse.success(stats))
    }
    
    @Operation(
        summary = "Get tenant atom statistics",
        description = "Retrieves overall atom statistics for the current tenant"
    )
    @GetMapping("/statistics")
    fun getTenantAtomStatistics(): ResponseEntity<ApiResponse<TenantAtomStatistics>> {
        logger.debug("Getting tenant atom statistics")
        
        val stats = atomService.getTenantAtomStatistics()
        
        return ResponseEntity.ok(ApiResponse.success(stats))
    }
    
    @Operation(
        summary = "Get category counts",
        description = "Retrieves atom counts grouped by category"
    )
    @GetMapping("/statistics/categories")
    fun getAtomCountsByCategory(): ResponseEntity<ApiResponse<Map<AtomCategory, Long>>> {
        logger.debug("Getting atom counts by category")
        
        val counts = atomService.getAtomCountsByCategory()
        
        return ResponseEntity.ok(ApiResponse.success(counts))
    }
}

// =============================================================================
// File: src/main/kotlin/com/kairos/hades/controller/TenantController.kt
// 🔥 HADES Tenant REST Controller
// Author: Sankhadeep Banerjee
// REST API endpoints for Tenant management
// =============================================================================

package com.kairos.hades.controller

import com.kairos.hades.entity.Tenant
import com.kairos.hades.enums.SubscriptionTier
import com.kairos.hades.enums.TenantStatus
import com.kairos.hades.service.TenantService
import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST Controller for Tenant operations
 * Provides tenant management endpoints
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Tenant management operations")
@CrossOrigin(origins = ["\${hades.security.cors.allowed-origins}"])
class TenantController @Autowired constructor(
    private val tenantService: TenantService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TenantController::class.java)
    }
    
    @Operation(
        summary = "Create a new tenant",
        description = "Creates a new tenant organization"
    )
    @PostMapping
    fun createTenant(
        @Valid @RequestBody request: CreateTenantRequest
    ): ResponseEntity<ApiResponse<Tenant>> {
        logger.info("Creating new tenant with slug: {}", request.slug)
        
        val tenant = tenantService.createTenant(request)
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(tenant, "Tenant created successfully"))
    }
    
    @Operation(
        summary = "Get tenant by ID",
        description = "Retrieves a tenant by its unique identifier"
    )
    @GetMapping("/{tenantId}")
    fun getTenantById(
        @Parameter(description = "Tenant ID", required = true)
        @PathVariable tenantId: UUID
    ): ResponseEntity<ApiResponse<Tenant>> {
        logger.debug("Getting tenant by ID: {}", tenantId)
        
        val tenant = tenantService.getTenantById(tenantId)
        
        return ResponseEntity.ok(ApiResponse.success(tenant))
    }
    
    @Operation(
        summary = "Get tenant by slug",
        description = "Retrieves a tenant by its unique slug"
    )
    @GetMapping("/slug/{slug}")
    fun getTenantBySlug(
        @Parameter(description = "Tenant slug", required = true)
        @PathVariable slug: String
    ): ResponseEntity<ApiResponse<Tenant>> {
        logger.debug("Getting tenant by slug: {}", slug)
        
        val tenant = tenantService.getTenantBySlug(slug)
        
        return ResponseEntity.ok(ApiResponse.success(tenant))
    }
    
    @Operation(
        summary = "Get all tenants",
        description = "Retrieves all tenants with pagination and filtering"
    )
    @GetMapping
    fun getAllTenants(
        @Parameter(description = "Search term")
        @RequestParam(required = false) search: String?,
        
        @Parameter(description = "Filter by status")
        @RequestParam(required = false) status: TenantStatus?,
        
        @Parameter(description = "Filter by subscription tier")
        @RequestParam(required = false) tier: SubscriptionTier?,
        
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC)
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Tenant>>> {
        logger.debug("Getting all tenants with filters")
        
        val searchRequest = TenantSearchRequest(
            searchTerm = search,
            status = status,
            subscriptionTier = tier
        )
        
        val tenants = tenantService.searchTenants(searchRequest, pageable)
        
        return ResponseEntity.ok(ApiResponse.success(tenants))
    }
    
    @Operation(
        summary = "Update tenant",
        description = "Updates an existing tenant"
    )
    @PutMapping("/{tenantId}")
    fun updateTenant(
        @Parameter(description = "Tenant ID", required = true)
        @PathVariable tenantId: UUID,
        
        @Valid @RequestBody request: UpdateTenantRequest
    ): ResponseEntity<ApiResponse<Tenant>> {
        logger.info("Updating tenant: {}", tenantId)
        
        val tenant = tenantService.updateTenant(tenantId, request)
        
        return ResponseEntity.ok(ApiResponse.success(tenant, "Tenant updated successfully"))
    }
}

// =============================================================================
// COMMON API RESPONSE WRAPPER
// =============================================================================

/**
 * Standard API response wrapper
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> =
            ApiResponse(success = true, data = data, message = message)
        
        fun <T> error(error: String, data: T? = null): ApiResponse<T> =
            ApiResponse(success = false, data = data, error = error)
    }
}

// =============================================================================
// REQUEST/RESPONSE DATA CLASSES
// =============================================================================

data class CreateTenantRequest(
    val name: String,
    val slug: String,
    val description: String? = null,
    val contactEmail: String,
    val contactPhone: String? = null,
    val websiteUrl: String? = null,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE
)

data class UpdateTenantRequest(
    val name: String? = null,
    val description: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val websiteUrl: String? = null,
    val logoUrl: String? = null
)

data class TenantSearchRequest(
    val searchTerm: String? = null,
    val status: TenantStatus? = null,
    val subscriptionTier: SubscriptionTier? = null
)