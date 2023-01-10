package band.gosrock.api.order.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.order.model.dto.request.ConfirmOrderRequest;
import band.gosrock.api.order.model.dto.response.OrderLineTicketResponse;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.service.OrderConfirmService;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import java.util.List;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfirmOrderUseCase {

    private final OrderConfirmService orderConfirmService;
    private final OrderAdaptor orderAdaptor;
    private final UserAdaptor userAdaptor;
    private final IssuedTicketAdaptor issuedTicketAdaptor;

    private final EntityManager entityManager;

    public OrderResponse execute(String orderId, ConfirmOrderRequest confirmOrderRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userAdaptor.queryUser(currentUserId);
        ConfirmPaymentsRequest confirmPaymentsRequest =
                ConfirmPaymentsRequest.builder()
                        .paymentKey(confirmOrderRequest.getPaymentKey())
                        .amount(confirmOrderRequest.getAmount())
                        .orderId(orderId)
                        .build();
        Long confirmedOrderId = orderConfirmService.execute(confirmPaymentsRequest, currentUserId);
        return getOrderResponse(confirmedOrderId,user.getProfile().getName());
    }


    /**
     * 트랜잭션이 클래스 래벨에서 시작해서
     * 레디스 락도 트랜잭션을 격리성 보장하기위해서 새로시작하는데 레디슨 락 끝나고 커밋된후에
     * 데이타가 안읽힘. 왜냐면 클래스 레벨 트랜잭션이 레디슨 락보다 먼저 시작했기때문 (Read_commited)
     * 그래서 새로운 트랜잭션 만들어서 반영
     * 근데 이거 그냥 lazy 로딩 때문에 그런거지 쿼리 짜야되는거임!
     * @param confirmedOrderId
     * @param userName
     * @return
     */
    //TODO : 쿼리만들기
    @Transactional(propagation = Propagation.REQUIRES_NEW ,readOnly = true)
    public OrderResponse getOrderResponse(Long confirmedOrderId,String userName) {
        Order order = orderAdaptor.findById(confirmedOrderId);

        List<OrderLineTicketResponse> orderLineTicketResponses =
            order.getOrderLineItems().stream()
                .map(
                    orderLineItem ->
                        OrderLineTicketResponse.of(
                            order,
                            orderLineItem,
                            userName,
                            getTicketNoName(orderLineItem.getId())))
                .toList();

        return OrderResponse.of(order, orderLineTicketResponses);
    }

    private String getTicketNoName(Long orderLineItemId) {
        List<String> issuedTicketNos =
                issuedTicketAdaptor.findAllByOrderLineId(orderLineItemId).stream()
                        .map(IssuedTicket::getIssuedTicketNo)
                        .toList();
        Integer size = issuedTicketNos.size();
        // 없을 경우긴 함 테스트를 위해서
        if (issuedTicketNos.isEmpty()) return "";
        else if (size.equals(1)) {
            return String.format("%s (%d매)", issuedTicketNos.get(0), size);
        } else {
            return String.format(
                    "%s ~ %s (%d매)", issuedTicketNos.get(0), issuedTicketNos.get(size - 1), size);
        }
    }
}
