// =============================================================================
// File: src/main/kotlin/com/kairos/hades/service/AtomExecutionService.kt
// 🔥 HADES Atom Execution Service
// Author: Sankhadeep Banerjee
// Core service for executing EligibilityAtoms with caching and monitoring
// =============================================================================

package com.kairos.hades.service

import com.kairos.hades.atoms.EligibilityAtom
import com.kairos.hades.config.HadesProperties
import com.kairos.hades.enums.*
import com.kairos.hades.exception.*
import com.kairos.hades.multitenancy.TenantContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.*
import kotlin.system.measureTimeMillis

/**
 * Service responsible for executing EligibilityAtoms
 * Handles logic execution, caching, dependency resolution, and performance monitoring
 */
@Service
class AtomExecutionService @Autowired constructor(
    private val tenantContext: TenantContext,
    private val hadesProperties: HadesProperties,
    private val objectMapper: ObjectMapper,
    private val cacheManager: CacheManager,
    private val atomDependencyResolver: AtomDependencyResolver,
    private val executorService: ThreadPoolExecutor
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(AtomExecutionService::class.java)
        private const val CACHE_NAME = "atom-results"
        private const val DEFAULT_TIMEOUT_MS = 30000L
    }
    
    // ==========================================================================
    // MAIN EXECUTION METHODS
    // ==========================================================================
    
    /**
     * Execute an atom with given input parameters
     */
    fun executeAtom(atom: EligibilityAtom, input: Map<String, Any>): AtomExecutionResult {
        val tenantId = tenantContext.getTenantId()
        val requestId = tenantContext.getRequestId()
        
        logger.info("Executing atom '{}' (ID: {}) for tenant '{}' with request '{}'", 
            atom.code, atom.id, tenantId, requestId)
        
        // Validate atom status
        if (!atom.isActive() && !atom.isTesting()) {
            throw AtomExecutionException("Cannot execute atom in status: ${atom.status}")
        }
        
        // Validate input parameters
        validateInput(atom, input)
        
        // Check cache first
        val cacheKey = generateCacheKey(atom, input)
        if (atom.cacheEnabled) {
            getCachedResult(cacheKey)?.let { cachedResult ->
                logger.debug("Cache hit for atom '{}' with key '{}'", atom.code, cacheKey)
                return cachedResult.copy(cacheHit = true)
            }
        }
        
        // Resolve dependencies
        val dependencyResults = resolveDependencies(atom, input)
        
        // Create execution context
        val context = AtomExecutionContext(
            atom = atom,
            input = input,
            dependencyResults = dependencyResults,
            tenantId = tenantId,
            requestId = requestId,
            executionTime = LocalDateTime.now()
        )
        
        // Execute with timeout
        val result = executeWithTimeout(context)
        
        // Cache result if successful and caching is enabled
        if (result.success && atom.cacheEnabled) {
            cacheResult(cacheKey, result, atom.cacheTtlSeconds)
        }
        
        // Update atom statistics (async)
        CompletableFuture.runAsync {
            updateAtomStatistics(atom, result)
        }
        
        logger.info("Completed execution of atom '{}' in {}ms - Success: {}", 
            atom.code, result.executionTimeMs, result.success)
        
        return result
    }
    
    /**
     * Test an atom using its defined test cases
     */
    fun testAtom(atom: EligibilityAtom): AtomTestResult {
        logger.info("Testing atom '{}' with test cases", atom.code)
        
        val testCases = parseTestCases(atom.testCases)
        if (testCases.isEmpty()) {
            return AtomTestResult(
                atomId = atom.id!!,
                totalTests = 0,
                passedTests = 0,
                failedTests = 0,
                testResults = emptyList()
            )
        }
        
        val testResults = testCases.map { testCase ->
            runTestCase(atom, testCase)
        }
        
        val passedTests = testResults.count { it.success }
        val failedTests = testResults.count { !it.success }
        
        logger.info("Test completed for atom '{}' - Passed: {}, Failed: {}", 
            atom.code, passedTests, failedTests)
        
        return AtomTestResult(
            atomId = atom.id!!,
            totalTests = testResults.size,
            passedTests = passedTests,
            failedTests = failedTests,
            testResults = testResults
        )
    }
    
    // ==========================================================================
    // EXECUTION LOGIC
    // ==========================================================================
    
    /**
     * Execute atom with timeout protection
     */
    private fun executeWithTimeout(context: AtomExecutionContext): AtomExecutionResult {
        val timeout = hadesProperties.atoms.executionTimeout ?: DEFAULT_TIMEOUT_MS
        
        val future = CompletableFuture.supplyAsync({
            executeAtomLogic(context)
        }, executorService)
        
        return try {
            future.get(timeout, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw AtomExecutionTimeoutException(
                "Atom execution timed out after ${timeout}ms",
                timeout,
                context.atom.code
            )
        } catch (e: ExecutionException) {
            throw AtomExecutionException(
                "Atom execution failed: ${e.cause?.message}",
                context.atom.code,
                cause = e.cause
            )
        }
    }
    
    /**
     * Execute the core atom logic based on its type
     */
    private fun executeAtomLogic(context: AtomExecutionContext): AtomExecutionResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = when (context.atom.type) {
                AtomType.SIMPLE -> executeSimpleAtom(context)
                AtomType.COMPLEX -> executeComplexAtom(context)
                AtomType.COMPOSITE -> executeCompositeAtom(context)
                AtomType.TEMPLATE -> executeTemplateAtom(context)
                AtomType.MACHINE_LEARNING -> executeMlAtom(context)
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            AtomExecutionResult(
                atomId = context.atom.id!!,
                success = true,
                result = result,
                executionTimeMs = executionTime,
                cacheHit = false
            )
            
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            
            logger.error("Atom execution failed for '{}': {}", context.atom.code, e.message, e)
            
            AtomExecutionResult(
                atomId = context.atom.id!!,
                success = false,
                error = e.message ?: "Unknown execution error",
                executionTimeMs = executionTime,
                cacheHit = false
            )
        }
    }
    
    /**
     * Execute simple atom with single condition
     */
    private fun executeSimpleAtom(context: AtomExecutionContext): Any {
        val logic = parseLogicDefinition(context.atom.logicDefinition)
        val condition = logic["condition"] as? Map<String, Any>
            ?: throw AtomExecutionException("Simple atom must have a condition")
        
        return evaluateCondition(condition, context.input, context.dependencyResults)
    }
    
    /**
     * Execute complex atom with multiple conditions and operators
     */
    private fun executeComplexAtom(context: AtomExecutionContext): Any {
        val logic = parseLogicDefinition(context.atom.logicDefinition)
        val conditions = logic["conditions"] as? List<Map<String, Any>>
            ?: throw AtomExecutionException("Complex atom must have conditions")
        
        val operator = LogicalOperator.valueOf(
            (logic["operator"] as? String) ?: "AND"
        )
        
        val results = conditions.map { condition ->
            evaluateCondition(condition, context.input, context.dependencyResults)
        }
        
        return applyLogicalOperator(operator, results)
    }
    
    /**
     * Execute composite atom by combining child atoms
     */
    private fun executeCompositeAtom(context: AtomExecutionContext): Any {
        val logic = parseLogicDefinition(context.atom.logicDefinition)
        val childAtoms = logic["childAtoms"] as? List<String>
            ?: throw AtomExecutionException("Composite atom must have child atoms")
        
        val operator = LogicalOperator.valueOf(
            (logic["operator"] as? String) ?: "AND"
        )
        
        // Execute child atoms
        val childResults = childAtoms.map { atomCode ->
            val childAtom = atomDependencyResolver.resolveAtom(atomCode)
            executeAtom(childAtom, context.input).result
        }
        
        return applyLogicalOperator(operator, childResults)
    }
    
    /**
     * Execute template atom with parameter substitution
     */
    private fun executeTemplateAtom(context: AtomExecutionContext): Any {
        val logic = parseLogicDefinition(context.atom.logicDefinition)
        val template = logic["template"] as? Map<String, Any>
            ?: throw AtomExecutionException("Template atom must have template definition")
        
        // Substitute template parameters with input values
        val substitutedLogic = substituteTemplateParameters(template, context.input)
        
        // Create temporary context with substituted logic
        val tempAtom = context.atom.copy().apply {
            logicDefinition = objectMapper.writeValueAsString(substitutedLogic)
            type = AtomType.SIMPLE // Execute as simple after substitution
        }
        
        val tempContext = context.copy(atom = tempAtom)
        return executeSimpleAtom(tempContext)
    }
    
    /**
     * Execute machine learning atom
     */
    private fun executeMlAtom(context: AtomExecutionContext): Any {
        val logic = parseLogicDefinition(context.atom.logicDefinition)
        val modelConfig = logic["model"] as? Map<String, Any>
            ?: throw AtomExecutionException("ML atom must have model configuration")
        
        val modelType = modelConfig["type"] as? String
            ?: throw AtomExecutionException("ML atom must specify model type")
        
        return when (modelType.lowercase()) {
            "classification" -> executeClassificationModel(modelConfig, context.input)
            "regression" -> executeRegressionModel(modelConfig, context.input)
            "clustering" -> executeClusteringModel(modelConfig, context.input)
            "prediction" -> executePredictionModel(modelConfig, context.input)
            else -> throw AtomExecutionException("Unsupported ML model type: $modelType")
        }
    }
    
    // ==========================================================================
    // CONDITION EVALUATION
    // ==========================================================================
    
    /**
     * Evaluate a single condition
     */
    private fun evaluateCondition(
        condition: Map<String, Any>,
        input: Map<String, Any>,
        dependencyResults: Map<String, Any>
    ): Boolean {
        val field = condition["field"] as? String
            ?: throw AtomExecutionException("Condition must specify field")
        
        val operator = ComparisonOperator.valueOf(
            (condition["operator"] as? String) ?: "EQUALS"
        )
        
        val expectedValue = condition["value"]
        
        // Get actual value from input or dependency results
        val actualValue = input[field] ?: dependencyResults[field]
        
        return compareValues(actualValue, expectedValue, operator)
    }
    
    /**
     * Compare two values using the specified operator
     */
    private fun compareValues(actual: Any?, expected: Any?, operator: ComparisonOperator): Boolean {
        return when (operator) {
            ComparisonOperator.EQUALS -> actual == expected
            ComparisonOperator.NOT_EQUALS -> actual != expected
            ComparisonOperator.GREATER_THAN -> compareNumbers(actual, expected) { a, e -> a > e }
            ComparisonOperator.GREATER_THAN_OR_EQUAL -> compareNumbers(actual, expected) { a, e -> a >= e }
            ComparisonOperator.LESS_THAN -> compareNumbers(actual, expected) { a, e -> a < e }
            ComparisonOperator.LESS_THAN_OR_EQUAL -> compareNumbers(actual, expected) { a, e -> a <= e }
            ComparisonOperator.CONTAINS -> actual.toString().contains(expected.toString())
            ComparisonOperator.NOT_CONTAINS -> !actual.toString().contains(expected.toString())
            ComparisonOperator.STARTS_WITH -> actual.toString().startsWith(expected.toString())
            ComparisonOperator.ENDS_WITH -> actual.toString().endsWith(expected.toString())
            ComparisonOperator.IN -> (expected as? List<*>)?.contains(actual) ?: false
            ComparisonOperator.NOT_IN -> !((expected as? List<*>)?.contains(actual) ?: false)
            ComparisonOperator.IS_NULL -> actual == null
            ComparisonOperator.IS_NOT_NULL -> actual != null
            ComparisonOperator.MATCHES -> actual.toString().matches(Regex(expected.toString()))
            ComparisonOperator.NOT_MATCHES -> !actual.toString().matches(Regex(expected.toString()))
            ComparisonOperator.BETWEEN -> {
                val range = expected as? List<*>
                if (range?.size == 2) {
                    val min = range[0]
                    val max = range[1]
                    compareNumbers(actual, min) { a, m -> a >= m } && 
                    compareNumbers(actual, max) { a, m -> a <= m }
                } else false
            }
            ComparisonOperator.NOT_BETWEEN -> {
                val range = expected as? List<*>
                if (range?.size == 2) {
                    val min = range[0]
                    val max = range[1]
                    !(compareNumbers(actual, min) { a, m -> a >= m } && 
                      compareNumbers(actual, max) { a, m -> a <= m })
                } else true
            }
        }
    }
    
    /**
     * Helper for numeric comparisons
     */
    private fun compareNumbers(
        actual: Any?, 
        expected: Any?, 
        comparison: (Double, Double) -> Boolean
    ): Boolean {
        return try {
            val actualNum = when (actual) {
                is Number -> actual.toDouble()
                is String -> actual.toDouble()
                else -> return false
            }
            
            val expectedNum = when (expected) {
                is Number -> expected.toDouble()
                is String -> expected.toDouble()
                else -> return false
            }
            
            comparison(actualNum, expectedNum)
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * Apply logical operator to a list of boolean results
     */
    private fun applyLogicalOperator(operator: LogicalOperator, results: List<Any>): Boolean {
        val boolResults = results.map { 
            when (it) {
                is Boolean -> it
                is Number -> it.toDouble() != 0.0
                is String -> it.isNotBlank()
                else -> it != null
            }
        }
        
        return when (operator) {
            LogicalOperator.AND -> boolResults.all { it }
            LogicalOperator.OR -> boolResults.any { it }
            LogicalOperator.NOT -> boolResults.size == 1 && !boolResults[0]
            LogicalOperator.XOR -> boolResults.count { it } == 1
            LogicalOperator.NAND -> !boolResults.all { it }
            LogicalOperator.NOR -> !boolResults.any { it }
        }
    }
    
    // ==========================================================================
    // ML MODEL EXECUTION (PLACEHOLDER IMPLEMENTATIONS)
    // ==========================================================================
    
    private fun executeClassificationModel(config: Map<String, Any>, input: Map<String, Any>): Any {
        // TODO: Implement actual ML model execution
        // This is a placeholder implementation
        val threshold = (config["threshold"] as? Number)?.toDouble() ?: 0.5
        val score = generateMockScore(input)
        
        return mapOf(
            "prediction" to if (score >= threshold) "ELIGIBLE" else "NOT_ELIGIBLE",
            "confidence" to score,
            "threshold" to threshold
        )
    }
    
    private fun executeRegressionModel(config: Map<String, Any>, input: Map<String, Any>): Any {
        // TODO: Implement actual regression model
        return mapOf(
            "prediction" to generateMockScore(input),
            "modelVersion" to config["version"] ?: "1.0"
        )
    }
    
    private fun executeClusteringModel(config: Map<String, Any>, input: Map<String, Any>): Any {
        // TODO: Implement actual clustering model
        val clusters = (config["clusters"] as? Number)?.toInt() ?: 3
        return mapOf(
            "cluster" to (generateMockScore(input) * clusters).toInt(),
            "totalClusters" to clusters
        )
    }
    
    private fun executePredictionModel(config: Map<String, Any>, input: Map<String, Any>): Any {
        // TODO: Implement actual prediction model
        return mapOf(
            "prediction" to generateMockScore(input),
            "factors" to input.keys.take(3)
        )
    }
    
    private fun generateMockScore(input: Map<String, Any>): Double {
        // Generate deterministic score based on input for testing
        val hash = input.toString().hashCode()
        return (hash % 100).absoluteValue / 100.0
    }
    
    // ==========================================================================
    // HELPER METHODS
    // ==========================================================================
    
    private fun validateInput(atom: EligibilityAtom, input: Map<String, Any>) {
        atom.inputParameters?.let { inputParamsJson ->
            val inputParams = parseInputParameters(inputParamsJson)
            
            // Check required parameters
            inputParams.forEach { (paramName, paramConfig) ->
                val required = (paramConfig as? Map<*, *>)?.get("required") as? Boolean ?: false
                if (required && !input.containsKey(paramName)) {
                    throw MissingParameterException(paramName)
                }
            }
        }
    }
    
    private fun resolveDependencies(atom: EligibilityAtom, input: Map<String, Any>): Map<String, Any> {
        if (atom.dependencies.isEmpty()) {
            return emptyMap()
        }
        
        return atom.dependencies.associateWith { dependencyCode ->
            val dependencyAtom = atomDependencyResolver.resolveAtom(dependencyCode)
            executeAtom(dependencyAtom, input).result ?: false
        }
    }
    
    private fun parseLogicDefinition(logicJson: String): Map<String, Any> {
        return try {
            objectMapper.readValue(logicJson)
        } catch (e: Exception) {
            throw AtomExecutionException("Invalid logic definition JSON: ${e.message}")
        }
    }
    
    private fun parseInputParameters(inputParamsJson: String): Map<String, Any> {
        return try {
            objectMapper.readValue(inputParamsJson)
        } catch (e: Exception) {
            throw AtomExecutionException("Invalid input parameters JSON: ${e.message}")
        }
    }
    
    private fun parseTestCases(testCasesJson: String?): List<AtomTestCase> {
        if (testCasesJson.isNullOrBlank()) return emptyList()
        
        return try {
            objectMapper.readValue(testCasesJson)
        } catch (e: Exception) {
            logger.warn("Invalid test cases JSON: ${e.message}")
            emptyList()
        }
    }
    
    private fun runTestCase(atom: EligibilityAtom, testCase: AtomTestCase): TestCaseResult {
        return try {
            val result = executeAtom(atom, testCase.input)
            val success = if (result.success) {
                result.result == testCase.expected
            } else false
            
            TestCaseResult(
                testName = testCase.name,
                success = success,
                expected = testCase.expected,
                actual = result.result,
                error = if (!result.success) result.error else null
            )
        } catch (e: Exception) {
            TestCaseResult(
                testName = testCase.name,
                success = false,
                expected = testCase.expected,
                actual = null,
                error = e.message
            )
        }
    }
    
    private fun substituteTemplateParameters(
        template: Map<String, Any>, 
        input: Map<String, Any>
    ): Map<String, Any> {
        // TODO: Implement template parameter substitution
        return template
    }
    
    private fun generateCacheKey(atom: EligibilityAtom, input: Map<String, Any>): String {
        val tenantId = tenantContext.getTenantId()
        val inputHash = input.toString().hashCode()
        return "atom:${tenantId}:${atom.code}:${atom.version}:${inputHash}"
    }
    
    private fun getCachedResult(cacheKey: String): AtomExecutionResult? {
        return try {
            cacheManager.getCache(CACHE_NAME)?.get(cacheKey, AtomExecutionResult::class.java)
        } catch (e: Exception) {
            logger.debug("Cache retrieval failed for key '{}': {}", cacheKey, e.message)
            null
        }
    }
    
    private fun cacheResult(cacheKey: String, result: AtomExecutionResult, ttlSeconds: Long) {
        try {
            cacheManager.getCache(CACHE_NAME)?.put(cacheKey, result)
            logger.debug("Cached result for key '{}' with TTL {}s", cacheKey, ttlSeconds)
        } catch (e: Exception) {
            logger.warn("Failed to cache result for key '{}': {}", cacheKey, e.message)
        }
    }
    
    private fun updateAtomStatistics(atom: EligibilityAtom, result: AtomExecutionResult) {
        try {
            atom.updateExecutionStats(result.executionTimeMs, result.success)
            // Note: This would typically save to database via repository
            logger.debug("Updated statistics for atom '{}'", atom.code)
        } catch (e: Exception) {
            logger.warn("Failed to update statistics for atom '{}': {}", atom.code, e.message)
        }
    }
}

// =============================================================================
// DATA CLASSES
// =============================================================================

data class AtomExecutionContext(
    val atom: EligibilityAtom,
    val input: Map<String, Any>,
    val dependencyResults: Map<String, Any>,
    val tenantId: String,
    val requestId: String,
    val executionTime: LocalDateTime
)

data class AtomTestCase(
    val name: String,
    val input: Map<String, Any>,
    val expected: Any,
    val description: String? = null
)