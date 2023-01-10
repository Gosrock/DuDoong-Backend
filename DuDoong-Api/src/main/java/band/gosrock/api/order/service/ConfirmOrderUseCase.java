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
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfirmOrderUseCase {

    private final OrderConfirmService orderConfirmService;
    private final OrderAdaptor orderAdaptor;
    private final UserAdaptor userAdaptor;
    private final IssuedTicketAdaptor issuedTicketAdaptor;

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
        Order order = orderAdaptor.findById(confirmedOrderId);

        List<OrderLineTicketResponse> orderLineTicketResponses =
                order.getOrderLineItems().stream()
                        .map(
                                orderLineItem ->
                                        OrderLineTicketResponse.of(
                                                order,
                                                orderLineItem,
                                                user.getProfile().getName(),
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
