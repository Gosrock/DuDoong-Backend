package band.gosrock.api.config.security;


import band.gosrock.common.dto.ErrorResponse;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Component
public class AccessDeniedFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

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
            ErrorResponse access_denied =
                    new ErrorResponse(
                            403,
                            "Access Denied",
                            "check if accessToken exist",
                            request.getRequestURL().toString());
            responseToClient(response, access_denied);

        } finally {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
    }

    private ErrorResponse getErrorResponse(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                errorCode.getStatus(), errorCode.getCode(), errorCode.getReason(), path);
    }

    private void responseToClient(HttpServletResponse response, ErrorResponse errorResponse)
            throws IOException {
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
