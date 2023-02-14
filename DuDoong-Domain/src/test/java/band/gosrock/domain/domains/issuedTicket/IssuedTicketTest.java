package band.gosrock.domain.domains.issuedTicket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketItemInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelEntranceException;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelException;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotEntranceException;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketAlreadyEntranceException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssuedTicketTest {

    @Mock IssuedTicketOptionAnswer issuedTicketOptionAnswer;

    @Mock IssuedTicketOptionAnswer issuedTicketOptionAnswer1;

    @Mock IssuedTicketUserInfoVo userInfoVo;

    @Mock IssuedTicketItemInfoVo itemInfoVo;

    IssuedTicket issuedTicket;

    IssuedTicket canceledIssuedTicket;

    IssuedTicket entranceIssuedTicket;

    private final String orderUuid = "testing";

    private final Money W3000 = Money.wons(3000L);

    @BeforeEach
    void setUp() {
        issuedTicket =
                IssuedTicket.builder()
                        .issuedTicketOptionAnswers(
                                List.of(issuedTicketOptionAnswer, issuedTicketOptionAnswer1))
                        .itemInfo(itemInfoVo)
                        .userInfo(userInfoVo)
                        .eventId(1L)
                        .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
                        .orderLineId(1L)
                        .orderUuid(orderUuid)
                        .build();

        canceledIssuedTicket =
                IssuedTicket.builder()
                        .issuedTicketOptionAnswers(
                                List.of(issuedTicketOptionAnswer, issuedTicketOptionAnswer1))
                        .itemInfo(itemInfoVo)
                        .userInfo(userInfoVo)
                        .eventId(1L)
                        .issuedTicketStatus(IssuedTicketStatus.CANCELED)
                        .orderLineId(1L)
                        .orderUuid(orderUuid)
                        .build();

        entranceIssuedTicket =
                IssuedTicket.builder()
                        .issuedTicketOptionAnswers(
                                List.of(issuedTicketOptionAnswer, issuedTicketOptionAnswer1))
                        .itemInfo(itemInfoVo)
                        .userInfo(userInfoVo)
                        .eventId(1L)
                        .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_COMPLETED)
                        .orderLineId(1L)
                        .orderUuid(orderUuid)
                        .build();
    }

    @Test
    public void 발급티켓_옵션들_합_검증() {
        // given
        Money w1500 = Money.wons(1500L);
        Money w4500 = Money.wons(4500L);

        given(issuedTicketOptionAnswer.getAdditionalPrice()).willReturn(w1500);
        given(issuedTicketOptionAnswer1.getAdditionalPrice()).willReturn(w4500);

        // when
        Money sumOptionPrice = issuedTicket.sumOptionPrice();

        // then
        Money w6000 = Money.wons(6000L);
        assertEquals(w6000, sumOptionPrice);
    }

    @Test
    public void 발급티켓_취소_로직_검증() {
        // given
        // when
        issuedTicket.cancel();
        // then
        assertEquals(IssuedTicketStatus.CANCELED, issuedTicket.getIssuedTicketStatus());
    }

    @Test
    public void 이미취소된_발급티켓_취소시_에러_검증() {
        // given
        // when
        // then
        assertThrows(CanNotCancelException.class, () -> canceledIssuedTicket.cancel());
    }

    @Test
    public void 발급티켓_입장_로직_검증() {
        // given
        // when
        issuedTicket.entrance();
        // then
        assertEquals(IssuedTicketStatus.ENTRANCE_COMPLETED, issuedTicket.getIssuedTicketStatus());
    }

    @Test
    public void 취소티켓_입장요청시_에러_검증() {
        // given
        // when
        // then
        assertThrows(CanNotEntranceException.class, () -> canceledIssuedTicket.entrance());
    }

    @Test
    public void 이미입장된티켓_입장요청시_에러_검증() {
        // given
        // when
        // then
        assertThrows(
                IssuedTicketAlreadyEntranceException.class, () -> entranceIssuedTicket.entrance());
    }

    @Test
    public void 입장티켓_입장_취소_로직_검증() {
        // given
        // when
        entranceIssuedTicket.entranceCancel();
        // then
        assertEquals(
                IssuedTicketStatus.ENTRANCE_INCOMPLETE,
                entranceIssuedTicket.getIssuedTicketStatus());
    }

    @Test
    public void 입장취소요청시_에러_검증() {
        // given
        // when
        // then
        assertThrows(
                CanNotCancelEntranceException.class, () -> canceledIssuedTicket.entranceCancel());
        assertThrows(CanNotCancelEntranceException.class, () -> issuedTicket.entranceCancel());
    }

}
