package band.gosrock.domain.domains.user.repository

import band.gosrock.domain.domains.user.domain.RefreshTokenEntity
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface RefreshTokenRepository : CrudRepository<RefreshTokenEntity, String> {
    fun findByRefreshToken(refreshToken: String): Optional<RefreshTokenEntity>
}
