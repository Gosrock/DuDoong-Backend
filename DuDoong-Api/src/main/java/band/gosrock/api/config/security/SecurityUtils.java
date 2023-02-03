package band.gosrock.api.config.security;


import band.gosrock.common.exception.SecurityContextNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    private static SimpleGrantedAuthority anonymous = new SimpleGrantedAuthority("ROLE_ANONYMOUS");

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw SecurityContextNotFoundException.EXCEPTION;
        }
        if (authentication.isAuthenticated()
                && !authentication.getAuthorities().contains(anonymous)) {
            return Long.valueOf(authentication.getName());
        }
        // 익명유저시 userId 0 반환
        return 0L;
    }
}
