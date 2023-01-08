package band.gosrock.api.auth.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class LogoutUseCase {
    private final RefreshTokenAdaptor refreshTokenAdaptor;

    public void execute() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        refreshTokenAdaptor.deleteByUserId(currentUserId);
    }
}
