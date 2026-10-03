package band.gosrock.domain.domains.issuedTicket.service.v2

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/** v2 체크인 규칙 (#712): 결과 코드 판정, 토큰 생성, 입장률 */
class V2CheckInDomainServiceTest {

    private val service = V2CheckInDomainService(
        mock(IssuedTicketRepository::class.java),
        mock(EventRepository::class.java),
        mock(EventAdaptor::class.java),
        mock(EntityManager::class.java),
    )

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
}
