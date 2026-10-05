package band.gosrock.domain.domains.event.repository

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface EventRepository : CrudRepository<Event, Long>, EventCustomRepository {
    /**
     * 공연 행 공유 잠금 (`FOR SHARE`, #719): 선물 생성(G-1)이 공연 상태를 확인하는 동안 운영 삭제·상태 변경(행 X 잠금)과 줄 선다.
     * 삭제된 공연은 엔티티 @Where 로 null
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from tbl_event e where e.id = :eventId")
    fun findByIdForShare(@Param("eventId") eventId: Long): Event?

    override fun findAll(): List<Event>

    fun findAllByHostId(hostId: Long, pageable: Pageable): Page<Event>

    fun findAllByIdIn(ids: List<Long>): List<Event>

    fun findAllByHostIdIn(hostIds: List<Long>, pageable: Pageable): Page<Event>

    fun findAllByHostIdAndStatusIn(hostId: Long, statuses: Collection<EventStatus>, pageable: Pageable): Page<Event>

    /** Admin: 키워드(이벤트명/호스트명) + 상태 필터로 이벤트 검색 (@Where 바이패스를 위해 native query 사용) */
    @Query(
        value = "SELECT e.* FROM tbl_event e " +
            "LEFT JOIN tbl_host h ON h.host_id = e.host_id " +
            "WHERE (:keyword IS NULL OR e.name LIKE CONCAT('%', :keyword, '%') " +
            "OR h.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR e.status = :status)",
        countQuery = "SELECT COUNT(*) FROM tbl_event e " +
            "LEFT JOIN tbl_host h ON h.host_id = e.host_id " +
            "WHERE (:keyword IS NULL OR e.name LIKE CONCAT('%', :keyword, '%') " +
            "OR h.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR e.status = :status)",
        nativeQuery = true
    )
    fun findAllForAdmin(
        @Param("keyword") keyword: String?,
        @Param("status") status: String?,
        pageable: Pageable
    ): Page<Event>

    /** Admin: ID로 이벤트 조회 (@Where 바이패스) */
    @Query(
        value = "SELECT * FROM tbl_event e WHERE e.event_id = :eventId",
        nativeQuery = true
    )
    fun findByIdForAdmin(@Param("eventId") eventId: Long): Event?

    /** Admin: 상태별 이벤트 수 (DELETED 포함을 위해 native query) */
    @Query(
        value = "SELECT COUNT(*) FROM tbl_event e WHERE e.status = :status",
        nativeQuery = true
    )
    fun countByStatusNative(@Param("status") status: String): Long

    /** Admin: 최근 이벤트 N건 (DELETED 포함을 위해 native query) */
    @Query(
        value = "SELECT * FROM tbl_event e ORDER BY e.created_at DESC LIMIT :limit",
        nativeQuery = true
    )
    fun findTopNByOrderByCreatedAtDesc(@Param("limit") limit: Int): List<Event>

    /** Admin: 페이지네이션 없이 전체 이벤트 조회 (엑셀 다운로드용) */
    @Query(
        value = "SELECT e.* FROM tbl_event e " +
            "LEFT JOIN tbl_host h ON h.host_id = e.host_id " +
            "WHERE (:keyword IS NULL OR e.name LIKE CONCAT('%', :keyword, '%') " +
            "OR h.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR e.status = :status)",
        nativeQuery = true
    )
    fun findAllForAdminNoPage(
        @Param("keyword") keyword: String?,
        @Param("status") status: String?,
    ): List<Event>

    // ===== v2 셀프 체크인 토큰 (#712). @Where 를 타지 않는 native 쿼리라 호출 측에서 공연 존재(삭제 제외)를 먼저 확인한다 =====

    @Query(value = "SELECT e.check_in_token FROM tbl_event e WHERE e.event_id = :eventId", nativeQuery = true)
    fun findCheckInTokenById(@Param("eventId") eventId: Long): String?

    /** 토큰이 없을 때만 기록 (동시 최초 조회 시 먼저 커밋한 쪽이 이김). 반환: 바뀐 행 수 */
    @Modifying
    @Query(
        value = "UPDATE tbl_event SET check_in_token = :token WHERE event_id = :eventId AND check_in_token IS NULL",
        nativeQuery = true,
    )
    fun assignCheckInTokenIfAbsent(@Param("eventId") eventId: Long, @Param("token") token: String): Int

    /** 잠금 읽기: REPEATABLE READ 스냅샷이 아니라 최신 커밋 값을 읽는다 (경합에서 진 쪽이 이긴 토큰을 받도록) */
    @Query(value = "SELECT e.check_in_token FROM tbl_event e WHERE e.event_id = :eventId FOR UPDATE", nativeQuery = true)
    fun lockAndFindCheckInTokenById(@Param("eventId") eventId: Long): String?

    /** 토큰으로 공연 조회 (삭제된 공연 제외, @Where 적용) */
    fun findByCheckInToken(checkInToken: String): Event?
}
