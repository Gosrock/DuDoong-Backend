package band.gosrock.api.config.rateLimit

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.api.slack.sender.SlackThrottleErrorSender
import band.gosrock.common.dto.ErrorResponse
import band.gosrock.common.exception.GlobalErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.WebUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val log = LoggerFactory.getLogger(ThrottlingInterceptor::class.java)

@Component
class ThrottlingInterceptor(
    private val userRateLimiter: UserRateLimiter,
    private val ipRateLimiter: IPRateLimiter,
    private val objectMapper: ObjectMapper,
    private val slackThrottleErrorSender: SlackThrottleErrorSender,
) : HandlerInterceptor {

    @Value("\${acl.whiteList}")
    private lateinit var aclWhiteList: List<String>

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val userId = SecurityUtils.getCurrentUserId()
        // 프록시 헤더는 신뢰하는 프록시(server.tomcat.remoteip.internal-proxies)가 붙인 값만 반영된 주소다 (#764).
        // 클라이언트가 보낸 Forwarded·X-Forwarded-For 값으로는 바뀌지 않는다
        val remoteAddr = request.remoteAddr
        log.info("remoteAddr : $remoteAddr")

        // next js ssr 대응
        if (aclWhiteList.contains(remoteAddr)) {
            log.info("white List pass$remoteAddr")
            return true
        }

        val bucket = if (userId == 0L) {
            // 익명 유저 ip 기반처리
            ipRateLimiter.resolveBucket(remoteAddr)
        } else {
            // 비 익명 유저 유저 아이디 기반 처리
            userRateLimiter.resolveBucket(userId.toString())
        }

        val availableTokens = bucket.availableTokens
        log.info("$userId : $availableTokens")

        if (bucket.tryConsume(1)) {
            return true
        }

        // 슬랙 알림 메시지 발송 (키별 분당 1회, 비동기). 알림 실패는 429 응답에 영향을 주지 않는다
        // limit is exceeded
        try {
            WebUtils.getNativeRequest(request, ContentCachingRequestWrapper::class.java)
                ?.let { slackThrottleErrorSender.execute(it, userId) }
        } catch (e: Exception) {
            log.warn("rate limit Slack 알림 실패: {}", e.toString())
        }
        responseTooManyRequestError(request, response)

        return false
    }

    private fun responseTooManyRequestError(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val errorResponse = ErrorResponse(
            GlobalErrorCode.TOO_MANY_REQUEST.getErrorReason(),
            request.requestURL.toString(),
        )
        response.characterEncoding = "UTF-8"
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = errorResponse.status
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
