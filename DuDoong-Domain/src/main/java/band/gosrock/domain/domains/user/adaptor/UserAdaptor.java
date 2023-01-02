package band.gosrock.domain.domains.user.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.exception.UserNotFoundException;
import band.gosrock.domain.domains.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserAdaptor {

    private final UserRepository userRepository;

    public User queryUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> UserNotFoundException.EXCEPTION);
    }
}
