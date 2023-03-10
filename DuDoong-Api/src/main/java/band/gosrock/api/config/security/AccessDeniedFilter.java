package band.gosrock.api.config.security;

import static band.gosrock.common.consts.DuDoongStatic.SwaggerPatterns;

import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.dto.ErrorResponse;
import band.gosrock.common.exception.BaseErrorCode;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Component
public class AccessDeniedFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    private AuthenticationTrustResolver authenticationTrustResolver =
            new AuthenticationTrustResolverImpl();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return PatternMatchUtils.simpleMatch(SwaggerPatterns, servletPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (DuDoongCodeException e) {
            responseToClient(
                    response,
                    getErrorResponse(e.getErrorCode(), request.getRequestURL().toString()));
        } catch (AccessDeniedException e) {
            // ?????? ????????? ?????? ( ?????? ?????? ????????? Role ????????? ??????????????? )
            // basic authentication ????????????
            //  ExceptionTranslateFilter ????????????
            //  this.authenticationEntryPoint.commence(request, response, reason); ???????????? ??????????????????.
            //            Authentication authentication =
            // SecurityContextHolder.getContext().getAuthentication();
            //            boolean isAnonymous =
            // this.authenticationTrustResolver.isAnonymous(authentication);
            //            // ExceptionTranslateFilter ?????? ?????? ??????
            //            // ???????????????.. ????????? ????????? ?????????????????? ???????????????!
            //            if (isAnonymous) {
            //                throw e;
            //            }
            // ?????? ??????????????? Access denied exception ???????????? ( jwt ????????? ????????? )
            // ?????? ??????????????? ?????????.
            ErrorResponse access_denied =
                    new ErrorResponse(
                            GlobalErrorCode.ACCESS_TOKEN_NOT_EXIST.getErrorReason(),
                            request.getRequestURL().toString());
            responseToClient(response, access_denied);
        }
    }

    private ErrorResponse getErrorResponse(BaseErrorCode errorCode, String path) {
        ErrorReason errorReason = errorCode.getErrorReason();
        return new ErrorResponse(
                errorReason.getStatus(), errorReason.getCode(), errorReason.getReason(), path);
    }

    private void responseToClient(HttpServletResponse response, ErrorResponse errorResponse)
            throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorResponse.getStatus());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
