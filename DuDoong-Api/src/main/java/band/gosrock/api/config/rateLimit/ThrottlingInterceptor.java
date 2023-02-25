package band.gosrock.api.config.rateLimit;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.slack.sender.SlackThrottleErrorSender;
import band.gosrock.common.dto.ErrorResponse;
import band.gosrock.common.exception.GlobalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThrottlingInterceptor implements HandlerInterceptor {

    private final UserRateLimiter userRateLimiter;
    private final IPRateLimiter ipRateLimiter;
    private final ObjectMapper objectMapper;

    private final SlackThrottleErrorSender slackThrottleErrorSender;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        Bucket bucket;
        if (userId == 0L) {
            // 익명 유저 ip 기반처리
            String remoteAddr = request.getRemoteAddr();
            bucket = ipRateLimiter.resolveBucket(remoteAddr);
        } else {
            // 비 익명 유저 유저 아이디 기반 처리
            bucket = userRateLimiter.resolveBucket(userId.toString());
        }

        long availableTokens = bucket.getAvailableTokens();
        log.info(userId + " : " + availableTokens);

        if (bucket.tryConsume(1)) {
            return true;
        }

        // 슬랙 알림 메시지 발송.
        // limit is exceeded
        ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
        slackThrottleErrorSender.execute(cachingRequest, userId);
        responseTooManyRequestError(request, response);

        return false;
    }

    private void responseTooManyRequestError(
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        ErrorResponse errorResponse =
                new ErrorResponse(
                        GlobalErrorCode.TOO_MANY_REQUEST.getErrorReason(),
                        request.getRequestURL().toString());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorResponse.getStatus());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
