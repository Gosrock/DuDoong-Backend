package band.gosrock.domain.domains.issuedTicket.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.CannotCheckInEventStatusException
import band.gosrock.domain.domains.event.exception.InvalidCheckInTokenException
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.gift.service.TicketGiftGuard
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketUserNotMatchedException
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import java.security.SecureRandom
import java.util.Base64
import org.springframework.transaction.annotation.Transactional

/** 체크인 결과 (DEC-010). [SELECT_TICKET] 은 셀프 체크인에서 입장 전 티켓이 여러 장일 때만. [GIFT_PENDING] 은 선물 대기 중인 티켓 (#719) */
enum class V2CheckInResult {
    ENTERED,
    ALREADY_ENTERED,
    OTHER_EVENT,
    CANCELED,
    SELECT_TICKET,
    GIFT_PENDING,
}

/**
 * @property eventId 판정한 공연 (호스트 스캔은 경로의 공연, 셀프는 토큰의 공연)
 * @property ticket 판정한 티켓. OTHER_EVENT 면 null (다른 공연 티켓 정보를 내보내지 않는다)
 * @property candidates SELECT_TICKET 일 때 입장 전 본인 티켓 (발급 순)
 */
data class V2CheckInOutcome(
    val eventId: Long,
    val result: V2CheckInResult,
    val ticket: IssuedTicket?,
    val candidates: List<IssuedTicket> = emptyList(),
)

/**
 * v2 QR 체크인 규칙 (DEC-009 / 010 / 011, #712). v1 코드는 이 서비스를 호출하지 않는다.
 *
 * - 호스트 스캔은 OPEN·CALCULATING 공연만, 셀프 체크인은 OPEN 만.
 * - 입장 처리는 v1 과 같은 [IssuedTicket.entrance] (상태·입장 시각·입장 메일 이벤트)를 쓴다.
 * - 동시 스캔: 티켓 행을 `SELECT ... FOR UPDATE` 로 다시 읽은 뒤 판정하므로 같은 티켓은 한 요청만 ENTERED, 나머지는 ALREADY_ENTERED.
 *   (v1 입장 API 는 락이 없다. v1 동작은 바꾸지 않는다)
 * - 없는 티켓 uuid 와 다른 공연 티켓은 둘 다 OTHER_EVENT (티켓 정보 없음): 스캐너 입장에선 둘 다 "이 공연 티켓 아님"이고, 다른 공연 티켓의 존재를 드러내지 않는다.
 * - 셀프 체크인 토큰: 공연별 고정 랜덤 토큰 (32바이트 SecureRandom, base64url 43자). 최초 조회 시 조건부 UPDATE 로 생성한다.
 * - 선물 대기 중인 티켓(#719)은 GIFT_PENDING (입장 안 함). 셀프 체크인 후보에서도 뺀다. 선물 수락으로 uuid 가 바뀐 옛 QR 은 없는 티켓 = OTHER_EVENT
 */
@DomainService
class V2CheckInDomainService(
    private val issuedTicketRepository: IssuedTicketRepository,
    private val eventRepository: EventRepository,
    private val eventAdaptor: EventAdaptor,
    private val entityManager: EntityManager,
    private val ticketGiftGuard: TicketGiftGuard,
) {
    private val random = SecureRandom()

    /** 잠금 후 상태로 판정 (입장 처리 전). 없는 티켓·다른 공연 티켓은 OTHER_EVENT */
    fun classify(ticket: IssuedTicket?, eventId: Long, giftPending: Boolean = false): V2CheckInResult = when {
        ticket == null || ticket.eventId != eventId -> V2CheckInResult.OTHER_EVENT
        ticket.issuedTicketStatus.isCanceled() -> V2CheckInResult.CANCELED
        ticket.issuedTicketStatus.isAfterEntrance() -> V2CheckInResult.ALREADY_ENTERED
        giftPending -> V2CheckInResult.GIFT_PENDING
        else -> V2CheckInResult.ENTERED
    }

    /** 호스트 스캔 가능 공연: 등록(OPEN) + 정산중(CALCULATING, 공연 종료 직후 지각 입장). 준비중·지난공연은 400 */
    fun validateHostCheckInStatus(status: EventStatus) {
        if (status != EventStatus.OPEN && status != EventStatus.CALCULATING) throw CannotCheckInEventStatusException.EXCEPTION
    }

    /** 호스트 스캔 (Q-2). 없는·삭제된 공연은 404 */
    @Transactional
    fun checkIn(eventId: Long, ticketUuid: String): V2CheckInOutcome {
        validateHostCheckInStatus(eventAdaptor.findById(eventId).status)
        return enter(eventId, issuedTicketRepository.findByUuid(ticketUuid).orElse(null))
    }

    /**
     * 관객 셀프 체크인 (Q-5). 토큰의 공연이 OPEN 이어야 한다.
     * - ticketUuid 지정: 그 티켓 (이 공연의 남의 티켓이면 IssuedTicket_400_1, 없거나 다른 공연이면 OTHER_EVENT)
     * - 미지정: 입장 전 본인 티켓(선물 대기 제외)이 1장이면 입장, 2장 이상이면 SELECT_TICKET + 후보,
     *   0장이면 선물 대기 티켓만 있으면 GIFT_PENDING, 입장한 티켓이 있으면 ALREADY_ENTERED, 취소 티켓만 있으면 CANCELED, 티켓이 없으면 OTHER_EVENT
     */
    @Transactional
    fun selfCheckIn(userId: Long, token: String, ticketUuid: String?): V2CheckInOutcome {
        val event = eventRepository.findByCheckInToken(token) ?: throw InvalidCheckInTokenException.EXCEPTION
        event.validateNotOpenStatus()
        val eventId = event.id!!
        if (ticketUuid != null) {
            val ticket = issuedTicketRepository.findByUuid(ticketUuid).orElse(null)
            if (ticket != null && ticket.eventId == eventId && ticket.getUserId() != userId) throw IssuedTicketUserNotMatchedException.EXCEPTION
            return enter(eventId, ticket)
        }
        val mine = issuedTicketRepository.findAllByEventIdAndUserInfo_UserId(eventId, userId).sortedBy { it.id }
        val notEntered = mine.filter { it.issuedTicketStatus.isBeforeEntrance() }
        val pending = ticketGiftGuard.pendingTicketIds(notEntered.mapNotNull { it.id })
        val before = notEntered.filterNot { it.id in pending }
        return when {
            before.size == 1 -> enter(eventId, before.single())
            before.size > 1 -> V2CheckInOutcome(eventId, V2CheckInResult.SELECT_TICKET, ticket = null, candidates = before)
            notEntered.isNotEmpty() -> V2CheckInOutcome(eventId, V2CheckInResult.GIFT_PENDING, notEntered.first())
            else -> {
                val entered = mine.filter { it.issuedTicketStatus.isAfterEntrance() }.maxByOrNull { it.enteredAt ?: java.time.LocalDateTime.MIN }
                when {
                    entered != null -> V2CheckInOutcome(eventId, V2CheckInResult.ALREADY_ENTERED, entered)
                    mine.isNotEmpty() -> V2CheckInOutcome(eventId, V2CheckInResult.CANCELED, mine.last())
                    else -> V2CheckInOutcome(eventId, V2CheckInResult.OTHER_EVENT, ticket = null)
                }
            }
        }
    }

    /** 이 공연 티켓이면 행 잠금 + 최신 상태로 다시 읽고 판정, 입장 가능하면 입장 처리 */
    private fun enter(eventId: Long, ticket: IssuedTicket?): V2CheckInOutcome {
        if (ticket == null || ticket.eventId != eventId) return V2CheckInOutcome(eventId, V2CheckInResult.OTHER_EVENT, ticket = null)
        entityManager.refresh(ticket, LockModeType.PESSIMISTIC_WRITE)
        // 선물 생성·수락도 같은 티켓 행 잠금 안에서 바뀌므로 잠근 뒤에 본다
        val result = classify(ticket, eventId, giftPending = ticketGiftGuard.isGiftPending(ticket.id!!))
        if (result == V2CheckInResult.ENTERED) ticket.entrance()
        return V2CheckInOutcome(eventId, result, ticket)
    }

    /**
     * 셀프 체크인 토큰 (Q-4). 없으면 만든다. 동시 최초 조회는 조건부 UPDATE 라 먼저 커밋한 토큰 하나로 정해지고,
     * 진 쪽은 잠금 읽기로 최신 커밋 값을 받는다. 삭제·없는 공연은 404
     */
    @Transactional
    fun getOrCreateCheckInToken(eventId: Long): String {
        eventAdaptor.findById(eventId)
        eventRepository.findCheckInTokenById(eventId)?.let { return it }
        eventRepository.assignCheckInTokenIfAbsent(eventId, newToken())
        return eventRepository.lockAndFindCheckInTokenById(eventId)!!
    }

    /** 추측 불가한 랜덤 토큰: 32바이트(256bit) SecureRandom, URL-safe base64 (패딩 없음, 43자) */
    fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val TOKEN_BYTES = 32
        const val TOKEN_MAX_LENGTH = 64
    }
}
