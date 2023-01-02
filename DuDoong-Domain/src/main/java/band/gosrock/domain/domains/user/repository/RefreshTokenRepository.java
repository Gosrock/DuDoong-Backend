package band.gosrock.domain.domains.user.repository;


import band.gosrock.domain.domains.user.domain.RefreshTokenEntity;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshTokenEntity, String> {
    Optional<RefreshTokenEntity> findByRefreshToken(String refreshToken);
}
