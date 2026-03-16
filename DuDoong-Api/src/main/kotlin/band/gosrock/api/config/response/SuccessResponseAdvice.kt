package band.gosrock.api.config.response

import band.gosrock.common.dto.SuccessResponse
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import jakarta.servlet.http.HttpServletResponse

@RestControllerAdvice(basePackages = ["band.gosrock"])
class SuccessResponseAdvice : ResponseBodyAdvice<Any> {

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = true

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        val servletResponse: HttpServletResponse =
            (response as ServletServerHttpResponse).servletResponse

        val status = servletResponse.status
        val resolve = HttpStatus.resolve(status) ?: return body

        return if (resolve.is2xxSuccessful) {
            SuccessResponse(status, body)
        } else {
            body
        }
    }
}
