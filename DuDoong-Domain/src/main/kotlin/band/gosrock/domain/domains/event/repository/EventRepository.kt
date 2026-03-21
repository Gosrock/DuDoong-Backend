package band.gosrock.domain.domains.event.repository

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface EventRepository : CrudRepository<Event, Long>, EventCustomRepository {
    override fun findAll(): List<Event>

    fun findAllByHostId(hostId: Long, pageable: Pageable): Page<Event>

    fun findAllByIdIn(ids: List<Long>): List<Event>

    fun findAllByHostIdIn(hostIds: List<Long>, pageable: Pageable): Page<Event>

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
}
