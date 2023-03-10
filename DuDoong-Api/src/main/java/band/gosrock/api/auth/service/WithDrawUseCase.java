package band.gosrock.api.auth.service;


import band.gosrock.api.auth.service.helper.KakaoOauthHelper;
import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class WithDrawUseCase {

    private final RefreshTokenAdaptor refreshTokenAdaptor;
    private final UserDomainService userDomainService;
    private final UserAdaptor userAdaptor;
    private final KakaoOauthHelper kakaoOauthHelper;

    @Transactional
    public void execute() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        refreshTokenAdaptor.deleteByUserId(currentUserId);
        User user = userAdaptor.queryUser(currentUserId);
        String oid = user.getOauthInfo().getOid();
        userDomainService.withDrawUser(currentUserId);
        kakaoOauthHelper.unlink(oid);
    }
}
