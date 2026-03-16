package band.gosrock.api.config

import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Component
class HttpContentCacheFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val wrappingRequest = ContentCachingRequestWrapper(request)
        val wrappingResponse = ContentCachingResponseWrapper(response)

        chain.doFilter(wrappingRequest, wrappingResponse)

        wrappingResponse.copyBodyToResponse()
    }
}
