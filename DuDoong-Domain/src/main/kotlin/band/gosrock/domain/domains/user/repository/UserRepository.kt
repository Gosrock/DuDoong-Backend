package band.gosrock.domain.domains.user.repository

import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByOauthInfo(oauthInfo: OauthInfo): Optional<User>

    /** user id 리스트에 포함되어 있는 유저를 모두 가져오는 쿼리 */
    fun findAllByIdIn(id: List<Long>): List<User>

    /** email 로 유저를 가져오는 쿼리 */
    fun findByProfileEmailAndAccountState(email: String, accountState: AccountState): Optional<User>

    fun countByAccountStateAndCreatedAtBefore(accountState: AccountState, before: LocalDateTime): Long

    fun findByIdIn(userIds: List<Long>): List<User>
}
