// =============================================================================
// File: src/main/kotlin/com/kairos/hades/exception/HadesExceptions.kt
// 🔥 HADES Exception Classes
// Author: Sankhadeep Banerjee
// Custom exception hierarchy for Hades application
// =============================================================================

package com.kairos.hades.exception

import org.springframework.http.HttpStatus

/**
 * Base exception class for all Hades-specific exceptions
 */
abstract class HadesException(
    message: String,
    cause: Throwable? = null,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val errorCode: String = "HADES_ERROR"
) : RuntimeException(message, cause)

// =============================================================================
// ATOM EXCEPTIONS
// =============================================================================

/**
 * Exception thrown when an atom is not found
 */
class AtomNotFoundException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.NOT_FOUND,
    errorCode = "ATOM_NOT_FOUND"
)

/**
 * Exception thrown when trying to create an atom that already exists
 */
class AtomAlreadyExistsException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.CONFLICT,
    errorCode = "ATOM_ALREADY_EXISTS"
)

/**
 * Exception thrown when atom validation fails
 */
class AtomValidationException(
    message: String,
    val validationErrors: List<String> = emptyList(),
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.BAD_REQUEST,
    errorCode = "ATOM_VALIDATION_FAILED"
)

/**
 * Exception thrown when atom execution fails
 */
class AtomExecutionException(
    message: String,
    val atomCode: String? = null,
    val executionTimeMs: Long? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    errorCode = "ATOM_EXECUTION_FAILED"
)

/**
 * Exception thrown when atom update is not allowed
 */
class AtomUpdateNotAllowedException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.CONFLICT,
    errorCode = "ATOM_UPDATE_NOT_ALLOWED"
)

/**
 * Exception thrown when atom deletion is not allowed
 */
class AtomDeletionNotAllowedException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.CONFLICT,
    errorCode = "ATOM_DELETION_NOT_ALLOWED"
)

/**
 * Exception thrown for atom lifecycle violations
 */
class AtomLifecycleException(
    message: String,
    val currentStatus: String? = null,
    val targetStatus: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.CONFLICT,
    errorCode = "ATOM_LIFECYCLE_ERROR"
)

/**
 * Exception thrown when atom execution times out
 */
class AtomExecutionTimeoutException(
    message: String,
    val timeoutMs: Long,
    val atomCode: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.REQUEST_TIMEOUT,
    errorCode = "ATOM_EXECUTION_TIMEOUT"
)

/**
 * Exception thrown when atom dependencies are not met
 */
class AtomDependencyException(
    message: String,
    val missingDependencies: List<String> = emptyList(),
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
    errorCode = "ATOM_DEPENDENCY_ERROR"
)

// =============================================================================
// TENANT EXCEPTIONS
// =============================================================================

/**
 * Exception thrown when a tenant is not found
 */
class TenantNotFoundException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.NOT_FOUND,
    errorCode = "TENANT_NOT_FOUND"
)

/**
 * Exception thrown when trying to create a tenant that already exists
 */
class TenantAlreadyExistsException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.CONFLICT,
    errorCode = "TENANT_ALREADY_EXISTS"
)

/**
 * Exception thrown when tenant context is invalid or missing
 */
class InvalidTenantContextException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.BAD_REQUEST,
    errorCode = "INVALID_TENANT_CONTEXT"
)

/**
 * Exception thrown when tenant access is denied
 */
class TenantAccessDeniedException(
    message: String,
    val tenantId: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.FORBIDDEN,
    errorCode = "TENANT_ACCESS_DENIED"
)

/**
 * Exception thrown when tenant subscription limits are exceeded
 */
class TenantLimitExceededException(
    message: String,
    val limitType: String,
    val currentValue: Long,
    val maxValue: Long,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.PAYMENT_REQUIRED,
    errorCode = "TENANT_LIMIT_EXCEEDED"
)

/**
 * Exception thrown when tenant is suspended or inactive
 */
class TenantSuspendedException(
    message: String,
    val tenantId: String? = null,
    val suspensionReason: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.FORBIDDEN,
    errorCode = "TENANT_SUSPENDED"
)

// =============================================================================
// SECURITY EXCEPTIONS
// =============================================================================

/**
 * Exception thrown for authentication failures
 */
class AuthenticationException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.UNAUTHORIZED,
    errorCode = "AUTHENTICATION_FAILED"
)

/**
 * Exception thrown for authorization failures
 */
class AuthorizationException(
    message: String,
    val requiredRole: String? = null,
    val userRoles: Set<String>? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.FORBIDDEN,
    errorCode = "AUTHORIZATION_FAILED"
)

/**
 * Exception thrown for invalid JWT tokens
 */
class InvalidTokenException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.UNAUTHORIZED,
    errorCode = "INVALID_TOKEN"
)

/**
 * Exception thrown when token has expired
 */
class TokenExpiredException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.UNAUTHORIZED,
    errorCode = "TOKEN_EXPIRED"
)

// =============================================================================
// VALIDATION EXCEPTIONS
// =============================================================================

/**
 * Exception thrown for invalid request data
 */
class InvalidRequestException(
    message: String,
    val field: String? = null,
    val value: Any? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.BAD_REQUEST,
    errorCode = "INVALID_REQUEST"
)

/**
 * Exception thrown for missing required parameters
 */
class MissingParameterException(
    parameterName: String,
    cause: Throwable? = null
) : HadesException(
    message = "Required parameter '$parameterName' is missing",
    cause = cause,
    httpStatus = HttpStatus.BAD_REQUEST,
    errorCode = "MISSING_PARAMETER"
)

/**
 * Exception thrown for invalid parameter values
 */
class InvalidParameterException(
    parameterName: String,
    value: Any?,
    expectedType: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = "Invalid value for parameter '$parameterName': $value" + 
             (expectedType?.let { " (expected: $it)" } ?: ""),
    cause = cause,
    httpStatus = HttpStatus.BAD_REQUEST,
    errorCode = "INVALID_PARAMETER"
)

// =============================================================================
// RESOURCE EXCEPTIONS
// =============================================================================

/**
 * Exception thrown when a requested resource is not found
 */
class ResourceNotFoundException(
    resourceType: String,
    identifier: String,
    cause: Throwable? = null
) : HadesException(
    message = "$resourceType not found: $identifier",
    cause = cause,
    httpStatus = HttpStatus.NOT_FOUND,
    errorCode = "RESOURCE_NOT_FOUND"
)

/**
 * Exception thrown when a resource already exists
 */
class ResourceAlreadyExistsException(
    resourceType: String,
    identifier: String,
    cause: Throwable? = null
) : HadesException(
    message = "$resourceType already exists: $identifier",
    cause = cause,
    httpStatus = HttpStatus.CONFLICT,
    errorCode = "RESOURCE_ALREADY_EXISTS"
)

/**
 * Exception thrown when resource access is denied
 */
class ResourceAccessDeniedException(
    resourceType: String,
    identifier: String,
    action: String,
    cause: Throwable? = null
) : HadesException(
    message = "Access denied for $action on $resourceType: $identifier",
    cause = cause,
    httpStatus = HttpStatus.FORBIDDEN,
    errorCode = "RESOURCE_ACCESS_DENIED"
)

// =============================================================================
// SYSTEM EXCEPTIONS
// =============================================================================

/**
 * Exception thrown for configuration errors
 */
class ConfigurationException(
    message: String,
    val configurationKey: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    errorCode = "CONFIGURATION_ERROR"
)

/**
 * Exception thrown for service unavailable errors
 */
class ServiceUnavailableException(
    serviceName: String,
    cause: Throwable? = null
) : HadesException(
    message = "Service unavailable: $serviceName",
    cause = cause,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    errorCode = "SERVICE_UNAVAILABLE"
)

/**
 * Exception thrown for external service integration errors
 */
class ExternalServiceException(
    serviceName: String,
    operation: String,
    cause: Throwable? = null
) : HadesException(
    message = "External service error in $serviceName during $operation",
    cause = cause,
    httpStatus = HttpStatus.BAD_GATEWAY,
    errorCode = "EXTERNAL_SERVICE_ERROR"
)

/**
 * Exception thrown for rate limiting violations
 */
class RateLimitExceededException(
    message: String,
    val limit: Long,
    val windowSizeMs: Long,
    val retryAfterMs: Long? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.TOO_MANY_REQUESTS,
    errorCode = "RATE_LIMIT_EXCEEDED"
)

// =============================================================================
// DATA EXCEPTIONS
// =============================================================================

/**
 * Exception thrown for data access errors
 */
class DataAccessException(
    message: String,
    val operation: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    errorCode = "DATA_ACCESS_ERROR"
)

/**
 * Exception thrown for data integrity violations
 */
class DataIntegrityException(
    message: String,
    val constraint: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.CONFLICT,
    errorCode = "DATA_INTEGRITY_VIOLATION"
)

/**
 * Exception thrown for concurrent modification conflicts
 */
class ConcurrentModificationException(
    message: String,
    val resourceId: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.CONFLICT,
    errorCode = "CONCURRENT_MODIFICATION"
)

// =============================================================================
// CACHE EXCEPTIONS
// =============================================================================

/**
 * Exception thrown for cache-related errors
 */
class CacheException(
    message: String,
    val cacheKey: String? = null,
    val operation: String? = null,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    errorCode = "CACHE_ERROR"
)

/**
 * Exception thrown when cache is unavailable
 */
class CacheUnavailableException(
    message: String,
    cause: Throwable? = null
) : HadesException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    errorCode = "CACHE_UNAVAILABLE"
)