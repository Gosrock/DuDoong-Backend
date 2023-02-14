package band.gosrock.api.user.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReadUserUseCase {

    private final UserUtils userUtils;

    public UserInfoVo execute() {
        User currentUser = userUtils.getCurrentUser();
        return currentUser.toUserInfoVo();
    }
}
