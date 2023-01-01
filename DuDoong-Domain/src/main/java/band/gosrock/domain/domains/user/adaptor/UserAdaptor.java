package band.gosrock.domain.domains.user.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserAdaptor {

    private final UserRepository userRepository;

    @Transactional
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
}
