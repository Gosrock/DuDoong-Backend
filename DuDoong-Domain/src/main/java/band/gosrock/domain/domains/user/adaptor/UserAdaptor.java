package band.gosrock.domain.domains.user.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.exception.UserNotFoundException;
import band.gosrock.domain.domains.user.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class UserAdaptor {

    private final UserRepository userRepository;

    public User queryUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> UserNotFoundException.EXCEPTION);
    }

    public Boolean exist(OauthInfo oauthInfo) {
        return userRepository.findByOauthInfo(oauthInfo).isPresent();
    }

    public User queryUserByOauthInfo(OauthInfo oauthInfo) {
        return userRepository
                .findByOauthInfo(oauthInfo)
                .orElseThrow(() -> UserNotFoundException.EXCEPTION);
    }

    /** user id 리스트에 포함되어 있는 유저를 모두 가져오는 쿼리 */
    public Set<User> queryUserListByIdIn(Set<Long> userIdList) {
        return userRepository.findAllByIdIn(userIdList);
    }

    /** 이메일로 유저를 가져오는 쿼리 */
    public User queryUserByEmail(String email) {
        return userRepository
                .findByProfileEmail(email)
                .orElseThrow(() -> UserNotFoundException.EXCEPTION);
    }
}
