package band.gosrock.domain.domains.gift.repository

import band.gosrock.domain.domains.gift.domain.TicketGift
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 선물 저장소 (#719). v2 도메인 서비스와 v1 보호용 [band.gosrock.domain.domains.gift.service.TicketGiftGuard] 만 쓴다 (ArchUnit).
 * `…ForUpdate` 는 잠금 읽기 — 티켓 행을 잠근 뒤 REPEATABLE READ 스냅샷이 아니라 최신 커밋 값을 읽는다
 */
interface TicketGiftRepository : JpaRepository<TicketGift, Long> {

    fun findByToken(token: String): TicketGift?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from TicketGift g where g.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): TicketGift?

    /** 티켓의 가장 최근 선물 (잠금 읽기) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from TicketGift g where g.issuedTicketId = :ticketId order by g.id desc")
    fun findAllByTicketForUpdate(@Param("ticketId") issuedTicketId: Long, pageable: Pageable): List<TicketGift>

    /**
     * 티켓 행을 잠근 뒤의 대기 선물 판정용 공유 잠금 읽기 (`FOR SHARE`, #719 리뷰). REPEATABLE READ 일반 읽기는 트랜잭션 스냅샷이라
     * 티켓 행 잠금을 기다리는 동안 커밋된 선물 생성을 못 본다. 잠금 읽기는 최신 커밋 값을 읽는다
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select g from TicketGift g where g.issuedTicketId = :ticketId and g.status = :status")
    fun findAllByTicketAndStatusLocked(@Param("ticketId") issuedTicketId: Long, @Param("status") status: TicketGiftStatus): List<TicketGift>

    fun findAllByIssuedTicketIdInAndStatus(issuedTicketIds: Collection<Long>, status: TicketGiftStatus): List<TicketGift>

    fun findAllByIssuedTicketIdIn(issuedTicketIds: Collection<Long>): List<TicketGift>

    fun existsByIssuedTicketIdAndStatus(issuedTicketId: Long, status: TicketGiftStatus): Boolean

    fun findAllBySenderUserIdAndStatus(senderUserId: Long, status: TicketGiftStatus): List<TicketGift>

    fun findAllByEventIdAndStatus(eventId: Long, status: TicketGiftStatus): List<TicketGift>

    fun findAllByOrderUuidAndStatus(orderUuid: String, status: TicketGiftStatus): List<TicketGift>

    fun findBySenderUserIdOrderByIdDesc(senderUserId: Long, pageable: Pageable): Page<TicketGift>

    fun findByReceiverUserIdOrderByIdDesc(receiverUserId: Long, pageable: Pageable): Page<TicketGift>

    /** 잠금 키 조회용 스칼라 (엔티티를 영속성 컨텍스트에 올리지 않는다 — open-in-view) */
    @Query("select g.orderUuid from TicketGift g where g.id = :id")
    fun findOrderUuidById(@Param("id") id: Long): String?

    /** G-2·G-8: 락 전에 보낸 사람 확인 (남의 giftId 로 주문 락을 잡지 않는다) */
    @Query("select g.senderUserId from TicketGift g where g.id = :id")
    fun findSenderUserIdById(@Param("id") id: Long): Long?

    @Query("select g.orderUuid from TicketGift g where g.token = :token")
    fun findOrderUuidByToken(@Param("token") token: String): String?
}
