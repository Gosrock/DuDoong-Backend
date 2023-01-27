package band.gosrock.domain.domains.order.domain.validator;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.NotApprovalOrderException;
import band.gosrock.domain.domains.order.exception.NotFreeOrderException;
import band.gosrock.domain.domains.order.exception.NotOwnerOrderException;
import band.gosrock.domain.domains.order.exception.NotPaymentOrderException;
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 주문 영역에 관한 검증용 메서드의 집합 레퍼런스 : https://www.youtube.com/watch?v=dJ5C4qRqAgA&t=4691s 1시 19분쯤 다른객체의
 * 참조가 필요한 상황이므로 밸리데이터를 다른 객체로 뺌.
 */
@Component
@RequiredArgsConstructor
public class OrderValidator {

    private final EventAdaptor eventAdaptor;

    /** 승인 가능한 주문인지 검증합니다. */
    public void validCanApproveOrder(Order order) {
        if (order.getOrderMethod().isPayment()) {
            throw NotApprovalOrderException.EXCEPTION;
        }
        order.getOrderStatus().validCanApprove();
    }

    /** 주문에대한 주인인지 검증합니다. */
    public void validOwner(Order order, Long currentUserId) {
        if (!order.getUserId().equals(currentUserId)) {
            throw NotOwnerOrderException.EXCEPTION;
        }
    }
    /** 결제 방식의 주문을 승인할수있는지 확인합니다. */
    public void validCanConfirmPayment(Order order) {
        validPaymentOrder(order);
        order.getOrderStatus().validCanPaymentConfirm();
    }

    public void validCanFreeConfirm(Order order) {
        validFreeOrder(order);
        order.getOrderStatus().validCanPaymentConfirm();
    }

    /** 결제대금과,요청금액의 비교를 통해 정상적인 주문인지 검증합니다. */
    public void validOrderAmountIsSame(Order order, Money requestAmount) {
        if (!order.getTotalPaymentPrice().equals(requestAmount)) {
            throw InvalidOrderException.EXCEPTION;
        }
    }

    /** 주문 방식이 결제 방식인지 검증합니다. */
    public void validPaymentOrder(Order order) {
        if (!order.getOrderMethod().isPayment()) {
            throw NotPaymentOrderException.EXCEPTION;
        }
    }

    /** 환불 가능한 시점인지 검증합니다. */
    public void validAvailableRefundDate(Order order) {
        if (!canRefundDate(order)) {
            throw NotRefundAvailableDateOrderException.EXCEPTION;
        }
    }

    /** 무료 주문인지 검증합니다. */
    public void validFreeOrder(Order order) {
        if (order.isNeedPaid()) {
            throw NotFreeOrderException.EXCEPTION;
        }
    }

    public void validCanCancel(Order order) {
        validAvailableRefundDate(order);
        order.getOrderStatus().validCanCancel();
    }

    public void validCanRefund(Order order) {
        validAvailableRefundDate(order);
        order.getOrderStatus().validCanRefund();
    }

    public Boolean canRefundDate(Order order) {
        List<Long> eventIds = getEventIds(order);
        List<Event> events = eventAdaptor.findAllByIds(eventIds);
        return reduceEventRefundAvailable(events);
    }

    private Boolean reduceEventRefundAvailable(List<Event> events) {
        return events.stream()
                .map(event -> event.getRefundInfoVo().getAvailAble())
                .reduce(Boolean.TRUE, (Boolean::logicalAnd));
    }

    private List<Long> getEventIds(Order order) {
        return order.getOrderLineItems().stream()
                .map(orderLineItem -> orderLineItem.getOrderItem().getItemGroupId())
                .toList();
    }
}
