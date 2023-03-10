package band.gosrock.domain.domains.user.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.exception.AlreadySignUpUserException;
import band.gosrock.domain.domains.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@RequiredArgsConstructor
public class UserDomainService {
    private final UserRepository userRepository;
    private final UserAdaptor userAdaptor;

    @Transactional
    @RedissonLock(LockName = "유저등록", identifier = "oid", paramClassType = OauthInfo.class)
    public User registerUser(Profile profile, OauthInfo oauthInfo, Boolean marketingAgree) {
        validUserCanRegister(oauthInfo);
        User newUser =
                User.builder()
                        .profile(profile)
                        .marketingAgree(marketingAgree)
                        .oauthInfo(oauthInfo)
                        .build();
        userRepository.save(newUser);
        return newUser;
    }

    @Transactional
    @RedissonLock(LockName = "개발용회원가입", identifier = "oid", paramClassType = OauthInfo.class)
    public User upsertUser(Profile profile, OauthInfo oauthInfo) {
        return userRepository
                .findByOauthInfo(oauthInfo)
                .orElseGet(
                        () -> {
                            User newUser =
                                    User.builder()
                                            .profile(profile)
                                            .marketingAgree(Boolean.TRUE)
                                            .oauthInfo(oauthInfo)
                                            .build();
                            userRepository.save(newUser);
                            return newUser;
                        });
    }

    public Boolean checkUserCanRegister(OauthInfo oauthInfo) {
        return !userAdaptor.exist(oauthInfo);
    }

    public void validUserCanRegister(OauthInfo oauthInfo) {
        if (!checkUserCanRegister(oauthInfo)) throw AlreadySignUpUserException.EXCEPTION;
    }

    @Transactional
    public User loginUser(OauthInfo oauthInfo) {
        User user = userAdaptor.queryUserByOauthInfo(oauthInfo);
        user.login();
        return user;
    }

    @Transactional
    @RedissonLock(LockName = "유저탈퇴", identifier = "userId")
    public void withDrawUser(Long userId) {
        User user = userAdaptor.queryUser(userId);
        user.withDrawUser();
    }

    @Transactional
    public void toggleMarketAgree(Long currentUserId) {
        User user = userAdaptor.queryUser(currentUserId);
        user.toggleMarketingAgree();
    }

    @Transactional
    public void toggleMailAgree(Long currentUserId) {
        User user = userAdaptor.queryUser(currentUserId);
        user.toggleReceiveEmail();
    }
}
