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
                IssuedTicketItemInfoVo.builder()
                        .ticketItemId(1L)
                        .ticketType(ticketType)
                        .payType(payType)
                        .ticketName("testTicket")
                        .price(w3000)
                        .build();
    }

    @Test
    public void 티켓_아이템_정보를_발급티켓_아이템_인포로_정상적으로_변환_테스트() {
        // given
        TicketItem newTicketItem =
                TicketItem.builder()
                        .payType(TicketPayType.DUDOONG_TICKET)
                        .name("testTicket")
                        .description("test")
                        .price(w3000)
                        .quantity(1L)
                        .supplyCount(1L)
                        .purchaseLimit(1L)
                        .type(ticketType)
                        .bankName("test")
                        .accountHolder("test")
                        .accountNumber("test")
                        .isQuantityPublic(true)
                        .isSellable(true)
                        .saleStartAt(startAt)
                        .saleEndAt(endAt)
                        .eventId(1L)
                        .build();

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
