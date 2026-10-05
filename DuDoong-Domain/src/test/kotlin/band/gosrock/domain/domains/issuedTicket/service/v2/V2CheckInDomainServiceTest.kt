package band.gosrock.domain.domains.issuedTicket.service.v2

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.gift.service.TicketGiftGuard
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.CannotCheckInEventStatusException
import org.mockito.Mockito.mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyLong
import org.springframework.test.util.ReflectionTestUtils
import band.gosrock.domain.domains.event.domain.Event
import jakarta.persistence.LockModeType
import java.util.Optional

/** v2 체크인 규칙 (#712): 결과 코드 판정, 토큰 생성, 입장률 */
class V2CheckInDomainServiceTest {

    private val issuedTicketRepository = mock(IssuedTicketRepository::class.java)
    private val eventAdaptor = mock(EventAdaptor::class.java)
    private val entityManager = mock(EntityManager::class.java)
    private val giftGuard = mock(TicketGiftGuard::class.java)
    private val service = V2CheckInDomainService(issuedTicketRepository, mock(EventRepository::class.java), eventAdaptor, entityManager, giftGuard)

    private fun ticket(eventId: Long, status: IssuedTicketStatus) = IssuedTicket(eventId = eventId, issuedTicketStatus = status)

    @Test
    fun `결과 코드 - 없는 티켓·다른 공연은 OTHER_EVENT, 취소·이미 입장·입장 전`() {
        assertEquals(V2CheckInResult.OTHER_EVENT, service.classify(null, 1L))
        assertEquals(V2CheckInResult.OTHER_EVENT, service.classify(ticket(2L, IssuedTicketStatus.ENTRANCE_INCOMPLETE), 1L))
        assertEquals(V2CheckInResult.CANCELED, service.classify(ticket(1L, IssuedTicketStatus.CANCELED), 1L))
        assertEquals(V2CheckInResult.ALREADY_ENTERED, service.classify(ticket(1L, IssuedTicketStatus.ENTRANCE_COMPLETED), 1L))
        assertEquals(V2CheckInResult.ENTERED, service.classify(ticket(1L, IssuedTicketStatus.ENTRANCE_INCOMPLETE), 1L))
    }

    @Test
    fun `결과 코드 - 선물 대기 중인 입장 전 티켓은 GIFT_PENDING, 취소·입장·다른 공연 판정이 먼저 (#719)`() {
        assertEquals(V2CheckInResult.GIFT_PENDING, service.classify(ticket(1L, IssuedTicketStatus.ENTRANCE_INCOMPLETE), 1L, giftPending = true))
        assertEquals(V2CheckInResult.OTHER_EVENT, service.classify(ticket(2L, IssuedTicketStatus.ENTRANCE_INCOMPLETE), 1L, giftPending = true))
        assertEquals(V2CheckInResult.CANCELED, service.classify(ticket(1L, IssuedTicketStatus.CANCELED), 1L, giftPending = true))
        assertEquals(V2CheckInResult.ALREADY_ENTERED, service.classify(ticket(1L, IssuedTicketStatus.ENTRANCE_COMPLETED), 1L, giftPending = true))
    }

    @Test
    fun `호스트 스캔 가능 공연 상태 - OPEN, CALCULATING 만`() {
        assertDoesNotThrow { service.validateHostCheckInStatus(EventStatus.OPEN) }
        assertDoesNotThrow { service.validateHostCheckInStatus(EventStatus.CALCULATING) }
        for (status in listOf(EventStatus.PREPARING, EventStatus.CLOSED, EventStatus.DELETED)) {
            assertThrows<CannotCheckInEventStatusException> { service.validateHostCheckInStatus(status) }
        }
    }

    @Test
    fun `토큰 - base64url 43자, 매번 다르다`() {
        val tokens = (1..200).map { service.newToken() }
        assertEquals(200, tokens.toSet().size)
        tokens.forEach {
            assertEquals(43, it.length)
            assertTrue(it.matches(Regex("[A-Za-z0-9_-]+")), it)
            assertTrue(it.length <= V2CheckInDomainService.TOKEN_MAX_LENGTH)
        }
    }

    @Test
    fun `입장률 - 소수 첫째 자리 반올림, 발급 0이면 0`() {
        assertEquals(0.0, V2EntranceStats(0, 0).entranceRate)
        assertEquals(42.9, V2EntranceStats(7, 3).entranceRate)
        assertEquals(100.0, V2EntranceStats(2, 2).entranceRate)
        assertEquals(4L, V2EntranceStats(7, 3).notEnteredCount)
    }

    @Test
    fun `호스트 스캔 - 옛 QR 로 찾은 티켓이 잠그는 동안 선물 수락으로 uuid 가 바뀌면 OTHER_EVENT (입장 안 함, #719 리뷰)`() {
        val ticket = ticket(1L, IssuedTicketStatus.ENTRANCE_INCOMPLETE).also {
            ReflectionTestUtils.setField(it, "id", 5L)
            ReflectionTestUtils.setField(it, "uuid", "old-qr")
        }
        `when`(eventAdaptor.findById(1L)).thenReturn(Event(hostId = 1L, name = "공연").also { ReflectionTestUtils.setField(it, "status", EventStatus.OPEN) })
        `when`(issuedTicketRepository.findByUuid("old-qr")).thenReturn(Optional.of(ticket))
        doAnswer { ReflectionTestUtils.setField(ticket, "uuid", "new-qr"); null }.`when`(entityManager).refresh(ticket, LockModeType.PESSIMISTIC_WRITE)
        val outcome = service.checkIn(1L, "old-qr")
        assertEquals(V2CheckInResult.OTHER_EVENT, outcome.result)
        assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, ticket.issuedTicketStatus)
    }

    @Test
    fun `호스트 스캔 - 잠근 뒤 선물 대기는 잠금 읽기(isGiftPendingLocked)로 판정한다`() {
        val ticket = ticket(1L, IssuedTicketStatus.ENTRANCE_INCOMPLETE).also {
            ReflectionTestUtils.setField(it, "id", 6L)
            ReflectionTestUtils.setField(it, "uuid", "qr")
        }
        `when`(eventAdaptor.findById(1L)).thenReturn(Event(hostId = 1L, name = "공연").also { ReflectionTestUtils.setField(it, "status", EventStatus.OPEN) })
        `when`(issuedTicketRepository.findByUuid("qr")).thenReturn(Optional.of(ticket))
        `when`(giftGuard.isGiftPendingLocked(6L)).thenReturn(true)
        assertEquals(V2CheckInResult.GIFT_PENDING, service.checkIn(1L, "qr").result)
        verify(giftGuard).isGiftPendingLocked(6L)
        verify(giftGuard, never()).isGiftPending(anyLong())
    }
}
