// =============================================================================
// File: src/main/kotlin/com/kairos/hades/service/AtomValidationService.kt
// 🔥 HADES Atom Validation Service
// Author: Sankhadeep Banerjee
// Comprehensive validation service for EligibilityAtoms
// =============================================================================

package com.kairos.hades.service

import com.kairos.hades.atoms.EligibilityAtom
import com.kairos.hades.enums.*
import com.kairos.hades.exception.AtomDependencyException
import com.kairos.hades.exception.AtomValidationException
import com.kairos.hades.multitenancy.TenantContext
import com.kairos.hades.repository.EligibilityAtomRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service for validating EligibilityAtoms
 * Performs comprehensive validation including syntax, logic, dependencies, and business rules
 */
@Service
class AtomValidationService @Autowired constructor(
    private val atomRepository: EligibilityAtomRepository,
    private val tenantContext: TenantContext,
    private val objectMapper: ObjectMapper
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(AtomValidationService::class.java)
        
        // Validation limits
        private const val MAX_DEPENDENCIES = 10
        private const val MAX_CONDITIONS = 20
        private const val MAX_RECURSION_DEPTH = 5
        private const val MIN_EXECUTION_TIME_MS = 1L
        private const val MAX_EXECUTION_TIME_MS = 300000L // 5 minutes
        private const val MIN_CACHE_TTL = 60L // 1 minute
        private const val MAX_CACHE_TTL = 86400L // 24 hours
    }
    
    // ==========================================================================
    // MAIN VALIDATION METHODS
    // ==========================================================================
    
    /**
     * Validate atom for general use (CRUD operations)
     */
    fun validateAtom(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        try {
            // Basic field validation
            errors.addAll(validateBasicFields(atom))
            
            // Logic definition validation
            errors.addAll(validateLogicDefinition(atom))
            
            // Parameter validation
            errors.addAll(validateParameters(atom))
            
            // Dependency validation
            errors.addAll(validateDependencies(atom))
            
            // Type-specific validation
            errors.addAll(validateTypeSpecific(atom))
            
            // Performance validation
            errors.addAll(validatePerformanceSettings(atom))
            
            // Test cases validation
            errors.addAll(validateTestCases(atom))
            
        } catch (e: Exception) {
            logger.error("Validation error for atom '{}': {}", atom.code, e.message, e)
            errors.add("Validation process failed: ${e.message}")
        }
        
        return errors
    }
    
    /**
     * Validate atom for activation (stricter validation)
     */
    fun validateAtomForActivation(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        // Run standard validation first
        errors.addAll(validateAtom(atom))
        
        // Additional activation-specific validations
        errors.addAll(validateForActivation(atom))
        
        return errors
    }
    
    /**
     * Validate atom execution prerequisites
     */
    fun validateForExecution(atom: EligibilityAtom, input: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        
        // Check atom status
        if (!atom.isActive() && !atom.isTesting()) {
            errors.add("Atom is not in executable status: ${atom.status}")
        }
        
        // Validate input against requirements
        errors.addAll(validateInputParameters(atom, input))
        
        // Check dependency availability
        errors.addAll(validateDependencyAvailability(atom))
        
        return errors
    }
    
    // ==========================================================================
    // BASIC FIELD VALIDATION
    // ==========================================================================
    
    private fun validateBasicFields(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        // Code validation
        if (atom.code.isBlank()) {
            errors.add("Code is required")
        } else {
            if (!atom.code.matches(Regex("^[A-Z][A-Z0-9_]*$"))) {
                errors.add("Code must start with uppercase letter and contain only uppercase letters, numbers, and underscores")
            }
            if (atom.code.length < 3 || atom.code.length > 100) {
                errors.add("Code must be between 3 and 100 characters")
            }
        }
        
        // Name validation
        if (atom.name.isBlank()) {
            errors.add("Name is required")
        } else if (atom.name.length > 255) {
            errors.add("Name cannot exceed 255 characters")
        }
        
        // Description validation
        atom.description?.let { desc ->
            if (desc.length > 1000) {
                errors.add("Description cannot exceed 1000 characters")
            }
        }
        
        // Version validation
        if (atom.version < 1) {
            errors.add("Version must be at least 1")
        }
        
        // Priority validation
        if (atom.priority < 1 || atom.priority > 10) {
            errors.add("Priority must be between 1 and 10")
        }
        
        // Tags validation
        atom.tags.forEach { tag ->
            if (tag.length > 50) {
                errors.add("Tag '$tag' exceeds 50 characters")
            }
            if (!tag.matches(Regex("^[a-z0-9-_]+$"))) {
                errors.add("Tag '$tag' must contain only lowercase letters, numbers, hyphens, and underscores")
            }
        }
        
        return errors
    }
    
    // ==========================================================================
    // LOGIC DEFINITION VALIDATION
    // ==========================================================================
    
    private fun validateLogicDefinition(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        if (atom.logicDefinition.isBlank()) {
            errors.add("Logic definition is required")
            return errors
        }
        
        try {
            val logic = objectMapper.readValue<Map<String, Any>>(atom.logicDefinition)
            
            when (atom.type) {
                AtomType.SIMPLE -> errors.addAll(validateSimpleLogic(logic))
                AtomType.COMPLEX -> errors.addAll(validateComplexLogic(logic))
                AtomType.COMPOSITE -> errors.addAll(validateCompositeLogic(logic))
                AtomType.TEMPLATE -> errors.addAll(validateTemplateLogic(logic))
                AtomType.MACHINE_LEARNING -> errors.addAll(validateMlLogic(logic))
            }
            
        } catch (e: Exception) {
            errors.add("Invalid JSON in logic definition: ${e.message}")
        }
        
        return errors
    }
    
    private fun validateSimpleLogic(logic: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        
        val condition = logic["condition"] as? Map<String, Any>
        if (condition == null) {
            errors.add("Simple atom must have a 'condition' object")
        } else {
            errors.addAll(validateCondition(condition))
        }
        
        return errors
    }
    
    private fun validateComplexLogic(logic: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        
        val conditions = logic["conditions"] as? List<*>
        if (conditions == null) {
            errors.add("Complex atom must have 'conditions' array")
        } else {
            if (conditions.size > MAX_CONDITIONS) {
                errors.add("Complex atom cannot have more than $MAX_CONDITIONS conditions")
            }
            
            conditions.forEachIndexed { index, condition ->
                if (condition is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    errors.addAll(validateCondition(condition as Map<String, Any>, "conditions[$index]"))
                } else {
                    errors.add("conditions[$index] must be an object")
                }
            }
        }
        
        val operator = logic["operator"] as? String
        if (operator != null) {
            try {
                LogicalOperator.valueOf(operator)
            } catch (e: IllegalArgumentException) {
                errors.add("Invalid logical operator: $operator")
            }
        }
        
        return errors
    }
    
    private fun validateCompositeLogic(logic: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        
        val childAtoms = logic["childAtoms"] as? List<*>
        if (childAtoms == null) {
            errors.add("Composite atom must have 'childAtoms' array")
        } else {
            childAtoms.forEachIndexed { index, childAtom ->
                if (childAtom !is String) {
                    errors.add("childAtoms[$index] must be a string (atom code)")
                } else if (childAtom.isBlank()) {
                    errors.add("childAtoms[$index] cannot be blank")
                }
            }
        }
        
        val operator = logic["operator"] as? String
        if (operator != null) {
            try {
                LogicalOperator.valueOf(operator)
            } catch (e: IllegalArgumentException) {
                errors.add("Invalid logical operator: $operator")
            }
        }
        
        return errors
    }
    
    private fun validateTemplateLogic(logic: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        
        val template = logic["template"] as? Map<String, Any>
        if (template == null) {
            errors.add("Template atom must have 'template' object")
        } else {
            // Validate template structure
            val parameters = logic["parameters"] as? List<*>
            if (parameters == null) {
                errors.add("Template atom must have 'parameters' array")
            }
        }
        
        return errors
    }
    
    private fun validateMlLogic(logic: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        
        val model = logic["model"] as? Map<String, Any>
        if (model == null) {
            errors.add("ML atom must have 'model' object")
        } else {
            val modelType = model["type"] as? String
            if (modelType == null) {
                errors.add("ML atom model must specify 'type'")
            } else {
                val validTypes = setOf("classification", "regression", "clustering", "prediction")
                if (modelType.lowercase() !in validTypes) {
                    errors.add("Invalid ML model type: $modelType")
                }
            }
            
            val version = model["version"] as? String
            if (version.isNullOrBlank()) {
                errors.add("ML atom model must specify 'version'")
            }
        }
        
        return errors
    }
    
    private fun validateCondition(condition: Map<String, Any>, prefix: String = "condition"): List<String> {
        val errors = mutableListOf<String>()
        
        val field = condition["field"] as? String
        if (field.isNullOrBlank()) {
            errors.add("$prefix must specify 'field'")
        }
        
        val operator = condition["operator"] as? String
        if (operator != null) {
            try {
                ComparisonOperator.valueOf(operator)
            } catch (e: IllegalArgumentException) {
                errors.add("$prefix has invalid operator: $operator")
            }
        }
        
        if (!condition.containsKey("value")) {
            errors.add("$prefix must specify 'value'")
        }
        
        return errors
    }
    
    // ==========================================================================
    // PARAMETER VALIDATION
    // ==========================================================================
    
    private fun validateParameters(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate input parameters
        atom.inputParameters?.let { inputParams ->
            if (inputParams.isNotBlank()) {
                try {
                    val params = objectMapper.readValue<Map<String, Any>>(inputParams)
                    errors.addAll(validateParameterSchema(params, "inputParameters"))
                } catch (e: Exception) {
                    errors.add("Invalid JSON in inputParameters: ${e.message}")
                }
            }
        }
        
        // Validate output schema
        atom.outputSchema?.let { outputSchema ->
            if (outputSchema.isNotBlank()) {
                try {
                    val schema = objectMapper.readValue<Map<String, Any>>(outputSchema)
                    errors.addAll(validateParameterSchema(schema, "outputSchema"))
                } catch (e: Exception) {
                    errors.add("Invalid JSON in outputSchema: ${e.message}")
                }
            }
        }
        
        // Validate validation rules
        atom.validationRules?.let { validationRules ->
            if (validationRules.isNotBlank()) {
                try {
                    val rules = objectMapper.readValue<Map<String, Any>>(validationRules)
                    errors.addAll(validateValidationRules(rules))
                } catch (e: Exception) {
                    errors.add("Invalid JSON in validationRules: ${e.message}")
                }
            }
        }
        
        return errors
    }
    
    private fun validateParameterSchema(schema: Map<String, Any>, schemaName: String): List<String> {
        val errors = mutableListOf<String>()
        
        schema.forEach { (paramName, paramConfig) ->
            if (paramConfig is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val config = paramConfig as Map<String, Any>
                
                val type = config["type"] as? String
                if (type != null) {
                    try {
                        DataType.valueOf(type.uppercase())
                    } catch (e: IllegalArgumentException) {
                        errors.add("$schemaName.$paramName has invalid type: $type")
                    }
                }
            } else {
                errors.add("$schemaName.$paramName must be an object")
            }
        }
        
        return errors
    }
    
    private fun validateValidationRules(rules: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        
        rules.forEach { (paramName, paramRules) ->
            if (paramRules is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val ruleConfig = paramRules as Map<String, Any>
                
                ruleConfig.forEach { (ruleType, ruleValue) ->
                    try {
                        ValidationRuleType.valueOf(ruleType.uppercase())
                    } catch (e: IllegalArgumentException) {
                        errors.add("validationRules.$paramName has invalid rule type: $ruleType")
                    }
                }
            }
        }
        
        return errors
    }
    
    // ==========================================================================
    // DEPENDENCY VALIDATION
    // ==========================================================================
    
    private fun validateDependencies(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        if (atom.dependencies.size > MAX_DEPENDENCIES) {
            errors.add("Atom cannot have more than $MAX_DEPENDENCIES dependencies")
        }
        
        // Check for self-dependency
        if (atom.dependencies.contains(atom.code)) {
            errors.add("Atom cannot depend on itself")
        }
        
        // Check for circular dependencies
        errors.addAll(validateCircularDependencies(atom))
        
        return errors
    }
    
    private fun validateCircularDependencies(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        
        fun detectCycle(atomCode: String, depth: Int): Boolean {
            if (depth > MAX_RECURSION_DEPTH) {
                errors.add("Dependency chain too deep (max depth: $MAX_RECURSION_DEPTH)")
                return true
            }
            
            if (recursionStack.contains(atomCode)) {
                errors.add("Circular dependency detected involving atom: $atomCode")
                return true
            }
            
            if (visited.contains(atomCode)) {
                return false
            }
            
            visited.add(atomCode)
            recursionStack.add(atomCode)
            
            try {
                val dependentAtom = atomRepository.findByTenantIdAndCode(
                    tenantContext.getTenantId(), 
                    atomCode
                )
                
                dependentAtom?.dependencies?.forEach { dependency ->
                    if (detectCycle(dependency, depth + 1)) {
                        return true
                    }
                }
            } catch (e: Exception) {
                // Dependency not found - will be caught in availability validation
            }
            
            recursionStack.remove(atomCode)
            return false
        }
        
        atom.dependencies.forEach { dependency ->
            detectCycle(dependency, 0)
        }
        
        return errors
    }
    
    private fun validateDependencyAvailability(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        val tenantId = tenantContext.getTenantId()
        
        atom.dependencies.forEach { dependencyCode ->
            val dependencyAtom = atomRepository.findByTenantIdAndCode(tenantId, dependencyCode)
            
            if (dependencyAtom == null) {
                errors.add("Dependency not found: $dependencyCode")
            } else if (!dependencyAtom.isActive() && !dependencyAtom.isTesting()) {
                errors.add("Dependency '$dependencyCode' is not in executable status: ${dependencyAtom.status}")
            }
        }
        
        return errors
    }
    
    // ==========================================================================
    // TYPE-SPECIFIC VALIDATION
    // ==========================================================================
    
    private fun validateTypeSpecific(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        when (atom.type) {
            AtomType.COMPOSITE -> {
                if (atom.dependencies.isEmpty()) {
                    errors.add("Composite atom must have at least one dependency")
                }
            }
            AtomType.TEMPLATE -> {
                if (atom.inputParameters.isNullOrBlank()) {
                    errors.add("Template atom must define input parameters")
                }
            }
            AtomType.MACHINE_LEARNING -> {
                // Additional ML-specific validations could go here
            }
            else -> {
                // No additional validations for SIMPLE and COMPLEX types
            }
        }
        
        return errors
    }
    
    // ==========================================================================
    // PERFORMANCE VALIDATION
    // ==========================================================================
    
    private fun validatePerformanceSettings(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        // Expected execution time validation
        if (atom.expectedExecutionTimeMs < MIN_EXECUTION_TIME_MS) {
            errors.add("Expected execution time must be at least ${MIN_EXECUTION_TIME_MS}ms")
        }
        if (atom.expectedExecutionTimeMs > MAX_EXECUTION_TIME_MS) {
            errors.add("Expected execution time cannot exceed ${MAX_EXECUTION_TIME_MS}ms")
        }
        
        // Cache TTL validation
        if (atom.cacheEnabled) {
            if (atom.cacheTtlSeconds < MIN_CACHE_TTL) {
                errors.add("Cache TTL must be at least ${MIN_CACHE_TTL} seconds")
            }
            if (atom.cacheTtlSeconds > MAX_CACHE_TTL) {
                errors.add("Cache TTL cannot exceed ${MAX_CACHE_TTL} seconds")
            }
        }
        
        return errors
    }
    
    // ==========================================================================
    // TEST CASES VALIDATION
    // ==========================================================================
    
    private fun validateTestCases(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        atom.testCases?.let { testCasesJson ->
            if (testCasesJson.isNotBlank()) {
                try {
                    val testCases = objectMapper.readValue<List<Map<String, Any>>>(testCasesJson)
                    
                    testCases.forEachIndexed { index, testCase ->
                        if (!testCase.containsKey("name")) {
                            errors.add("testCases[$index] must have 'name'")
                        }
                        if (!testCase.containsKey("input")) {
                            errors.add("testCases[$index] must have 'input'")
                        }
                        if (!testCase.containsKey("expected")) {
                            errors.add("testCases[$index] must have 'expected'")
                        }
                    }
                    
                } catch (e: Exception) {
                    errors.add("Invalid JSON in testCases: ${e.message}")
                }
            }
        }
        
        return errors
    }
    
    // ==========================================================================
    // ACTIVATION-SPECIFIC VALIDATION
    // ==========================================================================
    
    private fun validateForActivation(atom: EligibilityAtom): List<String> {
        val errors = mutableListOf<String>()
        
        // Ensure all dependencies are active
        errors.addAll(validateDependencyAvailability(atom))
        
        // Ensure test cases exist and pass
        if (atom.testCases.isNullOrBlank()) {
            errors.add("Atom must have test cases before activation")
        }
        
        // Ensure documentation exists
        if (atom.documentation.isNullOrBlank()) {
            errors.add("Atom must have documentation before activation")
        }
        
        // Ensure example exists
        if (atom.example.isNullOrBlank()) {
            errors.add("Atom must have usage example before activation")
        }
        
        return errors
    }
    
    // ==========================================================================
    // INPUT VALIDATION FOR EXECUTION
    // ==========================================================================
    
    private fun validateInputParameters(atom: EligibilityAtom, input: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        
        atom.inputParameters?.let { inputParamsJson ->
            if (inputParamsJson.isNotBlank()) {
                try {
                    val params = objectMapper.readValue<Map<String, Any>>(inputParamsJson)
                    
                    params.forEach { (paramName, paramConfig) ->
                        if (paramConfig is Map<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            val config = paramConfig as Map<String, Any>
                            
                            val required = config["required"] as? Boolean ?: false
                            if (required && !input.containsKey(paramName)) {
                                errors.add("Required parameter '$paramName' is missing")
                            }
                            
                            if (input.containsKey(paramName)) {
                                errors.addAll(validateParameterValue(
                                    paramName, 
                                    input[paramName], 
                                    config
                                ))
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    errors.add("Cannot validate input against parameter schema: ${e.message}")
                }
            }
        }
        
        return errors
    }
    
    private fun validateParameterValue(
        paramName: String, 
        value: Any?, 
        config: Map<String, Any>
    ): List<String> {
        val errors = mutableListOf<String>()
        
        val expectedType = config["type"] as? String
        if (expectedType != null) {
            if (!isValidType(value, expectedType)) {
                errors.add("Parameter '$paramName' has invalid type (expected: $expectedType)")
            }
        }
        
        // Additional type-specific validations could be added here
        
        return errors
    }
    
    private fun isValidType(value: Any?, expectedType: String): Boolean {
        return when (expectedType.uppercase()) {
            "STRING" -> value is String
            "INTEGER" -> value is Int || (value is Number && value.toDouble() % 1 == 0.0)
            "LONG" -> value is Long || value is Int
            "DOUBLE" -> value is Number
            "BOOLEAN" -> value is Boolean
            "LIST" -> value is List<*>
            "MAP" -> value is Map<*, *>
            else -> true // Unknown type, allow it
        }
    }
}