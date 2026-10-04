package band.gosrock.domain.domains.issuedTicket.repository

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import jakarta.persistence.LockModeType
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface IssuedTicketRepository : JpaRepository<IssuedTicket, Long>, IssuedTicketCustomRepository {
    fun findAllByOrderLineId(orderLineId: Long): List<IssuedTicket>
    fun findAllByOrderUuid(orderId: String): List<IssuedTicket>
    fun findByIssuedTicketNo(issuedTicketNo: String): Optional<IssuedTicket>
    fun existsByEventId(eventId: Long): Boolean
    fun countByEventId(eventId: Long): Long
    fun findAllByEventId(eventId: Long, pageable: Pageable): Page<IssuedTicket>
    fun findAllByEventId(eventId: Long): List<IssuedTicket>
    fun findByUuid(uuid: String): Optional<IssuedTicket>

    /** 공연의 한 유저 발급 티켓 전체 (취소 포함). v2 셀프 체크인 (#712) */
    fun findAllByEventIdAndUserInfo_UserId(eventId: Long, userId: Long): List<IssuedTicket>

    fun existsByUuid(uuid: String): Boolean

    /**
     * 티켓 행 잠금 (`SELECT ... FOR UPDATE`, #719). 선물 전이·주문 연쇄 처리·입장(v1/v2)이 같은 행 잠금으로 줄 선다.
     * 잠금 읽기라 REPEATABLE READ 스냅샷이 아니라 최신 커밋 값을 읽는다
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from tbl_issued_ticket t where t.uuid = :uuid")
    fun findByUuidForUpdate(@Param("uuid") uuid: String): IssuedTicket?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from tbl_issued_ticket t where t.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): IssuedTicket?

    /** 여러 티켓 행 잠금. 교착을 피하려고 항상 id 오름차순으로 잡는다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from tbl_issued_ticket t where t.orderUuid = :orderUuid order by t.id")
    fun findAllByOrderUuidForUpdate(@Param("orderUuid") orderUuid: String): List<IssuedTicket>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from tbl_issued_ticket t where t.id in :ids order by t.id")
    fun findAllByIdInForUpdate(@Param("ids") ids: Collection<Long>): List<IssuedTicket>

    /** 잠금 키 조회용 스칼라 (엔티티를 영속성 컨텍스트에 올리지 않는다 — open-in-view) */
    @Query("select t.orderUuid from tbl_issued_ticket t where t.uuid = :uuid")
    fun findOrderUuidByUuid(@Param("uuid") uuid: String): String?
}
