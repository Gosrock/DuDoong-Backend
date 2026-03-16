package band.gosrock.api.config.response

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.api.slack.sender.SlackInternalErrorSender
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.dto.ErrorResponse
import band.gosrock.common.exception.BaseErrorCode
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.common.exception.GlobalErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.UriComponentsBuilder
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException

private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

@RestControllerAdvice
class GlobalExceptionHandler(
    private val slackInternalErrorSender: SlackInternalErrorSender,
) : ResponseEntityExceptionHandler() {

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val servletWebRequest = request as ServletWebRequest
        val url = UriComponentsBuilder
            .fromHttpRequest(ServletServerHttpRequest(servletWebRequest.request))
            .build()
            .toUriString()
        val status = HttpStatus.valueOf(statusCode.value())
        val errorResponse = ErrorResponse(status.value(), status.name, ex.message ?: "", url)
        return super.handleExceptionInternal(ex, errorResponse, headers, statusCode, request)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors: List<FieldError> = ex.bindingResult.fieldErrors
        val servletWebRequest = request as ServletWebRequest
        val url = UriComponentsBuilder
            .fromHttpRequest(ServletServerHttpRequest(servletWebRequest.request))
            .build()
            .toUriString()
        val fieldAndErrorMessages = errors.associate { it.field to it.defaultMessage }
        val errorsToJsonString = ObjectMapper().writeValueAsString(fieldAndErrorMessages)
        val status = HttpStatus.valueOf(statusCode.value())
        val errorResponse = ErrorResponse(status.value(), status.name, errorsToJsonString, url)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(DuDoongCodeException::class)
    fun duDoongCodeExceptionHandler(
        e: DuDoongCodeException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val code: BaseErrorCode = e.errorCode
        val errorReason: ErrorReason = code.getErrorReason()
        val errorResponse = ErrorResponse(errorReason, request.requestURL.toString())
        return ResponseEntity.status(HttpStatus.valueOf(errorReason.status)).body(errorResponse)
    }

    /** Request Param Validation 예외 처리 */
    @ExceptionHandler(ConstraintViolationException::class)
    fun constraintViolationExceptionHandler(
        e: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val bindingErrors = mutableMapOf<String, Any?>()
        e.constraintViolations.forEach { constraintViolation ->
            val propertyPath = constraintViolation.propertyPath.toString().split(".")
            val path = propertyPath.drop(propertyPath.size - 1).firstOrNull()
            bindingErrors[path ?: ""] = constraintViolation.message
        }
        val errorReason = ErrorReason(
            status = 400,
            code = "BAD_REQUEST",
            reason = bindingErrors.toString(),
        )
        val errorResponse = ErrorResponse(errorReason, request.requestURL.toString())
        return ResponseEntity.status(HttpStatus.valueOf(errorReason.status)).body(errorResponse)
    }

    @ExceptionHandler(DuDoongDynamicException::class)
    fun duDoongDynamicExceptionHandler(
        e: DuDoongDynamicException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            e.status,
            e.code,
            e.reason,
            request.requestURL.toString(),
        )
        return ResponseEntity.status(HttpStatus.valueOf(e.status)).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val cachingRequest = request as ContentCachingRequestWrapper
        val userId = SecurityUtils.getCurrentUserId()
        val url = UriComponentsBuilder
            .fromHttpRequest(ServletServerHttpRequest(request))
            .build()
            .toUriString()

        log.error("INTERNAL_SERVER_ERROR", e)
        val internalServerError = GlobalErrorCode.INTERNAL_SERVER_ERROR
        val errorResponse = ErrorResponse(
            internalServerError.status,
            internalServerError.code,
            internalServerError.reason,
            url,
        )

        slackInternalErrorSender.execute(cachingRequest, e, userId)
        return ResponseEntity.status(HttpStatus.valueOf(internalServerError.status)).body(errorResponse)
    }
}
