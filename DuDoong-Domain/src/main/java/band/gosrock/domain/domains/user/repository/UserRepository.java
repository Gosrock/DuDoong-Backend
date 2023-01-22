package band.gosrock.domain.domains.user.repository;


import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOauthInfo(OauthInfo oauthInfo);

    /** user id 리스트에 포함되어 있는 유저를 모두 가져오는 쿼리 */
    Set<User> findAllByIdIn(Set<Long> id);
}
