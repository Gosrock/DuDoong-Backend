package band.gosrock.api.auth.service;


import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.common.jwt.JwtTokenProvider;
import band.gosrock.domain.common.dto.ProfileViewDto;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshUseCase {
    private final UserAdaptor userAdaptor;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenAndUserResponse execute(String refreshToken) {
        Long refreshUserId = jwtTokenProvider.parseRefreshToken(refreshToken);

        User user = userAdaptor.queryUser(refreshUserId);

        String newAccessToken =
                jwtTokenProvider.generateAccessToken(
                        user.getId(), user.getAccountRole().getValue());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return TokenAndUserResponse.builder()
                .userProfile(ProfileViewDto.from(user))
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
