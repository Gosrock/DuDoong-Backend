package band.gosrock.api.config.security

import band.gosrock.common.consts.DuDoongStatic.SwaggerPatterns
import band.gosrock.common.dto.ErrorResponse
import band.gosrock.common.exception.BaseErrorCode
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.exception.GlobalErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.util.PatternMatchUtils
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AccessDeniedFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val servletPath = request.servletPath
        return PatternMatchUtils.simpleMatch(SwaggerPatterns, servletPath)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: DuDoongCodeException) {
            responseToClient(
                response,
                getErrorResponse(e.errorCode, request.requestURL.toString())
            )
        } catch (e: AccessDeniedException) {
            val accessDenied = ErrorResponse(
                GlobalErrorCode.ACCESS_TOKEN_NOT_EXIST.getErrorReason(),
                request.requestURL.toString()
            )
            responseToClient(response, accessDenied)
        }
    }

    private fun getErrorResponse(errorCode: BaseErrorCode, path: String): ErrorResponse {
        val errorReason = errorCode.getErrorReason()
        return ErrorResponse(errorReason.status, errorReason.code, errorReason.reason, path)
    }

    private fun responseToClient(response: HttpServletResponse, errorResponse: ErrorResponse) {
        response.characterEncoding = "UTF-8"
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = errorResponse.status
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
