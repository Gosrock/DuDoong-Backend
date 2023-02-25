package band.gosrock.api.user.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class MarketingUserUseCase {

    private final UserUtils userUtils;

    private final UserDomainService userDomainService;

    public UserInfoVo execute() {
        User currentUser = userUtils.getCurrentUser();
        return currentUser.toUserInfoVo();
    }

    public UserInfoVo toggleMailAgree() {
        userDomainService.toggleMailAgree(userUtils.getCurrentUserId());
        return userUtils.getCurrentUser().toUserInfoVo();
    }

    public UserInfoVo toggleMarketAgree() {
        userDomainService.toggleMarketAgree(userUtils.getCurrentUserId());
        return userUtils.getCurrentUser().toUserInfoVo();
    }
}
