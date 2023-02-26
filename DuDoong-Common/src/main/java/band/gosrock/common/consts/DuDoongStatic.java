package band.gosrock.common.consts;

public class DuDoongStatic {
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String WITHDRAW_PREFIX = "DELETED:";
    public static final String TOKEN_ROLE = "role";
    public static final String TOKEN_TYPE = "type";
    public static final String TOKEN_ISSUER = "DuDoong";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String KR_YES = "예";
    public static final String KR_NO = "아니요";

    public static final int MILLI_TO_SECOND = 1000;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_SERVER = 500;

    public static final Long NO_START_NUMBER = 1000000L;
    public static final Long MINIMUM_PAYMENT_WON = 1000L;
    public static final Long ZERO = 0L;

    public static final String assetDomain = "https://asset.dudoong.com/";

    public static final String KAKAO_OAUTH_QUERY_STRING =
            "/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code";

    public static final String[] SwaggerPatterns = {
        "/swagger-resources/**", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs",
    };
}
