package band.gosrock.domain.domain.user.service;


import band.gosrock.domain.domain.user.domain.OauthInfo;
import band.gosrock.domain.domain.user.domain.Profile;
import band.gosrock.domain.domain.user.domain.User;
import band.gosrock.domain.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserDomainService {

    private final UserRepository userRepository;

    @Transactional
    public User registerUser(Profile profile, OauthInfo oauthInfo) {
        User newUser = User.builder().profile(profile).oauthInfo(oauthInfo).build();
        userRepository.save(newUser);
        return newUser;
    }
}
