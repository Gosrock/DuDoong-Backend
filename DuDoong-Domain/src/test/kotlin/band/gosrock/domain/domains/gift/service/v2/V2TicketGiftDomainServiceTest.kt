package band.gosrock.domain.domains.gift.service.v2

import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.gift.domain.TicketGift
import band.gosrock.domain.domains.gift.domain.TicketGiftCancelReason
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.repository.TicketGiftRepository
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.User
import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

/** v2 선물 판정 규칙 (#719): 선물 상태·랜딩 viewState 판정 순서, 만료 기준(A11), 생성·수락·반환 차단 순서, uuid 재발급 충돌 재시도, 토큰 */
class V2TicketGiftDomainServiceTest {

    private val now = LocalDateTime.of(2026, 10, 5, 12, 0)
    private val eventAdaptor = mock(EventAdaptor::class.java)
    private val orderAdaptor = mock(OrderAdaptor::class.java)
    private val userAdaptor = mock(UserAdaptor::class.java)
    private val issuedTicketRepository = mock(IssuedTicketRepository::class.java)
    private val service = V2TicketGiftDomainService(
        issuedTicketRepository, mock(TicketGiftRepository::class.java), orderAdaptor, eventAdaptor, userAdaptor,
        V2TicketUuidIssuer(issuedTicketRepository), mock(EntityManager::class.java),
    )

    private fun code(e: DuDoongCodeException?) = e?.errorCode?.getErrorReason()?.code

    private fun event(status: EventStatus = EventStatus.OPEN, startAt: LocalDateTime? = now.plusDays(1), runTime: Long? = 120L) =
        Event(hostId = 1L, name = "공연", startAt = startAt, runTime = runTime).also {
            ReflectionTestUtils.setField(it, "status", status)
            ReflectionTestUtils.setField(it, "id", 10L)
        }

    private fun ticket(owner: Long, status: IssuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE) =
        IssuedTicket(eventId = 10L, userInfo = IssuedTicketUserInfoVo(userId = owner), orderUuid = "order", issuedTicketStatus = status)
            .also { ReflectionTestUtils.setField(it, "id", 100L) }

    private fun order(userId: Long, status: OrderStatus = OrderStatus.APPROVED) = Order.forTest(userId = userId, orderStatus = status).also {
        ReflectionTestUtils.setField(it, "uuid", "order")
    }

    private fun gift(status: TicketGiftStatus = TicketGiftStatus.PENDING, sender: Long = 1L, receiver: Long? = null) =
        TicketGift(issuedTicketId = 100L, orderUuid = "order", eventId = 10L, senderUserId = sender, token = "t", memo = null).also {
            ReflectionTestUtils.setField(it, "status", status)
            ReflectionTestUtils.setField(it, "receiverUserId", receiver)
            ReflectionTestUtils.setField(it, "id", 7L)
        }

    // ===== 만료 기준 (A11) =====

    @Test
    fun `공연 종료 = 종료 시각(시작 + runTime) 경과 또는 정산중·지난공연·삭제, 진행 중·준비중은 아님`() {
        assertFalse(service.isEventEnded(event(startAt = now.plusMinutes(1)), now))
        assertFalse(service.isEventEnded(event(startAt = now.minusMinutes(119)), now), "진행 중")
        assertTrue(service.isEventEnded(event(startAt = now.minusMinutes(120)), now), "종료 시각 = 지금")
        assertTrue(service.isEventEnded(event(startAt = now.minusMinutes(1), runTime = null), now), "runTime 없으면 종료 = 시작")
        assertTrue(service.isEventEnded(event(startAt = null), now), "startAt 없음 (방어)")
        for (status in listOf(EventStatus.CALCULATING, EventStatus.CLOSED, EventStatus.DELETED)) {
            assertTrue(service.isEventEnded(event(status = status, startAt = now.plusDays(1)), now), status.name)
        }
        assertFalse(service.isEventEnded(event(status = EventStatus.PREPARING), now))
    }

    // ===== 선물 상태 =====

    @Test
    fun `giftState - 내 티켓+대기 PENDING, 남이 가진+내가 보내 수락 SENT, 내 티켓+주문자 아님 RECEIVED, 그 외 NONE`() {
        assertEquals(V2GiftState.PENDING, service.giftStateOf(ticket(1L), 1L, gift(), 1L))
        assertEquals(V2GiftState.SENT, service.giftStateOf(ticket(2L), 1L, gift(TicketGiftStatus.ACCEPTED, receiver = 2L), 1L))
        assertEquals(V2GiftState.RECEIVED, service.giftStateOf(ticket(2L), 1L, gift(TicketGiftStatus.ACCEPTED, receiver = 2L), 2L))
        assertEquals(V2GiftState.NONE, service.giftStateOf(ticket(1L), 1L, null, 1L))
        for (back in listOf(TicketGiftStatus.RETURNED, TicketGiftStatus.REJECTED, TicketGiftStatus.CANCELED)) {
            assertEquals(V2GiftState.NONE, service.giftStateOf(ticket(1L), 1L, gift(back), 1L), back.name)
        }
        // 받은 사람이 보는 받은 티켓에 다른 사람의 대기 선물은 있을 수 없지만, 대기가 우선한다
        assertEquals(V2GiftState.PENDING, service.giftStateOf(ticket(2L), 1L, gift(sender = 2L), 2L))
    }

    // ===== 랜딩 viewState =====

    @ParameterizedTest
    @EnumSource(value = TicketGiftStatus::class, names = ["CANCELED", "REJECTED", "RETURNED", "ACCEPTED"])
    fun `viewState ① 선물 상태가 만료·본인 링크보다 먼저`(status: TicketGiftStatus) {
        val expected = mapOf(
            TicketGiftStatus.CANCELED to V2GiftViewState.CANCELED,
            TicketGiftStatus.REJECTED to V2GiftViewState.REJECTED,
            TicketGiftStatus.RETURNED to V2GiftViewState.RETURNED,
            TicketGiftStatus.ACCEPTED to V2GiftViewState.ALREADY_ACCEPTED,
        ).getValue(status)
        val ended = event(status = EventStatus.CLOSED)
        assertEquals(expected, service.viewStateOf(gift(status), ended, viewerId = 1L, now = now), "보낸 사람 + 종료")
        assertEquals(expected, service.viewStateOf(gift(status), event(), viewerId = 0L, now = now), "비로그인")
    }

    @Test
    fun `viewState ② 만료가 ③ 본인 링크보다 먼저, ④ 그 밖 AVAILABLE (비로그인 포함)`() {
        assertEquals(V2GiftViewState.EXPIRED, service.viewStateOf(gift(), event(startAt = now.minusHours(3)), viewerId = 1L, now = now))
        assertEquals(V2GiftViewState.EXPIRED, service.viewStateOf(gift(), event(startAt = now.minusHours(3)), viewerId = 2L, now = now))
        assertEquals(V2GiftViewState.OWN_LINK, service.viewStateOf(gift(), event(), viewerId = 1L, now = now))
        assertEquals(V2GiftViewState.AVAILABLE, service.viewStateOf(gift(), event(), viewerId = 2L, now = now))
        assertEquals(V2GiftViewState.AVAILABLE, service.viewStateOf(gift(), event(), viewerId = 0L, now = now))
        assertEquals(V2GiftViewState.AVAILABLE, service.viewStateOf(gift(), event(startAt = now.minusMinutes(30)), viewerId = 2L, now = now), "시작 후 종료 전")
    }

    // ===== 생성 차단 =====

    @Test
    fun `G-1 차단 - 받은 티켓·주문 상태·입장·공연 상태·시작 후는 Gift_400_1, 그 다음 대기 중 선물 Gift_400_2`() {
        assertNull(service.giftBlocker(ticket(1L), order(1L), event(), null, 1L, now))
        assertNull(service.giftBlocker(ticket(1L), order(1L, OrderStatus.CONFIRM), event(), gift(TicketGiftStatus.RETURNED), 1L, now))
        assertEquals("Gift_400_2", code(service.giftBlocker(ticket(1L), order(1L), event(), gift(), 1L, now)))
        // 대기 중이어도 선물할 수 없는 티켓이면 400_1 이 먼저
        assertEquals("Gift_400_1", code(service.giftBlocker(ticket(1L, IssuedTicketStatus.ENTRANCE_COMPLETED), order(1L), event(), gift(), 1L, now)))
        assertEquals("Gift_400_1", code(service.giftBlocker(ticket(1L), order(2L), event(), null, 1L, now)), "받은 티켓")
        for (status in listOf(OrderStatus.PENDING_APPROVE, OrderStatus.CANCELED, OrderStatus.REFUND)) {
            assertEquals("Gift_400_1", code(service.giftBlocker(ticket(1L), order(1L, status), event(), null, 1L, now)), status.name)
        }
        assertEquals("Gift_400_1", code(service.giftBlocker(ticket(1L, IssuedTicketStatus.CANCELED), order(1L), event(), null, 1L, now)))
        assertEquals("Gift_400_1", code(service.giftBlocker(ticket(1L), order(1L), event(status = EventStatus.PREPARING), null, 1L, now)))
        assertEquals("Gift_400_1", code(service.giftBlocker(ticket(1L), order(1L), event(startAt = now), null, 1L, now)), "시작 시각 = 지금")
        assertEquals("Gift_400_1", code(service.giftBlocker(ticket(1L), order(1L), event(startAt = null), null, 1L, now)))
    }

    // ===== 수락·거절 차단 =====

    @Test
    fun `G-4·G-5 차단 순서 - 대기 아님 400_3 → 만료 400_5 → 본인 링크 400_4 → 원 주문·티켓 비정상 400_6`() {
        `when`(eventAdaptor.findById(anyLong())).thenReturn(event())
        `when`(orderAdaptor.findByOrderUuid(anyString())).thenReturn(order(1L))
        assertNull(service.respondBlocker(ticket(1L), gift(), 2L, now))
        assertEquals("Gift_400_4", code(service.respondBlocker(ticket(1L), gift(), 1L, now)))
        assertEquals("Gift_400_3", code(service.respondBlocker(ticket(1L), gift(TicketGiftStatus.CANCELED), 1L, now)))
        assertEquals("Gift_400_6", code(service.respondBlocker(ticket(1L, IssuedTicketStatus.CANCELED), gift(), 2L, now)))
        assertEquals("Gift_400_6", code(service.respondBlocker(ticket(3L), gift(), 2L, now)), "소유자가 보낸 사람이 아님 (방어)")

        `when`(orderAdaptor.findByOrderUuid(anyString())).thenReturn(order(1L, OrderStatus.CANCELED))
        assertEquals("Gift_400_6", code(service.respondBlocker(ticket(1L), gift(), 2L, now)))
        assertEquals("Gift_400_4", code(service.respondBlocker(ticket(1L), gift(), 1L, now)))

        `when`(eventAdaptor.findById(anyLong())).thenReturn(event(startAt = now.minusHours(3)))
        assertEquals("Gift_400_5", code(service.respondBlocker(ticket(1L), gift(), 1L, now)))
        assertEquals("Gift_400_3", code(service.respondBlocker(ticket(1L), gift(TicketGiftStatus.ACCEPTED), 2L, now)))
    }

    // ===== 반환 차단 =====

    @Test
    fun `G-6 차단 - 받은 티켓 아님 400_8 → 입장·취소·시작 후·보낸 사람 비활성 400_7`() {
        val sender = User().also { ReflectionTestUtils.setField(it, "id", 1L) }
        `when`(userAdaptor.queryUser(1L)).thenReturn(sender)
        val accepted = gift(TicketGiftStatus.ACCEPTED, receiver = 2L)
        assertNull(service.returnBlocker(ticket(2L), accepted, event(), 2L, now))
        assertEquals("Gift_400_8", code(service.returnBlocker(ticket(2L), null, event(), 2L, now)))
        assertEquals("Gift_400_8", code(service.returnBlocker(ticket(2L), gift(TicketGiftStatus.RETURNED, receiver = 2L), event(), 2L, now)))
        assertEquals("Gift_400_8", code(service.returnBlocker(ticket(2L), gift(TicketGiftStatus.ACCEPTED, receiver = 3L), event(), 2L, now)))
        assertEquals("Gift_400_7", code(service.returnBlocker(ticket(2L, IssuedTicketStatus.ENTRANCE_COMPLETED), accepted, event(), 2L, now)))
        assertEquals("Gift_400_7", code(service.returnBlocker(ticket(2L, IssuedTicketStatus.CANCELED), accepted, event(), 2L, now)))
        assertEquals("Gift_400_7", code(service.returnBlocker(ticket(2L), accepted, event(startAt = now), 2L, now)))
        for (state in listOf(AccountState.DELETED, AccountState.FORBIDDEN, AccountState.SUSPENDED)) {
            ReflectionTestUtils.setField(sender, "accountState", state)
            assertEquals("Gift_400_7", code(service.returnBlocker(ticket(2L), accepted, event(), 2L, now)), state.name)
        }
    }

    // ===== 운영 상태 변경 연쇄 대상 =====

    @Test
    fun `공연 운영 상태 변경 중 대기 선물을 취소하는 것은 삭제·준비중만`() {
        assertEquals(setOf(EventStatus.DELETED, EventStatus.PREPARING), V2TicketGiftDomainService.EVENT_REMOVED_STATUSES)
        for (status in listOf(EventStatus.OPEN, EventStatus.CALCULATING, EventStatus.CLOSED)) {
            assertEquals(0, service.cancelPendingByEventRemoved(10L, status), status.name)
        }
    }

    // ===== 토큰·uuid =====

    @Test
    fun `토큰 - base64url 43자, 매번 다르다`() {
        val tokens = (1..500).map { service.newToken() }
        assertEquals(500, tokens.toSet().size)
        tokens.forEach {
            assertEquals(43, it.length)
            assertTrue(it.matches(Regex("[A-Za-z0-9_-]+")), it)
            assertTrue(it.length <= TicketGift.TOKEN_MAX_LENGTH)
        }
    }

    @Test
    fun `uuid 재발급 - 이미 있는 값이면 다시 뽑는다, 연속 중복이면 예외`() {
        val repo = mock(IssuedTicketRepository::class.java)
        val queue = ArrayDeque(listOf("dup-1", "dup-2", "fresh"))
        val issuer = object : V2TicketUuidIssuer(repo) {
            override fun candidate(): String = queue.removeFirst()
        }
        `when`(repo.existsByUuid("dup-1")).thenReturn(true)
        `when`(repo.existsByUuid("dup-2")).thenReturn(true)
        `when`(repo.existsByUuid("fresh")).thenReturn(false)
        assertEquals("fresh", issuer.issue())

        val always = object : V2TicketUuidIssuer(repo) {
            override fun candidate(): String = "dup-1"
        }
        val e = runCatching { always.issue() }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
    }

    @Test
    fun `엔티티 전이 기록 - 수락·거절은 받은 사람, 취소는 사유와 시각`() {
        val g = gift()
        g.cancel(TicketGiftCancelReason.ORDER_CANCELED, now)
        assertEquals(TicketGiftStatus.CANCELED, g.status)
        assertEquals(TicketGiftCancelReason.ORDER_CANCELED, g.cancelReason)
        assertEquals(now, g.canceledAt)
        val a = gift().also { it.accept(5L, now) }
        assertEquals(5L, a.receiverUserId)
        assertEquals(now, a.acceptedAt)
        val r = gift().also { it.reject(6L, now) }
        assertEquals(6L, r.receiverUserId)
        assertEquals(TicketGiftStatus.REJECTED, r.status)
    }
}
