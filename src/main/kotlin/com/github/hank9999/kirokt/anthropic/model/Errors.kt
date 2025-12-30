package com.github.hank9999.kirokt.anthropic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ 错误类型 ============

/**
 * API 错误类型
 */
enum class ErrorType(val value: String, val httpStatus: Int) {
    INVALID_REQUEST_ERROR("invalid_request_error", 400),
    AUTHENTICATION_ERROR("authentication_error", 401),
    PERMISSION_ERROR("permission_error", 403),
    NOT_FOUND_ERROR("not_found_error", 404),
    REQUEST_TOO_LARGE("request_too_large", 413),
    RATE_LIMIT_ERROR("rate_limit_error", 429),
    API_ERROR("api_error", 500),
    OVERLOADED_ERROR("overloaded_error", 529);

    companion object {
        fun fromValue(value: String): ErrorType? =
            entries.find { it.value == value }

        fun fromHttpStatus(status: Int): ErrorType =
            entries.find { it.httpStatus == status } ?: API_ERROR
    }
}

/**
 * API 错误详情
 */
@Serializable
data class ApiError(
    val type: String,
    val message: String
) {
    companion object {
        fun invalidRequest(message: String) = ApiError(
            type = ErrorType.INVALID_REQUEST_ERROR.value,
            message = message
        )

        fun authentication(message: String = "Invalid API key") = ApiError(
            type = ErrorType.AUTHENTICATION_ERROR.value,
            message = message
        )

        fun permission(message: String = "Permission denied") = ApiError(
            type = ErrorType.PERMISSION_ERROR.value,
            message = message
        )

        fun notFound(message: String = "The requested resource could not be found") = ApiError(
            type = ErrorType.NOT_FOUND_ERROR.value,
            message = message
        )

        fun requestTooLarge(message: String = "Request size exceeds limit") = ApiError(
            type = ErrorType.REQUEST_TOO_LARGE.value,
            message = message
        )

        fun rateLimit(message: String = "Rate limit exceeded") = ApiError(
            type = ErrorType.RATE_LIMIT_ERROR.value,
            message = message
        )

        fun apiError(message: String = "Internal server error") = ApiError(
            type = ErrorType.API_ERROR.value,
            message = message
        )

        fun overloaded(message: String = "API is temporarily overloaded") = ApiError(
            type = ErrorType.OVERLOADED_ERROR.value,
            message = message
        )
    }
}

/**
 * 错误响应
 */
@Serializable
data class ErrorResponse(
    val type: String = "error",
    val error: ApiError,
    @SerialName("request_id")
    val requestId: String? = null
) {
    companion object {
        fun invalidRequest(message: String, requestId: String? = null) = ErrorResponse(
            error = ApiError.invalidRequest(message),
            requestId = requestId
        )

        fun authentication(message: String = "Invalid API key", requestId: String? = null) = ErrorResponse(
            error = ApiError.authentication(message),
            requestId = requestId
        )

        fun permission(message: String = "Permission denied", requestId: String? = null) = ErrorResponse(
            error = ApiError.permission(message),
            requestId = requestId
        )

        fun notFound(message: String = "The requested resource could not be found", requestId: String? = null) = ErrorResponse(
            error = ApiError.notFound(message),
            requestId = requestId
        )

        fun requestTooLarge(message: String = "Request size exceeds limit", requestId: String? = null) = ErrorResponse(
            error = ApiError.requestTooLarge(message),
            requestId = requestId
        )

        fun rateLimit(message: String = "Rate limit exceeded", requestId: String? = null) = ErrorResponse(
            error = ApiError.rateLimit(message),
            requestId = requestId
        )

        fun apiError(message: String = "Internal server error", requestId: String? = null) = ErrorResponse(
            error = ApiError.apiError(message),
            requestId = requestId
        )

        fun overloaded(message: String = "API is temporarily overloaded", requestId: String? = null) = ErrorResponse(
            error = ApiError.overloaded(message),
            requestId = requestId
        )
    }
}
