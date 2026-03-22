package band.gosrock.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import java.util.UUID

@Component
class MdcFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(MdcFilter::class.java)

    companion object {
        private const val MAX_BODY_LOG_SIZE = 2048
        private val EXCLUDED_PATHS = listOf("/api/v1/auth", "/api/v1/payment")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request, MAX_BODY_LOG_SIZE)
        try {
            val traceId = request.getHeader("X-Trace-Id")
                ?: UUID.randomUUID().toString().replace("-", "").substring(0, 8)
            MDC.put("traceId", traceId)
            response.setHeader("X-Trace-Id", traceId)
            filterChain.doFilter(wrappedRequest, response)
        } finally {
            logRequest(wrappedRequest, response)
            MDC.clear()
        }
    }

    private fun logRequest(request: ContentCachingRequestWrapper, response: HttpServletResponse) {
        val uri = request.requestURI
        val method = request.method
        val status = response.status

        val body = if (shouldLogBody(uri, method)) {
            val content = request.contentAsByteArray
            if (content.isNotEmpty()) {
                String(content, Charsets.UTF_8).take(MAX_BODY_LOG_SIZE)
            } else {
                null
            }
        } else {
            "[FILTERED]"
        }

        if (body != null) {
            log.info("{} {} {} body={}", method, uri, status, body)
        } else {
            log.info("{} {} {}", method, uri, status)
        }
    }

    private fun shouldLogBody(uri: String, method: String): Boolean {
        if (method == "GET") return false
        return EXCLUDED_PATHS.none { uri.startsWith(it) }
    }
}
