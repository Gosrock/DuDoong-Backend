package band.gosrock.domain.domains.issuedTicket.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssuedTicketItemInfoVoTest {

    IssuedTicketItemInfoVo itemInfoVo;

    private final TicketType ticketType = TicketType.FIRST_COME_FIRST_SERVED;

    private final TicketPayType payType = TicketPayType.DUDOONG_TICKET;

    private final Money w3000 = Money.wons(3000L);

    private final LocalDateTime startAt = LocalDateTime.now();

    private final LocalDateTime endAt = startAt.plusDays(1L);

    @BeforeEach
    void setUp() {
        itemInfoVo =
                new IssuedTicketItemInfoVo(1L, ticketType, payType, "testTicket", w3000);
    }

    @Test
    public void 티켓_아이템_정보를_발급티켓_아이템_인포로_정상적으로_변환_테스트() {
        // given
        TicketItem newTicketItem =
                new TicketItem(TicketPayType.DUDOONG_TICKET, "testTicket", "test", w3000, 1L, 1L, 1L, ticketType, "test", "test", "test", true, true, startAt, endAt, 1L);

        // when
        IssuedTicketItemInfoVo itemInfoVoForTest = IssuedTicketItemInfoVo.from(newTicketItem);

        // then
        assertAll(
                () -> assertEquals(itemInfoVo.getPrice(), itemInfoVoForTest.getPrice()),
                () -> assertEquals(itemInfoVo.getTicketName(), itemInfoVoForTest.getTicketName()),
                () -> assertEquals(itemInfoVo.getTicketType(), itemInfoVoForTest.getTicketType()),
                () -> assertEquals(itemInfoVo.getPayType(), itemInfoVoForTest.getPayType()));
    }
}
