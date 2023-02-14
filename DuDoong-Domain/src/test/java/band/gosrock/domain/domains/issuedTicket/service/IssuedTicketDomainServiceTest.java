package band.gosrock.domain.domains.issuedTicket.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.*;

import band.gosrock.domain.DisableDomainEvent;
import band.gosrock.domain.DomainIntegrateSpringBootTest;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketItemInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo;
import band.gosrock.domain.domains.issuedTicket.validator.IssuedTicketValidator;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@DomainIntegrateSpringBootTest
@DisableDomainEvent
public class IssuedTicketDomainServiceTest {

    @Autowired IssuedTicketDomainService issuedTicketDomainService;

    @MockBean UserAdaptor userAdaptor;

    @MockBean TicketItemAdaptor ticketItemAdaptor;

    @MockBean OrderAdaptor orderAdaptor;

    @MockBean IssuedTicketAdaptor issuedTicketAdaptor;

    @Mock IssuedTicket issuedTicket;

    @Mock IssuedTicket issuedTicket1;

    @Mock IssuedTicket issuedTicket2;

    @Mock IssuedTicketOptionAnswer issuedTicketOptionAnswer;

    @Mock IssuedTicketOptionAnswer issuedTicketOptionAnswer1;

    @Mock TicketItem ticketItem;

    @Mock OrderLineItem orderLineItem;

    @Mock OrderLineItem orderLineItem1;

    @Mock OrderOptionAnswer orderOptionAnswer;

    @Mock User user;

    @Mock Order order;

    @Mock IssuedTicketItemInfoVo itemInfoVo;

    @Mock IssuedTicketItemInfoVo itemInfoVo1;

    @Mock IssuedTicketUserInfoVo userInfoVo;

    @Mock IssuedTicketValidator issuedTicketValidator;

    private final String orderUuid = "orderUuid";

    private final List<IssuedTicket> issuedTickets = new ArrayList<>();

    private final List<OrderLineItem> orderLineItems = new ArrayList<>();

    private final Money w3000 = Money.wons(3000L);

    @BeforeEach
    void setUp() {
        // Todo: orderLineItem 생성

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

        issuedTicket.createUUID();
        issuedTickets.add(issuedTicket);

        issuedTicket1 =
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

        issuedTicket1.createUUID();
        issuedTickets.add(issuedTicket1);

        issuedTicket2 =
                IssuedTicket.builder()
                        .issuedTicketOptionAnswers(
                                List.of(issuedTicketOptionAnswer, issuedTicketOptionAnswer1))
                        .itemInfo(itemInfoVo1)
                        .userInfo(userInfoVo)
                        .eventId(1L)
                        .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
                        .orderLineId(1L)
                        .orderUuid(orderUuid)
                        .build();

        issuedTicket2.createUUID();

        IssuedTicketDomainService issuedTicketDomainService =
                new IssuedTicketDomainService(
                        issuedTicketAdaptor,
                        ticketItemAdaptor,
                        userAdaptor,
                        orderAdaptor,
                        issuedTicketValidator);

        orderLineItems.add(orderLineItem);
        orderLineItems.add(orderLineItem1);
    }

    @Test
    public void 티켓_취소_로직_정상_작동_테스트() {
        IssuedTicketDomainService issuedTicketDomainService =
                new IssuedTicketDomainService(
                        issuedTicketAdaptor,
                        ticketItemAdaptor,
                        userAdaptor,
                        orderAdaptor,
                        issuedTicketValidator);

        // given
        given(ticketItemAdaptor.queryTicketItem(any())).willReturn(ticketItem);

        // when
        issuedTicketDomainService.withdrawIssuedTicket(1L, issuedTickets);

        // then
        then(ticketItem).should(times(2)).increaseQuantity(1L);
        assertAll(
                () ->
                        assertEquals(
                                issuedTicket.getIssuedTicketStatus(), IssuedTicketStatus.CANCELED),
                () ->
                        assertEquals(
                                issuedTicket1.getIssuedTicketStatus(), IssuedTicketStatus.CANCELED),
                () ->
                        assertEquals(
                                issuedTicket2.getIssuedTicketStatus(),
                                IssuedTicketStatus.ENTRANCE_INCOMPLETE));
    }

    @Test
    public void 주문중_티켓_취소_로직_정상_작동_테스트() {
        IssuedTicketDomainService issuedTicketDomainService =
                new IssuedTicketDomainService(
                        issuedTicketAdaptor,
                        ticketItemAdaptor,
                        userAdaptor,
                        orderAdaptor,
                        issuedTicketValidator);

        // given
        given(issuedTicketAdaptor.findAllByOrderUuid(orderUuid)).willReturn(issuedTickets);
        given(ticketItemAdaptor.queryTicketItem(any())).willReturn(ticketItem);

        // when
        issuedTicketDomainService.doneOrderEventAfterRollBackWithdrawIssuedTickets(1L, orderUuid);

        // then
        then(ticketItem).should(times(2)).increaseQuantity(1L);
        assertAll(
                () ->
                        assertEquals(
                                issuedTicket.getIssuedTicketStatus(), IssuedTicketStatus.CANCELED),
                () ->
                        assertEquals(
                                issuedTicket1.getIssuedTicketStatus(), IssuedTicketStatus.CANCELED),
                () ->
                        assertEquals(
                                issuedTicket2.getIssuedTicketStatus(),
                                IssuedTicketStatus.ENTRANCE_INCOMPLETE));
    }

    @Test
    public void 발급_티켓_입장_처리_로직_정상_작동_테스트() {
        // given
        given(issuedTicketAdaptor.queryIssuedTicket(any())).willReturn(issuedTicket1);
        given(issuedTicketOptionAnswer.getAdditionalPrice()).willReturn(w3000);
        given(issuedTicketOptionAnswer1.getAdditionalPrice()).willReturn(w3000);

        // when
        issuedTicketDomainService.processingEntranceIssuedTicket(1L, 1L);

        // then
        assertEquals(issuedTicket1.getIssuedTicketStatus(), IssuedTicketStatus.ENTRANCE_COMPLETED);
    }

    @Test
    public void 티켓_발급_로직_정상_작동_테스트() {
        // given
        given(ticketItemAdaptor.queryTicketItem(any())).willReturn(ticketItem);
        given(userAdaptor.queryUser(any())).willReturn(user);
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
        given(order.getOrderLineItems()).willReturn(orderLineItems);

        // when
        issuedTicketDomainService.createIssuedTicket(1L, orderUuid, 1L);

        // then
        then(ticketItem).should(times(orderLineItems.size())).reduceQuantity(any());

    }
}
