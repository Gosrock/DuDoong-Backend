package band.gosrock.domain.domains.user.service;


import band.gosrock.common.annotation.DomainService;
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
    //    @RedissonLock(LockName = "registerUser", paramName = "oauthInfo", paramClassType =
    // OauthInfo.class)
    public User registerUser(Profile profile, OauthInfo oauthInfo) {
        User newUser = User.builder().profile(profile).oauthInfo(oauthInfo).build();
        userRepository.save(newUser);
        return newUser;
    }

    @Transactional
    public User upsertUser(Profile profile, OauthInfo oauthInfo) {
        return userRepository
                .findByOauthInfo(oauthInfo)
                .orElseGet(
                        () -> {
                            User newUser =
                                    User.builder().profile(profile).oauthInfo(oauthInfo).build();
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

    public User loginUser(OauthInfo oauthInfo) {
        User user = userAdaptor.queryUserByOauthInfo(oauthInfo);
        user.login();
        return user;
    }
}
