package band.gosrock.domain.domains.user.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.user.domain.AccountState;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.exception.UserNotFoundException;
import band.gosrock.domain.domains.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
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
    public List<User> queryUserListByIdIn(List<Long> userIdList) {
        return userRepository.findAllByIdIn(userIdList);
    }

    /** 이메일로 유저를 가져오는 쿼리 */
    public User queryUserByEmail(String email) {
        return userRepository
                .findByProfileEmailAndAccountState(email, AccountState.NORMAL)
                .orElseThrow(() -> UserNotFoundException.EXCEPTION);
    }

    public Long countNormalUserCreatedBefore(LocalDateTime before) {
        return userRepository.countByAccountStateAndCreatedAtBefore(AccountState.NORMAL, before);
    }

    public List<User> findUserByIdIn(List<Long> userIds) {
        return userRepository.findByIdIn(userIds);
    }
}
