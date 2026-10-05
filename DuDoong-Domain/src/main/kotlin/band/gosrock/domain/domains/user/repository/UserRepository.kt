package band.gosrock.domain.domains.user.repository

import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import java.time.LocalDateTime
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    /** 사용자 행 공유 잠금 (`FOR SHARE`, #719): 선물 생성(G-1)이 보낸 사람 계정 상태를 확인하는 동안 탈퇴·운영 정지(행 X 잠금)와 줄 선다 */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select u from User u where u.id = :userId")
    fun findByIdForShare(@Param("userId") userId: Long): User?

    fun findByOauthInfo(oauthInfo: OauthInfo): Optional<User>

    /** user id 리스트에 포함되어 있는 유저를 모두 가져오는 쿼리 */
    fun findAllByIdIn(id: List<Long>): List<User>

    /** email 로 유저를 가져오는 쿼리 */
    fun findByProfileEmailAndAccountState(email: String, accountState: AccountState): Optional<User>

    fun findAllByProfileEmailInAndAccountState(emails: Collection<String>, accountState: AccountState): List<User>

    fun countByAccountStateAndCreatedAtBefore(accountState: AccountState, before: LocalDateTime): Long

    fun findByIdIn(userIds: List<Long>): List<User>

    /** Admin: 키워드로 유저 검색 (이름 또는 이메일) */
    @Query(
        "SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR u.profile.name LIKE %:keyword% OR u.profile.email LIKE %:keyword%)"
    )
    fun findAllByKeyword(@Param("keyword") keyword: String?, pageable: Pageable): Page<User>

    /** Admin: 페이지네이션 없이 전체 유저 조회 (엑셀 다운로드용) */
    @Query(
        "SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR u.profile.name LIKE %:keyword% OR u.profile.email LIKE %:keyword%)"
    )
    fun findAllByKeywordNoPage(@Param("keyword") keyword: String?): List<User>

    /** Admin: 오늘 가입한 유저 수 */
    fun countByCreatedAtAfter(after: LocalDateTime): Long

    /** Admin: 기간 내 가입한 유저 수 */
    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long
}
