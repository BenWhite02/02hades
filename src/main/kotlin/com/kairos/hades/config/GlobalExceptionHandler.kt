// =============================================================================
// File: src/main/kotlin/com/kairos/hades/config/GlobalExceptionHandler.kt
// 🔥 HADES Global Exception Handler
// Author: Sankhadeep Banerjee
// Centralized exception handling for REST API with structured error responses
// =============================================================================

package com.kairos.hades.config

import com.kairos.hades.controller.ApiResponse
import com.kairos.hades.exception.*
import com.kairos.hades.multitenancy.TenantContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import jakarta.validation.ConstraintViolationException
import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

/**
 * Global exception handler for all REST API endpoints
 * Provides consistent error response format and logging
 */
@RestControllerAdvice
class GlobalExceptionHandler @Autowired constructor(
    private val tenantContext: TenantContext
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
    
    // ==========================================================================
    // HADES SPECIFIC EXCEPTIONS
    // ==========================================================================
    
    @ExceptionHandler(HadesException::class)
    fun handleHadesException(
        ex: HadesException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Hades exception [{}]: {}", ex.errorCode, ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = ex.errorCode,
            message = ex.message ?: "An error occurred",
            status = ex.httpStatus.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId()
        )
        
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }
    
    @ExceptionHandler(AtomValidationException::class)
    fun handleAtomValidationException(
        ex: AtomValidationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Atom validation failed: {}", ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = ex.errorCode,
            message = ex.message ?: "Atom validation failed",
            status = ex.httpStatus.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = if (ex.validationErrors.isNotEmpty()) {
                mapOf("validationErrors" to ex.validationErrors)
            } else null
        )
        
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }
    
    @ExceptionHandler(AtomExecutionException::class)
    fun handleAtomExecutionException(
        ex: AtomExecutionException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Atom execution failed [{}]: {}", ex.atomCode, ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = ex.errorCode,
            message = ex.message ?: "Atom execution failed",
            status = ex.httpStatus.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = mapOf(
                "atomCode" to (ex.atomCode ?: "unknown"),
                "executionTimeMs" to (ex.executionTimeMs ?: 0)
            )
        )
        
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }
    
    @ExceptionHandler(AtomExecutionTimeoutException::class)
    fun handleAtomExecutionTimeoutException(
        ex: AtomExecutionTimeoutException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Atom execution timeout [{}]: {}ms", ex.atomCode, ex.timeoutMs, ex)
        
        val errorResponse = ErrorResponse(
            error = ex.errorCode,
            message = ex.message ?: "Atom execution timed out",
            status = ex.httpStatus.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = mapOf(
                "atomCode" to (ex.atomCode ?: "unknown"),
                "timeoutMs" to ex.timeoutMs
            )
        )
        
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }
    
    @ExceptionHandler(AtomDependencyException::class)
    fun handleAtomDependencyException(
        ex: AtomDependencyException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Atom dependency error: {}", ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = ex.errorCode,
            message = ex.message ?: "Atom dependency error",
            status = ex.httpStatus.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = if (ex.missingDependencies.isNotEmpty()) {
                mapOf("missingDependencies" to ex.missingDependencies)
            } else null
        )
        
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }
    
    @ExceptionHandler(TenantLimitExceededException::class)
    fun handleTenantLimitExceededException(
        ex: TenantLimitExceededException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Tenant limit exceeded [{}]: {} > {}", 
            ex.limitType, ex.currentValue, ex.maxValue)
        
        val errorResponse = ErrorResponse(
            error = ex.errorCode,
            message = ex.message ?: "Tenant limit exceeded",
            status = ex.httpStatus.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = mapOf(
                "limitType" to ex.limitType,
                "currentValue" to ex.currentValue,
                "maxValue" to ex.maxValue
            )
        )
        
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }
    
    // ==========================================================================
    // VALIDATION EXCEPTIONS
    // ==========================================================================
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Validation failed: {}", ex.message)
        
        val fieldErrors = ex.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }
        
        val errorResponse = ErrorResponse(
            error = "VALIDATION_FAILED",
            message = "Request validation failed",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = mapOf("fieldErrors" to fieldErrors)
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Constraint violation: {}", ex.message)
        
        val violations = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to violation.message
        }
        
        val errorResponse = ErrorResponse(
            error = "CONSTRAINT_VIOLATION",
            message = "Constraint validation failed",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = mapOf("violations" to violations)
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Malformed JSON request: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "MALFORMED_JSON",
            message = "Malformed JSON in request body",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId()
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameterException(
        ex: MissingServletRequestParameterException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Missing required parameter: {}", ex.parameterName)
        
        val errorResponse = ErrorResponse(
            error = "MISSING_PARAMETER",
            message = "Required parameter '${ex.parameterName}' is missing",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = mapOf("parameterName" to ex.parameterName)
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Type mismatch for parameter {}: {}", ex.name, ex.message)
        
        val errorResponse = ErrorResponse(
            error = "TYPE_MISMATCH",
            message = "Invalid type for parameter '${ex.name}'",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId(),
            details = mapOf(
                "parameterName" to ex.name,
                "expectedType" to (ex.requiredType?.simpleName ?: "unknown"),
                "providedValue" to (ex.value?.toString() ?: "null")
            )
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    // ==========================================================================
    // DATA ACCESS EXCEPTIONS
    // ==========================================================================
    
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Data integrity violation: {}", ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = "DATA_INTEGRITY_VIOLATION",
            message = "Data integrity constraint violated",
            status = HttpStatus.CONFLICT.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId()
        )
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }
    
    // ==========================================================================
    // TIMEOUT EXCEPTIONS
    // ==========================================================================
    
    @ExceptionHandler(TimeoutException::class)
    fun handleTimeoutException(
        ex: TimeoutException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Request timeout: {}", ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = "REQUEST_TIMEOUT",
            message = "Request timed out",
            status = HttpStatus.REQUEST_TIMEOUT.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId()
        )
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse)
    }
    
    // ==========================================================================
    // GENERIC EXCEPTIONS
    // ==========================================================================
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Illegal argument: {}", ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = "ILLEGAL_ARGUMENT",
            message = ex.message ?: "Invalid argument provided",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId()
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Illegal state: {}", ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = "ILLEGAL_STATE",
            message = ex.message ?: "Invalid operation state",
            status = HttpStatus.CONFLICT.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId()
        )
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: {}", ex.message, ex)
        
        val errorResponse = ErrorResponse(
            error = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred",
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            timestamp = LocalDateTime.now(),
            path = getRequestPath(request),
            tenantId = getTenantId(),
            requestId = getRequestId()
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
    
    // ==========================================================================
    // HELPER METHODS
    // ==========================================================================
    
    private fun getRequestPath(request: WebRequest): String {
        return request.getDescription(false).removePrefix("uri=")
    }
    
    private fun getTenantId(): String? {
        return try {
            tenantContext.getTenantId()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getRequestId(): String? {
        return try {
            tenantContext.getRequestId()
        } catch (e: Exception) {
            null
        }
    }
}

// =============================================================================
// ERROR RESPONSE DATA CLASS
// =============================================================================

/**
 * Standardized error response structure
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val status: Int,
    val timestamp: LocalDateTime,
    val path: String,
    val tenantId: String? = null,
    val requestId: String? = null,
    val details: Map<String, Any>? = null
) {
    /**
     * Convert to ApiResponse format for consistency
     */
    fun toApiResponse(): ApiResponse<Unit> {
        return ApiResponse.error(message)
    }
}