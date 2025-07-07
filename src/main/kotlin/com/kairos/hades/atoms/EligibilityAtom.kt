// =============================================================================
// 🔥 HADES BACKEND - ELIGIBILITYATOMS™ FRAMEWORK
// =============================================================================
// Author: Sankhadeep Banerjee
// Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
// File: src/main/kotlin/com/kairos/hades/atoms/EligibilityAtom.kt
// Purpose: Revolutionary reusable decision components for marketing eligibility
// =============================================================================

package com.kairos.hades.atoms

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.kairos.hades.entity.TenantAwareEntity
import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.*
import java.time.LocalDateTime
import java.util.*

// =============================================================================
// CORE ELIGIBILITY ATOM INTERFACES & TYPES
// =============================================================================

@JvmInline
value class AtomId(val value: UUID = UUID.randomUUID())

enum class AtomCategory {
    DEMOGRAPHIC("Demographic", "Age, gender, location-based eligibility"),
    BEHAVIORAL("Behavioral", "Purchase history, engagement patterns"),
    TEMPORAL("Temporal", "Time-based conditions and scheduling"),
    CONTEXTUAL("Contextual", "Device, channel, environment conditions"),
    SEGMENT("Segment", "Customer segment membership"),
    CONSENT("Consent", "Privacy and consent-based eligibility"),
    PREDICTIVE("Predictive", "AI/ML-based predictions"),
    CUSTOM("Custom", "Organization-specific custom logic");
    
    constructor(displayName: String, description: String) : this()
    
    val displayName: String = displayName
    val description: String = description
}

enum class AtomComplexity {
    SIMPLE,     // Single condition check
    MEDIUM,     // Multiple conditions with basic logic
    COMPLEX,    // Advanced logic with nested conditions
    AI_POWERED  // Machine learning based
}

enum class AtomStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    DEPRECATED,
    TESTING
}

data class AtomExecutionContext(
    val customerId: UUID?,
    val sessionId: String?,
    val deviceInfo: Map<String, Any> = emptyMap(),
    val locationInfo: Map<String, Any> = emptyMap(),
    val channelInfo: Map<String, Any> = emptyMap(),
    val temporalInfo: Map<String, Any> = emptyMap(),
    val userAttributes: Map<String, Any> = emptyMap(),
    val behaviorAttributes: Map<String, Any> = emptyMap(),
    val transactionHistory: List<Map<String, Any>> = emptyList(),
    val segmentMemberships: Set<String> = emptySet(),
    val consentStatus: Map<String, Boolean> = emptyMap(),
    val metadata: Map<String, Any> = emptyMap()
)

data class AtomExecutionResult(
    val eligible: Boolean,
    val confidence: Double = 1.0,
    val reason: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val executionTimeMs: Long = 0,
    val debugInfo: Map<String, Any> = emptyMap()
)

// =============================================================================
// ELIGIBILITY ATOM INTERFACE
// =============================================================================

interface EligibilityAtomLogic {
    
    /**
     * Executes the atom's eligibility logic
     * @param context The execution context containing customer and environmental data
     * @return The result of the eligibility check
     */
    suspend fun execute(context: AtomExecutionContext): AtomExecutionResult
    
    /**
     * Validates the atom's configuration
     * @return List of validation errors, empty if valid
     */
    fun validate(): List<String>
    
    /**
     * Returns the estimated execution time in milliseconds
     */
    fun getEstimatedExecutionTime(): Long = 10L
    
    /**
     * Returns the data requirements for this atom
     */
    fun getDataRequirements(): Set<String>
}

// =============================================================================
// ABSTRACT BASE ATOM IMPLEMENTATIONS
// =============================================================================

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = AgeRangeAtom::class, name = "age_range"),
    JsonSubTypes.Type(value = GeographyAtom::class, name = "geography"),
    JsonSubTypes.Type(value = TenureAtom::class, name = "tenure"),
    JsonSubTypes.Type(value = SegmentAtom::class, name = "segment"),
    JsonSubTypes.Type(value = TimeOfDayAtom::class, name = "time_of_day"),
    JsonSubTypes.Type(value = DeviceTypeAtom::class, name = "device_type"),
    JsonSubTypes.Type(value = ConsentAtom::class, name = "consent"),
    JsonSubTypes.Type(value = PurchaseFrequencyAtom::class, name = "purchase_frequency"),
    JsonSubTypes.Type(value = EngagementScoreAtom::class, name = "engagement_score"),
    JsonSubTypes.Type(value = ChurnRiskAtom::class, name = "churn_risk"),
    JsonSubTypes.Type(value = CompositeAtom::class, name = "composite")
)
abstract class AtomConfiguration : EligibilityAtomLogic {
    abstract val type: String
    abstract val name: String
    abstract val description: String
}

// =============================================================================
// BASIC ATOM IMPLEMENTATIONS
// =============================================================================

// Age Range Atom
data class AgeRangeAtom(
    override val type: String = "age_range",
    override val name: String = "Age Range",
    override val description: String = "Checks if customer age falls within specified range",
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val includeUnknown: Boolean = false
) : AtomConfiguration() {
    
    override suspend fun execute(context: AtomExecutionContext): AtomExecutionResult {
        val startTime = System.currentTimeMillis()
        
        val customerAge = context.userAttributes["age"] as? Int
        
        val eligible = when {
            customerAge == null -> includeUnknown
            minAge != null && customerAge < minAge -> false
            maxAge != null && customerAge > maxAge -> false
            else -> true
        }
        
        val reason = when {
            customerAge == null && !includeUnknown -> "Age unknown and not included"
            customerAge != null && minAge != null && customerAge < minAge -> 
                "Age $customerAge below minimum $minAge"
            customerAge != null && maxAge != null && customerAge > maxAge -> 
                "Age $customerAge above maximum $maxAge"
            else -> "Age criteria met"
        }
        
        return AtomExecutionResult(
            eligible = eligible,
            reason = reason,
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf(
                "customerAge" to customerAge,
                "minAge" to minAge,
                "maxAge" to maxAge
            )
        )
    }
    
    override fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (minAge != null && minAge < 0) errors.add("Minimum age cannot be negative")
        if (maxAge != null && maxAge < 0) errors.add("Maximum age cannot be negative")
        if (minAge != null && maxAge != null && minAge > maxAge) {
            errors.add("Minimum age cannot be greater than maximum age")
        }
        return errors
    }
    
    override fun getDataRequirements(): Set<String> = setOf("userAttributes.age")
}

// Geography Atom
data class GeographyAtom(
    override val type: String = "geography",
    override val name: String = "Geography",
    override val description: String = "Checks customer location against specified criteria",
    val includedCountries: Set<String> = emptySet(),
    val excludedCountries: Set<String> = emptySet(),
    val includedStates: Set<String> = emptySet(),
    val excludedStates: Set<String> = emptySet(),
    val includedCities: Set<String> = emptySet(),
    val excludedCities: Set<String> = emptySet(),
    val includeUnknown: Boolean = false
) : AtomConfiguration() {
    
    override suspend fun execute(context: AtomExecutionContext): AtomExecutionResult {
        val startTime = System.currentTimeMillis()
        
        val country = context.locationInfo["country"] as? String
        val state = context.locationInfo["state"] as? String
        val city = context.locationInfo["city"] as? String
        
        val eligible = when {
            country == null && state == null && city == null -> includeUnknown
            excludedCountries.isNotEmpty() && country in excludedCountries -> false
            excludedStates.isNotEmpty() && state in excludedStates -> false
            excludedCities.isNotEmpty() && city in excludedCities -> false
            includedCountries.isNotEmpty() && country !in includedCountries -> false
            includedStates.isNotEmpty() && state !in includedStates -> false
            includedCities.isNotEmpty() && city !in includedCities -> false
            else -> true
        }
        
        return AtomExecutionResult(
            eligible = eligible,
            reason = if (eligible) "Geography criteria met" else "Geography criteria not met",
            executionTimeMs = System.currentTimeMillis() - startTime,
            metadata = mapOf(
                "country" to country,
                "state" to state,
                "city" to city
            )
        )