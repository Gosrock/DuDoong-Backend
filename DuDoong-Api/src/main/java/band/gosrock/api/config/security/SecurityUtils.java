package band.gosrock.api.config.security;


import band.gosrock.common.exception.SecurityContextNotFoundException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;

public class SecurityUtils {
    private static SimpleGrantedAuthority anonymous = new SimpleGrantedAuthority("ROLE_ANONYMOUS");
    private static SimpleGrantedAuthority swagger = new SimpleGrantedAuthority("ROLE_SWAGGER");

    private static List<SimpleGrantedAuthority> notUserAuthority = List.of(anonymous, swagger);

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw SecurityContextNotFoundException.EXCEPTION;
        }

        if (authentication.isAuthenticated()
                && !CollectionUtils.containsAny(
                        authentication.getAuthorities(), notUserAuthority)) {
            return Long.valueOf(authentication.getName());
        }
        // 스웨거 유저일시 익명 유저 취급
        // 익명유저시 userId 0 반환
        return 0L;
    }
}
