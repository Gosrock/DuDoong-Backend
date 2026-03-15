package band.gosrock.api.config.security

import band.gosrock.common.dto.ErrorResponse
import band.gosrock.common.exception.BaseErrorCode
import band.gosrock.common.exception.DuDoongCodeException
import com.fasterxml.jackson.databind.ObjectMapper
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtExceptionFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

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
        }
    }

    private fun getErrorResponse(errorCode: BaseErrorCode, path: String): ErrorResponse =
        ErrorResponse(errorCode.getErrorReason(), path)

    private fun responseToClient(response: HttpServletResponse, errorResponse: ErrorResponse) {
        response.characterEncoding = "UTF-8"
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = errorResponse.status
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
