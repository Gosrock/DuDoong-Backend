package band.gosrock.domain.domains.gift.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayStatus
import band.gosrock.domain.domains.gift.domain.TicketGift
import band.gosrock.domain.domains.gift.domain.TicketGiftCancelReason
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.exception.GiftAlreadyPendingException
import band.gosrock.domain.domains.gift.exception.GiftCannotReturnException
import band.gosrock.domain.domains.gift.exception.GiftExpiredException
import band.gosrock.domain.domains.gift.exception.GiftInvalidMemoException
import band.gosrock.domain.domains.gift.exception.GiftNotFoundException
import band.gosrock.domain.domains.gift.exception.GiftNotGiftableException
import band.gosrock.domain.domains.gift.exception.GiftNotPendingException
import band.gosrock.domain.domains.gift.exception.GiftNotReceivedTicketException
import band.gosrock.domain.domains.gift.exception.GiftOrderInvalidException
import band.gosrock.domain.domains.gift.exception.GiftOwnLinkException
import band.gosrock.domain.domains.gift.repository.TicketGiftRepository
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountState
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** 티켓의 선물 상태 (보는 사람 기준, 11 문서 8-1 T-1 표) */
enum class V2GiftState {
    /** 내 티켓, 선물 없음 (반환·거절·취소로 돌아온 티켓 포함) */
    NONE,

    /** 내 티켓, 대기 중인 선물 있음 (공연이 끝났으면 isGiftExpired) */
    PENDING,

    /** 내가 선물해 수락됨 (소유자는 받은 사람, QR 없음 — DEC-026 #3) */
    SENT,

    /** 내 티켓, 주문자 ≠ 나 (선물받음) */
    RECEIVED,
}

/** 선물 랜딩 보는 사람 기준 상태 (G-3). 판정 순서: 선물 상태 → 만료 → 본인 링크 → 받기 가능 */
enum class V2GiftViewState {
    AVAILABLE,
    ALREADY_ACCEPTED,
    REJECTED,
    RETURNED,
    CANCELED,
    OWN_LINK,
    EXPIRED,
}

/**
 * v2 티켓 선물 (#719, DEC-022·DEC-026, 11 문서 8-2). v1 코드는 이 서비스를 호출하지 않는다 (v1 보호 판정은 [band.gosrock.domain.domains.gift.service.TicketGiftGuard]).
 *
 * 잠금 순서는 **주문 → 티켓** 하나로 고정한다 (8-4 A4):
 * - 선물 전이(생성·회수·수락·거절·반환·메모)는 `주문:{orderUuid}` 락(v1·v2 의 승인·거절·취소·환불과 같은 락, 새 트랜잭션)을 잡고, 그 안에서 티켓 행을 `SELECT ... FOR UPDATE` 로 잠근 뒤
 *   선물 행을 잠금 읽기로 다시 읽어 판정한다. 같은 링크 동시 수락은 한 요청만 성공한다
 * - 주문 쪽 연쇄 처리([cancelPendingByOrder])는 주문 전이와 같은 트랜잭션(이미 `주문` 락 안)에서 그 주문의 티켓 행을 id 순으로 잠근다
 * - 주문과 무관한 연쇄 처리(보낸 사람 탈퇴·정지, 공연 운영 삭제)는 주문 락 없이 티켓 행만 id 순으로 잠근다 (선물 상태만 바꾸므로 주문 불변식과 무관)
 * - 락 밖에서는 잠금 키(orderUuid)만 스칼라로 읽는다 (open-in-view: 먼저 올린 엔티티는 락 트랜잭션의 변경을 보지 못한다)
 *
 * 공연 종료(선물 만료) = [V2EventDisplayRule] 의 PAST: 종료 시각(startAt + runTime) 경과 또는 CALCULATING·CLOSED·DELETED (8-4 A11). 배치 없이 조회·요청 때 판정한다
 */
@DomainService
@Transactional(readOnly = true)
class V2TicketGiftDomainService(
    private val issuedTicketRepository: IssuedTicketRepository,
    private val ticketGiftRepository: TicketGiftRepository,
    private val orderAdaptor: OrderAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val userAdaptor: UserAdaptor,
    private val uuidIssuer: V2TicketUuidIssuer,
    private val entityManager: EntityManager,
) {
    private val random = SecureRandom()

    // ===== 잠금 키 (락 밖 스칼라 조회) =====

    /** 내 티켓 uuid → 주문 uuid. 없는 티켓은 IssuedTicket_404_1 (소유 확인은 락 안에서) */
    fun orderUuidOfTicket(ticketUuid: String): String =
        issuedTicketRepository.findOrderUuidByUuid(ticketUuid) ?: throw IssuedTicketNotFoundException.EXCEPTION

    fun orderUuidOfGift(giftId: Long): String = ticketGiftRepository.findOrderUuidById(giftId) ?: throw GiftNotFoundException.EXCEPTION

    fun orderUuidOfToken(token: String): String = ticketGiftRepository.findOrderUuidByToken(token) ?: throw GiftNotFoundException.EXCEPTION

    // ===== 전이 =====

    /**
     * G-1 선물 링크 생성. 조건: 소유자 = 원 주문자 = 나, 원 주문 승인/확정, 입장 전, 공연 OPEN + 시작 전, 대기 중 선물 없음.
     * 남의 티켓·없는 티켓은 IssuedTicket_404_1 (존재를 드러내지 않음)
     */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun create(orderUuid: String, userId: Long, ticketUuid: String, memo: String?): TicketGift {
        val ticket = lockTicketByUuid(orderUuid, ticketUuid)
        if (ticket.getUserId() != userId) throw IssuedTicketNotFoundException.EXCEPTION
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val event = eventAdaptor.findById(ticket.eventId!!)
        val latest = lockLatestGift(ticket.id!!)
        giftBlocker(ticket, order, event, latest, userId, LocalDateTime.now())?.let { throw it }
        val gift = ticketGiftRepository.save(
            TicketGift(
                issuedTicketId = ticket.id!!,
                orderUuid = orderUuid,
                eventId = ticket.eventId!!,
                senderUserId = userId,
                token = newToken(),
                memo = normalizeMemo(memo),
            ),
        )
        Events.raise(V2TicketGiftEvent(gift.id!!, V2TicketGiftChange.SENT))
        return gift
    }

    /** G-2·G-8 락 전 소유 확인 + 잠금 키. 남의 선물·없는 선물은 Gift_404_1 (남의 giftId 로 주문 락을 잡지 않는다, #719 리뷰). 락 안에서 다시 확인한다 */
    fun orderUuidOfMyGift(userId: Long, giftId: Long): String {
        if (ticketGiftRepository.findSenderUserIdById(giftId) != userId) throw GiftNotFoundException.EXCEPTION
        return orderUuidOfGift(giftId)
    }

    /** G-2 보낸 사람 회수: 대기 중일 때 언제든(공연 시작·종료 후에도). 알림 없음 */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun cancel(orderUuid: String, userId: Long, giftId: Long) {
        val (_, gift) = lockByGiftId(orderUuid, giftId)
        if (gift.senderUserId != userId) throw GiftNotFoundException.EXCEPTION
        if (!gift.isPending()) throw GiftNotPendingException.EXCEPTION
        gift.cancel(TicketGiftCancelReason.SENDER, LocalDateTime.now())
    }

    /** G-8 메모 수정: 보낸 사람, 대기 중일 때만 */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun changeMemo(orderUuid: String, userId: Long, giftId: Long, memo: String?) {
        val (_, gift) = lockByGiftId(orderUuid, giftId)
        if (gift.senderUserId != userId) throw GiftNotFoundException.EXCEPTION
        if (!gift.isPending()) throw GiftNotPendingException.EXCEPTION
        gift.changeMemo(normalizeMemo(memo))
    }

    /** G-4 수락: 소유자를 받은 사람으로 바꾸고 티켓 uuid(QR)를 새로 발급한다 (DEC-026 #6) */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun accept(orderUuid: String, userId: Long, token: String): TicketGift {
        val (ticket, gift) = lockByToken(orderUuid, token)
        respondBlocker(ticket, gift, userId, LocalDateTime.now())?.let { throw it }
        val receiver = userAdaptor.queryUser(userId)
        gift.accept(userId, LocalDateTime.now())
        ticket.transferOwner(IssuedTicketUserInfoVo.from(receiver), uuidIssuer.issue())
        Events.raise(V2TicketGiftEvent(gift.id!!, V2TicketGiftChange.ACCEPTED))
        return gift
    }

    /** G-5 거절: 티켓은 보낸 사람에게 그대로 (uuid 유지). 수락과 같은 조건 */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun reject(orderUuid: String, userId: Long, token: String) {
        val (ticket, gift) = lockByToken(orderUuid, token)
        respondBlocker(ticket, gift, userId, LocalDateTime.now())?.let { throw it }
        gift.reject(userId, LocalDateTime.now())
        Events.raise(V2TicketGiftEvent(gift.id!!, V2TicketGiftChange.REJECTED))
    }

    /**
     * G-6 수락 후 반환 (DEC-022 #1): 받은 사람이 입장 전·공연 시작 전에 보낸 사람에게 돌려보낸다. 소유자 복귀 + uuid 교체 (DEC-026 #6).
     * 남의 티켓·없는 티켓은 IssuedTicket_404_1, 내 티켓이지만 받은 티켓이 아니면 Gift_400_8
     */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun returnTicket(orderUuid: String, userId: Long, ticketUuid: String): TicketGift {
        val ticket = lockTicketByUuid(orderUuid, ticketUuid)
        if (ticket.getUserId() != userId) throw IssuedTicketNotFoundException.EXCEPTION
        val gift = lockLatestGift(ticket.id!!)
        returnBlocker(ticket, gift, eventAdaptor.findById(ticket.eventId!!), userId, LocalDateTime.now())?.let { throw it }
        val sender = userAdaptor.queryUser(gift!!.senderUserId)
        gift.returnToSender(LocalDateTime.now())
        ticket.transferOwner(IssuedTicketUserInfoVo.from(sender), uuidIssuer.issue())
        Events.raise(V2TicketGiftEvent(gift.id!!, V2TicketGiftChange.RETURNED))
        return gift
    }

    // ===== 연쇄 처리 (원 트랜잭션 BEFORE_COMMIT, [V2TicketGiftCascadeHandler]) =====

    /**
     * 원 주문 취소(호스트 취소 v1·v2, 운영 취소, 사용자 환불, 거절) → 대기 선물 CANCELED(ORDER_CANCELED) — 링크 무효 (DEC-026 #8).
     * 대기 선물을 먼저 찾고(idx_ticket_gift_order_uuid_status), 없으면 바로 끝낸다 — 선물이 없는 주문은 선물 인덱스 조회 1회뿐 (#719 리뷰).
     * 있으면 그 선물의 티켓 행만 PK 순으로 잠그고 선물을 잠금 읽기로 다시 읽어 아직 대기 중인 것만 취소한다.
     *
     * 선물 완료(ACCEPTED) 티켓은 잠그지 않는다: 같은 트랜잭션의 v1 티켓 철회 핸들러가 주문의 다른 티켓과 함께 취소하고, 선물 기록은 ACCEPTED 그대로라
     * 이 메서드가 바꿀 것이 없다. 그 티켓을 바꿀 수 있는 선물 전이(반환 G-6)는 같은 `주문` 락으로 이미 줄 서 있다
     * @return 취소한 선물 수
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun cancelPendingByOrder(orderUuid: String): Int {
        val candidates = ticketGiftRepository.findAllByOrderUuidAndStatus(orderUuid, TicketGiftStatus.PENDING)
        if (candidates.isEmpty()) return 0
        issuedTicketRepository.findAllByIdInForUpdate(candidates.map { it.issuedTicketId }.toSet())
        return cancelLocked(candidates, TicketGiftCancelReason.ORDER_CANCELED)
    }

    /** 보낸 사람 탈퇴·운영 정지 → 그 사람이 보낸 대기 선물 CANCELED(SENDER_WITHDRAWN) (DEC-026 #9·#10). 받은 사람 쪽은 기존 탈퇴 정책 */
    @Transactional(propagation = Propagation.MANDATORY)
    fun cancelPendingBySender(senderUserId: Long): Int {
        val candidates = ticketGiftRepository.findAllBySenderUserIdAndStatus(senderUserId, TicketGiftStatus.PENDING)
        if (candidates.isEmpty()) return 0
        issuedTicketRepository.findAllByIdInForUpdate(candidates.map { it.issuedTicketId }.toSet())
        return cancelLocked(candidates, TicketGiftCancelReason.SENDER_WITHDRAWN)
    }

    /**
     * 공연 운영 삭제·비공개 전환(DELETED·PREPARING) → 대기 선물 CANCELED(EVENT_REMOVED) (DEC-026 #9).
     * 운영자가 정산중·지난공연으로 바꾼 것은 '공연 종료'라 대기 그대로 두고 '선물 만료'로 보인다 (DEC-026 #7). @return 취소한 선물 수
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun cancelPendingByEventRemoved(eventId: Long, status: EventStatus): Int {
        if (status !in EVENT_REMOVED_STATUSES) return 0
        val candidates = ticketGiftRepository.findAllByEventIdAndStatus(eventId, TicketGiftStatus.PENDING)
        if (candidates.isEmpty()) return 0
        issuedTicketRepository.findAllByIdInForUpdate(candidates.map { it.issuedTicketId }.toSet())
        return cancelLocked(candidates, TicketGiftCancelReason.EVENT_REMOVED)
    }

    /** 티켓 행을 잠근 뒤 선물을 잠금 읽기로 다시 읽어 아직 대기 중인 것만 취소 (스냅샷에 남은 옛 상태로 덮어쓰지 않는다) */
    private fun cancelLocked(candidates: List<TicketGift>, reason: TicketGiftCancelReason): Int {
        val now = LocalDateTime.now()
        return candidates.sortedBy { it.id }.count { gift ->
            entityManager.refresh(gift, LockModeType.PESSIMISTIC_WRITE)
            gift.isPending().also { pending -> if (pending) gift.cancel(reason, now) }
        }
    }

    // ===== 판정 (조회 화면과 전이가 같은 함수를 쓴다) =====

    /** 공연 종료 = 선물 만료 기준 (8-4 A11) */
    fun isEventEnded(event: Event, now: LocalDateTime): Boolean = V2EventDisplayRule.of(event, now) == V2EventDisplayStatus.PAST

    /** G-1 을 막는 사유. 없으면 null. 순서: 받은 티켓·주문 상태·입장·공연 → 대기 중 선물 */
    fun giftBlocker(ticket: IssuedTicket, order: Order, event: Event, latest: TicketGift?, userId: Long, now: LocalDateTime): DuDoongCodeException? {
        val startAt = event.getStartAt()
        val giftable = ticket.getUserId() == userId && order.userId == userId && order.orderStatus.isCanWithDraw() &&
            ticket.issuedTicketStatus.isBeforeEntrance() && event.status == EventStatus.OPEN && startAt != null && now.isBefore(startAt)
        if (!giftable) return GiftNotGiftableException.EXCEPTION
        if (latest?.isPending() == true) return GiftAlreadyPendingException.EXCEPTION
        return null
    }

    /** G-4·G-5 를 막는 사유. 순서는 랜딩 viewState 와 같다: 대기 아님 → 만료 → 본인 링크 → 원 주문·티켓 비정상 */
    fun respondBlocker(ticket: IssuedTicket, gift: TicketGift, userId: Long, now: LocalDateTime): DuDoongCodeException? {
        if (!gift.isPending()) return GiftNotPendingException.EXCEPTION
        if (isEventEnded(eventAdaptor.findById(gift.eventId), now)) return GiftExpiredException.EXCEPTION
        if (gift.senderUserId == userId) return GiftOwnLinkException.EXCEPTION
        val order = orderAdaptor.findByOrderUuid(gift.orderUuid)
        if (!order.orderStatus.isCanWithDraw() || !ticket.issuedTicketStatus.isBeforeEntrance() || ticket.getUserId() != gift.senderUserId) {
            return GiftOrderInvalidException.EXCEPTION
        }
        return null
    }

    /** G-6 을 막는 사유. 순서: 받은 티켓 아님(Gift_400_8) → 입장·취소·공연 시작 후·보낸 사람 탈퇴/정지(Gift_400_7) */
    fun returnBlocker(ticket: IssuedTicket, latest: TicketGift?, event: Event, userId: Long, now: LocalDateTime): DuDoongCodeException? {
        if (ticket.getUserId() != userId || latest == null || latest.status != TicketGiftStatus.ACCEPTED || latest.receiverUserId != userId) {
            return GiftNotReceivedTicketException.EXCEPTION
        }
        val startAt = event.getStartAt()
        val senderActive = userAdaptor.queryUser(latest.senderUserId).accountState == AccountState.NORMAL
        if (!ticket.issuedTicketStatus.isBeforeEntrance() || startAt == null || !now.isBefore(startAt) || !senderActive) {
            return GiftCannotReturnException.EXCEPTION
        }
        return null
    }

    /** 보는 사람 기준 티켓의 선물 상태. [latest] = 이 티켓의 가장 최근 선물 기록 */
    fun giftStateOf(ticket: IssuedTicket, orderUserId: Long?, latest: TicketGift?, viewerId: Long): V2GiftState {
        val mine = ticket.getUserId() == viewerId
        return when {
            mine && latest?.isPending() == true -> V2GiftState.PENDING
            !mine && latest?.status == TicketGiftStatus.ACCEPTED && latest.senderUserId == viewerId -> V2GiftState.SENT
            mine && orderUserId != viewerId -> V2GiftState.RECEIVED
            else -> V2GiftState.NONE
        }
    }

    /** G-3 랜딩 상태 (판정 순서: 선물 상태 → 만료 → 본인 링크 → 받기 가능). [viewerId] 0 = 비로그인 */
    fun viewStateOf(gift: TicketGift, event: Event, viewerId: Long, now: LocalDateTime): V2GiftViewState = when (gift.status) {
        TicketGiftStatus.CANCELED -> V2GiftViewState.CANCELED
        TicketGiftStatus.REJECTED -> V2GiftViewState.REJECTED
        TicketGiftStatus.RETURNED -> V2GiftViewState.RETURNED
        TicketGiftStatus.ACCEPTED -> V2GiftViewState.ALREADY_ACCEPTED
        TicketGiftStatus.PENDING -> when {
            isEventEnded(event, now) -> V2GiftViewState.EXPIRED
            gift.senderUserId == viewerId -> V2GiftViewState.OWN_LINK
            else -> V2GiftViewState.AVAILABLE
        }
    }

    // ===== 조회 =====

    fun queryByToken(token: String): TicketGift = ticketGiftRepository.findByToken(token) ?: throw GiftNotFoundException.EXCEPTION

    fun querySentGift(userId: Long, giftId: Long): TicketGift =
        ticketGiftRepository.findById(giftId).orElse(null)?.takeIf { it.senderUserId == userId } ?: throw GiftNotFoundException.EXCEPTION

    /** 티켓별 가장 최근 선물 기록 */
    fun latestGiftsOf(issuedTicketIds: Collection<Long>): Map<Long, TicketGift> =
        if (issuedTicketIds.isEmpty()) emptyMap()
        else ticketGiftRepository.findAllByIssuedTicketIdIn(issuedTicketIds.toSet()).groupBy { it.issuedTicketId }.mapValues { (_, g) -> g.maxBy { it.id!! } }

    /** 내가 보내 수락된 선물 (보낸 사람 티켓탭·주문상세의 '선물 완료' 행) */
    fun acceptedSentGifts(senderUserId: Long): List<TicketGift> =
        ticketGiftRepository.findAllBySenderUserIdAndStatus(senderUserId, TicketGiftStatus.ACCEPTED)

    fun sentPage(userId: Long, page: Int, size: Int) = ticketGiftRepository.findBySenderUserIdOrderByIdDesc(userId, PageRequest.of(page, size))

    fun receivedPage(userId: Long, page: Int, size: Int) = ticketGiftRepository.findByReceiverUserIdOrderByIdDesc(userId, PageRequest.of(page, size))

    // ===== 내부 =====

    private fun lockTicketByUuid(orderUuid: String, ticketUuid: String): IssuedTicket =
        issuedTicketRepository.findByUuidForUpdate(ticketUuid)?.takeIf { it.orderUuid == orderUuid } ?: throw IssuedTicketNotFoundException.EXCEPTION

    /** 티켓의 가장 최근 선물 (티켓 행을 잠근 뒤 잠금 읽기) */
    private fun lockLatestGift(issuedTicketId: Long): TicketGift? =
        ticketGiftRepository.findAllByTicketForUpdate(issuedTicketId, PageRequest.of(0, 1)).firstOrNull()

    private fun lockByGiftId(orderUuid: String, giftId: Long): Pair<IssuedTicket, TicketGift> {
        val gift = ticketGiftRepository.findById(giftId).orElse(null)?.takeIf { it.orderUuid == orderUuid } ?: throw GiftNotFoundException.EXCEPTION
        return lockWithGift(gift)
    }

    private fun lockByToken(orderUuid: String, token: String): Pair<IssuedTicket, TicketGift> {
        val gift = ticketGiftRepository.findByToken(token)?.takeIf { it.orderUuid == orderUuid } ?: throw GiftNotFoundException.EXCEPTION
        return lockWithGift(gift)
    }

    /** 티켓 행 잠금 → 선물 잠금 읽기 (주문 → 티켓 순서) */
    private fun lockWithGift(gift: TicketGift): Pair<IssuedTicket, TicketGift> {
        val ticket = issuedTicketRepository.findByIdForUpdate(gift.issuedTicketId) ?: throw GiftNotFoundException.EXCEPTION
        entityManager.refresh(gift, LockModeType.PESSIMISTIC_WRITE)
        return ticket to gift
    }

    /** 앞뒤 공백 제거, 빈 값은 null, [TicketGift.MEMO_MAX_LENGTH]자 초과는 Gift_400_9 */
    private fun normalizeMemo(memo: String?): String? {
        val trimmed = memo?.trim().orEmpty()
        if (trimmed.length > TicketGift.MEMO_MAX_LENGTH) throw GiftInvalidMemoException.EXCEPTION
        return trimmed.ifEmpty { null }
    }

    /** 추측 불가한 선물 토큰: 32바이트(256bit) SecureRandom, URL-safe base64 (패딩 없음, 43자) */
    fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val ORDER_LOCK = "주문"
        private const val TOKEN_BYTES = 32

        /** 대기 선물을 취소하는 운영 상태 변경: 삭제·준비중(비공개). 정산중·지난공연은 '종료'라 대기 유지 */
        val EVENT_REMOVED_STATUSES = setOf(EventStatus.DELETED, EventStatus.PREPARING)
    }
}
