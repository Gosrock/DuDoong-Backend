package band.gosrock.api.config.rateLimit;

import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.exception.TooManyRequestException;
import io.github.bucket4j.Bucket;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThrottlingInterceptor implements HandlerInterceptor {

    private final UserRateLimiter userRateLimiter;
    private final IPRateLimiter ipRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        Long userId = SecurityUtils.getCurrentUserId();
        Bucket bucket;
        if(userId == 0L){
            // 익명 유저 ip 기반처리
            String remoteAddr = request.getRemoteAddr();
            bucket = ipRateLimiter.resolveBucket(remoteAddr);
        }else{
            // 비 익명 유저 유저 아이디 기반 처리
            bucket = userRateLimiter.resolveBucket(userId.toString());
        }

        long availableTokens = bucket.getAvailableTokens();
        log.info(userId + " : " + availableTokens);

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            // 슬랙 알림 메시지 발송.
            // limit is exceeded
            throw TooManyRequestException.EXCEPTION;
        }
    }

}