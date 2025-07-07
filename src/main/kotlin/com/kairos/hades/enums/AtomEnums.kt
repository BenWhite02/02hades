// =============================================================================
// File: src/main/kotlin/com/kairos/hades/enums/AtomEnums.kt
// 🔥 HADES Atom Enumerations
// Author: Sankhadeep Banerjee
// Defines all enums used by the EligibilityAtoms framework
// =============================================================================

package com.kairos.hades.enums

/**
 * Categories for EligibilityAtoms
 * Used to organize atoms by their primary purpose
 */
enum class AtomCategory(
    val displayName: String,
    val description: String,
    val icon: String
) {
    DEMOGRAPHIC("Demographic", "Age, gender, income, education, occupation-based rules", "👥"),
    BEHAVIORAL("Behavioral", "Purchase history, engagement patterns, activity-based rules", "🎯"),
    GEOGRAPHIC("Geographic", "Location, region, timezone, proximity-based rules", "🌍"),
    TEMPORAL("Temporal", "Time-based rules, scheduling, seasonal patterns", "⏰"),
    CONTEXTUAL("Contextual", "Device, channel, session, environment-based rules", "🌟"),
    PREDICTIVE("Predictive", "ML-powered predictions, propensity models, risk scores", "🔮"),
    TRANSACTIONAL("Transactional", "Purchase value, frequency, payment method rules", "💳"),
    ENGAGEMENT("Engagement", "Email opens, clicks, social media interaction rules", "📧"),
    LIFECYCLE("Lifecycle", "Customer lifecycle stage, tenure-based rules", "🔄"),
    RISK("Risk", "Fraud detection, compliance, security-based rules", "🛡️"),
    CUSTOM("Custom", "User-defined custom logic and business rules", "⚙️")
}

/**
 * Types of EligibilityAtoms
 * Defines the complexity and structure of the atom
 */
enum class AtomType(
    val displayName: String,
    val description: String,
    val allowsChildren: Boolean
) {
    SIMPLE("Simple", "Single condition or calculation", false),
    COMPLEX("Complex", "Multiple conditions with logical operators", false),
    COMPOSITE("Composite", "Combines multiple atoms with logical operators", true),
    TEMPLATE("Template", "Reusable template that can be instantiated", false),
    MACHINE_LEARNING("Machine Learning", "ML model-based decision making", false)
}

/**
 * Status of EligibilityAtoms
 * Lifecycle management for atoms
 */
enum class AtomStatus(
    val displayName: String,
    val description: String,
    val isExecutable: Boolean,
    val color: String
) {
    DRAFT("Draft", "Being developed, not ready for use", false, "#6B7280"),
    TESTING("Testing", "Under testing, limited execution allowed", true, "#F59E0B"),
    ACTIVE("Active", "Live and available for execution", true, "#10B981"),
    DEPRECATED("Deprecated", "Marked for removal, discouraged use", true, "#EF4444"),
    ARCHIVED("Archived", "Retired, no longer executable", false, "#374151"),
    MAINTENANCE("Maintenance", "Temporarily disabled for maintenance", false, "#8B5CF6")
}

/**
 * Logical operators for atom composition
 */
enum class LogicalOperator(
    val displayName: String,
    val symbol: String,
    val description: String
) {
    AND("AND", "&&", "All conditions must be true"),
    OR("OR", "||", "At least one condition must be true"),
    NOT("NOT", "!", "Negates the condition"),
    XOR("XOR", "⊕", "Exactly one condition must be true"),
    NAND("NAND", "!&&", "Not all conditions are true"),
    NOR("NOR", "!||", "None of the conditions are true")
}

/**
 * Comparison operators for conditions
 */
enum class ComparisonOperator(
    val displayName: String,
    val symbol: String,
    val description: String,
    val dataTypes: Set<String>
) {
    EQUALS("Equals", "==", "Values are exactly equal", setOf("string", "number", "boolean", "date")),
    NOT_EQUALS("Not Equals", "!=", "Values are not equal", setOf("string", "number", "boolean", "date")),
    GREATER_THAN("Greater Than", ">", "Left value is greater than right", setOf("number", "date")),
    GREATER_THAN_OR_EQUAL("Greater Than or Equal", ">=", "Left value is greater than or equal to right", setOf("number", "date")),
    LESS_THAN("Less Than", "<", "Left value is less than right", setOf("number", "date")),
    LESS_THAN_OR_EQUAL("Less Than or Equal", "<=", "Left value is less than or equal to right", setOf("number", "date")),
    CONTAINS("Contains", "contains", "String contains substring", setOf("string")),
    NOT_CONTAINS("Does Not Contain", "!contains", "String does not contain substring", setOf("string")),
    STARTS_WITH("Starts With", "startsWith", "String starts with prefix", setOf("string")),
    ENDS_WITH("Ends With", "endsWith", "String ends with suffix", setOf("string")),
    IN("In", "in", "Value is in list", setOf("string", "number")),
    NOT_IN("Not In", "!in", "Value is not in list", setOf("string", "number")),
    BETWEEN("Between", "between", "Value is between two values (inclusive)", setOf("number", "date")),
    NOT_BETWEEN("Not Between", "!between", "Value is not between two values", setOf("number", "date")),
    IS_NULL("Is Null", "isNull", "Value is null or empty", setOf("string", "number", "boolean", "date")),
    IS_NOT_NULL("Is Not Null", "isNotNull", "Value is not null or empty", setOf("string", "number", "boolean", "date")),
    MATCHES("Matches Pattern", "matches", "String matches regular expression", setOf("string")),
    NOT_MATCHES("Does Not Match Pattern", "!matches", "String does not match regular expression", setOf("string"))
}

/**
 * Data types supported by atoms
 */
enum class DataType(
    val displayName: String,
    val kotlinType: String,
    val description: String,
    val defaultValue: String
) {
    STRING("String", "String", "Text values", "\"\""),
    INTEGER("Integer", "Int", "Whole numbers", "0"),
    LONG("Long", "Long", "Large whole numbers", "0L"),
    DOUBLE("Double", "Double", "Decimal numbers", "0.0"),
    BOOLEAN("Boolean", "Boolean", "True/false values", "false"),
    DATE("Date", "LocalDate", "Date values", "LocalDate.now()"),
    DATETIME("DateTime", "LocalDateTime", "Date and time values", "LocalDateTime.now()"),
    TIME("Time", "LocalTime", "Time values", "LocalTime.now()"),
    LIST("List", "List<Any>", "List of values", "emptyList()"),
    MAP("Map", "Map<String, Any>", "Key-value pairs", "emptyMap()"),
    JSON("JSON", "String", "JSON formatted string", "\"{}\"")
}

/**
 * Execution priority levels
 */
enum class ExecutionPriority(
    val displayName: String,
    val level: Int,
    val description: String
) {
    CRITICAL("Critical", 10, "Must execute first, system critical"),
    HIGH("High", 8, "High priority execution"),
    NORMAL("Normal", 5, "Standard priority"),
    LOW("Low", 3, "Low priority, can be delayed"),
    BACKGROUND("Background", 1, "Execute when resources available")
}

/**
 * Cache strategies for atom results
 */
enum class CacheStrategy(
    val displayName: String,
    val description: String
) {
    NO_CACHE("No Cache", "Results are not cached"),
    MEMORY_ONLY("Memory Only", "Cache in application memory only"),
    REDIS_ONLY("Redis Only", "Cache in Redis only"),
    MULTI_LEVEL("Multi-Level", "Cache in both memory and Redis"),
    DATABASE("Database", "Cache results in database"),
    HYBRID("Hybrid", "Intelligent caching based on usage patterns")
}

/**
 * Atom execution modes
 */
enum class ExecutionMode(
    val displayName: String,
    val description: String,
    val isAsync: Boolean
) {
    SYNCHRONOUS("Synchronous", "Execute and wait for result", false),
    ASYNCHRONOUS("Asynchronous", "Execute in background", true),
    BATCH("Batch", "Execute as part of batch job", true),
    STREAMING("Streaming", "Execute on streaming data", true),
    SCHEDULED("Scheduled", "Execute on schedule", true)
}

/**
 * Performance metrics categories
 */
enum class MetricCategory(
    val displayName: String,
    val description: String,
    val unit: String
) {
    EXECUTION_TIME("Execution Time", "Time taken to execute atom", "milliseconds"),
    THROUGHPUT("Throughput", "Number of executions per second", "ops/sec"),
    SUCCESS_RATE("Success Rate", "Percentage of successful executions", "percentage"),
    ERROR_RATE("Error Rate", "Percentage of failed executions", "percentage"),
    CACHE_HIT_RATE("Cache Hit Rate", "Percentage of cache hits", "percentage"),
    MEMORY_USAGE("Memory Usage", "Memory consumed during execution", "bytes"),
    CPU_USAGE("CPU Usage", "CPU time consumed", "milliseconds")
}

/**
 * Validation rule types
 */
enum class ValidationRuleType(
    val displayName: String,
    val description: String
) {
    REQUIRED("Required", "Field must have a value"),
    MIN_LENGTH("Minimum Length", "String must have minimum length"),
    MAX_LENGTH("Maximum Length", "String must not exceed maximum length"),
    MIN_VALUE("Minimum Value", "Number must be at least minimum value"),
    MAX_VALUE("Maximum Value", "Number must not exceed maximum value"),
    PATTERN("Pattern", "String must match regular expression"),
    EMAIL("Email", "String must be valid email format"),
    URL("URL", "String must be valid URL format"),
    DATE_RANGE("Date Range", "Date must be within specified range"),
    CUSTOM("Custom", "Custom validation logic")
}

/**
 * Error types for atom execution
 */
enum class AtomErrorType(
    val displayName: String,
    val description: String,
    val isRetryable: Boolean
) {
    VALIDATION_ERROR("Validation Error", "Input validation failed", false),
    EXECUTION_ERROR("Execution Error", "Error during atom execution", true),
    DEPENDENCY_ERROR("Dependency Error", "Required dependency not available", true),
    TIMEOUT_ERROR("Timeout Error", "Execution exceeded time limit", true),
    CACHE_ERROR("Cache Error", "Cache operation failed", true),
    DATA_ERROR("Data Error", "Data access or processing error", true),
    CONFIGURATION_ERROR("Configuration Error", "Atom configuration invalid", false),
    PERMISSION_ERROR("Permission Error", "Insufficient permissions", false),
    RESOURCE_ERROR("Resource Error", "System resources unavailable", true),
    UNKNOWN_ERROR("Unknown Error", "Unexpected error occurred", true)
}