package band.gosrock.api.auth.service;


import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.api.auth.service.helper.TokenGenerateHelper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.common.jwt.JwtTokenProvider;
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.RefreshTokenEntity;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshUseCase {
    private final UserAdaptor userAdaptor;
    private final JwtTokenProvider jwtTokenProvider;

    private final UserDomainService userDomainService;
    private final RefreshTokenAdaptor refreshTokenAdaptor;

    private final TokenGenerateHelper tokenGenerateHelper;

    public TokenAndUserResponse execute(String refreshToken) {
        RefreshTokenEntity savedRefreshTokenEntity =
                refreshTokenAdaptor.queryRefreshToken(refreshToken);

        Long refreshUserId =
                jwtTokenProvider.parseRefreshToken(savedRefreshTokenEntity.getRefreshToken());

        User user = userAdaptor.queryUser(refreshUserId);
        // 리프레쉬 시에도 last로그인 정보 업데이트
        userDomainService.loginUser(user.getOauthInfo());
        return tokenGenerateHelper.execute(user);
    }
}
