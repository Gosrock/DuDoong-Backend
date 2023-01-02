package band.gosrock.api.auth.service;


import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.common.annotation.Helper;
import band.gosrock.common.jwt.JwtTokenProvider;
import band.gosrock.domain.common.dto.ProfileViewDto;
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor;
import band.gosrock.domain.domains.user.domain.RefreshTokenEntity;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;

@Helper
@RequiredArgsConstructor
public class TokenGenerateHelper {

    private final JwtTokenProvider jwtTokenProvider;

    private final RefreshTokenAdaptor refreshTokenAdaptor;

    public TokenAndUserResponse execute(User user) {
        String newAccessToken =
                jwtTokenProvider.generateAccessToken(
                        user.getId(), user.getAccountRole().getValue());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        RefreshTokenEntity newRefreshTokenEntity =
                RefreshTokenEntity.builder()
                        .refreshToken(newRefreshToken)
                        .id(user.getId())
                        .ttl(jwtTokenProvider.getRefreshTokenTTlSecond())
                        .build();
        refreshTokenAdaptor.save(newRefreshTokenEntity);

        return TokenAndUserResponse.builder()
                .userProfile(ProfileViewDto.from(user))
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
