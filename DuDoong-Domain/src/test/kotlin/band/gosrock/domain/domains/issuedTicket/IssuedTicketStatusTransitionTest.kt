package band.gosrock.domain.domains.issuedTicket

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketItemInfoVo
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelEntranceException
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelException
import band.gosrock.domain.domains.issuedTicket.exception.CanNotEntranceException
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketAlreadyEntranceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class IssuedTicketStatusTransitionTest {

    @Mock
    private lateinit var userInfo: IssuedTicketUserInfoVo

    @Mock
    private lateinit var itemInfo: IssuedTicketItemInfoVo

    private lateinit var issuedTicket: IssuedTicket

    @BeforeEach
    fun setUp() {
        issuedTicket = IssuedTicket.builder()
            .eventId(1L)
            .userInfo(userInfo)
            .itemInfo(itemInfo)
            .orderUuid("test-uuid")
            .orderLineId(10L)
            .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
            .issuedTicketOptionAnswers(emptyList())
            .build()
    }

    // ---- 기본 상태 ----

    @Test
    fun `신규 발급 티켓의 기본 상태는 ENTRANCE_INCOMPLETE다`() {
        assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicket.issuedTicketStatus)
    }

    // ---- cancel ----

    @Test
    fun `ENTRANCE_INCOMPLETE 상태의 티켓은 취소할 수 있다`() {
        issuedTicket.cancel()
        assertEquals(IssuedTicketStatus.CANCELED, issuedTicket.issuedTicketStatus)
    }

    @Test
    fun `이미 입장 완료된 티켓은 취소 시 CanNotCancelException이 발생한다`() {
        ReflectionTestUtils.setField(issuedTicket, "issuedTicketStatus", IssuedTicketStatus.ENTRANCE_COMPLETED)
        assertThrows(CanNotCancelException::class.java) {
            issuedTicket.cancel()
        }
    }

    @Test
    fun `이미 취소된 티켓은 다시 취소 시 CanNotCancelException이 발생한다`() {
        issuedTicket.cancel()
        assertThrows(CanNotCancelException::class.java) {
            issuedTicket.cancel()
        }
    }

    // ---- entrance ----

    @Test
    fun `ENTRANCE_INCOMPLETE 상태의 티켓은 입장 처리된다`() {
        issuedTicket.entrance()
        assertEquals(IssuedTicketStatus.ENTRANCE_COMPLETED, issuedTicket.issuedTicketStatus)
    }

    @Test
    fun `이미 입장 완료된 티켓은 재입장 시 IssuedTicketAlreadyEntranceException이 발생한다`() {
        issuedTicket.entrance()
        assertThrows(IssuedTicketAlreadyEntranceException::class.java) {
            issuedTicket.entrance()
        }
    }

    @Test
    fun `취소된 티켓은 입장 시 CanNotEntranceException이 발생한다`() {
        issuedTicket.cancel()
        assertThrows(CanNotEntranceException::class.java) {
            issuedTicket.entrance()
        }
    }

    // ---- entranceCancel ----

    @Test
    fun `ENTRANCE_COMPLETED 상태의 티켓은 입장 취소 처리된다`() {
        issuedTicket.entrance()
        issuedTicket.entranceCancel()
        assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicket.issuedTicketStatus)
    }

    @Test
    fun `ENTRANCE_INCOMPLETE 상태에서 입장 취소 시 CanNotCancelEntranceException이 발생한다`() {
        assertThrows(CanNotCancelEntranceException::class.java) {
            issuedTicket.entranceCancel()
        }
    }

    @Test
    fun `CANCELED 상태에서 입장 취소 시 CanNotCancelEntranceException이 발생한다`() {
        issuedTicket.cancel()
        assertThrows(CanNotCancelEntranceException::class.java) {
            issuedTicket.entranceCancel()
        }
    }

    // ---- sumOptionPrice ----

    @Test
    fun `옵션 답변이 없으면 sumOptionPrice는 ZERO다`() {
        assertEquals(Money.ZERO, issuedTicket.sumOptionPrice())
    }

    @Test
    fun `옵션 답변이 있으면 sumOptionPrice는 추가금액 합계를 반환한다`() {
        val answer1 = IssuedTicketOptionAnswer(optionId = 1L, additionalPrice = Money.wons(1000L), answer = "답변1")
        val answer2 = IssuedTicketOptionAnswer(optionId = 2L, additionalPrice = Money.wons(2000L), answer = "답변2")
        issuedTicket.addOptionAnswers(listOf(answer1, answer2))
        assertEquals(Money.wons(3000L), issuedTicket.sumOptionPrice())
    }

    // ---- IssuedTicketStatus 열거형 메서드 ----

    @Test
    fun `CANCELED 상태의 isCanceled는 true다`() {
        assert(IssuedTicketStatus.CANCELED.isCanceled())
    }

    @Test
    fun `ENTRANCE_INCOMPLETE 상태의 isBeforeEntrance는 true다`() {
        assert(IssuedTicketStatus.ENTRANCE_INCOMPLETE.isBeforeEntrance())
    }

    @Test
    fun `ENTRANCE_COMPLETED 상태의 isAfterEntrance는 true다`() {
        assert(IssuedTicketStatus.ENTRANCE_COMPLETED.isAfterEntrance())
    }
}
