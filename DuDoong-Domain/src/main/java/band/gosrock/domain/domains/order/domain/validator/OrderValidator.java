package band.gosrock.domain.domains.order.domain.validator;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.exception.CanNotCancelOrderException;
import band.gosrock.domain.domains.order.exception.CanNotRefundOrderException;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.NotApprovalOrderException;
import band.gosrock.domain.domains.order.exception.NotFreeOrderException;
import band.gosrock.domain.domains.order.exception.NotOwnerOrderException;
import band.gosrock.domain.domains.order.exception.NotPaymentOrderException;
import band.gosrock.domain.domains.order.exception.NotPendingOrderException;
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException;
import java.util.List;
import java.util.Objects;
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
        validMethodIsCanApprove(order);
        validStatusCanApprove(getOrderStatus(order));
    }

    /** 결제 방식의 주문을 승인할수있는지 확인합니다. */
    public void validCanConfirmPayment(Order order) {
        validMethodIsPaymentOrder(order);
        validStatusCanPaymentConfirm(getOrderStatus(order));
    }

    /** 선착순 방식 , 무료 주문을 바로 승인 할 수 있는 지 검증합니다 */
    public void validCanFreeConfirm(Order order) {
        validAmountIsFree(order);
        validStatusCanPaymentConfirm(getOrderStatus(order));
    }


    /** 취소할 수 있는 주문인지 검증합니다. */
    public void validCanCancel(Order order) {
        validAvailableRefundDate(order);
        validStatusCanCancel(getOrderStatus(order));
    }

    /** 환불 할 수 있는 주문인지 검증합니다. */
    public void validCanRefund(Order order) {
        validAvailableRefundDate(order);
        validStatusCanRefund(getOrderStatus(order));
    }

    /** 주문 방식이 승인 주문인지 검증합니다 */
    public void validMethodIsCanApprove(Order order) {
        if (isMethodPayment(order)) {
            throw NotApprovalOrderException.EXCEPTION;
        }
    }

    /** 주문에대한 주인인지 검증합니다. */
    public void validOwner(Order order, Long currentUserId) {
        if (!order.getUserId().equals(currentUserId)) {
            throw NotOwnerOrderException.EXCEPTION;
        }
    }

    /** 결제대금과,요청금액의 비교를 통해 정상적인 주문인지 검증합니다. */
    public void validAmountIsSameAsRequest(Order order, Money requestAmount) {
        if (!order.getTotalPaymentPrice().equals(requestAmount)) {
            throw InvalidOrderException.EXCEPTION;
        }
    }

    /** 주문 방식이 결제 방식인지 검증합니다. */
    public void validMethodIsPaymentOrder(Order order) {
        if (!isMethodPayment(order)) {
            throw NotPaymentOrderException.EXCEPTION;
        }
    }

    /** 환불 가능한 시점인지 검증합니다. */
    public void validAvailableRefundDate(Order order) {
        if (!isRefundDateNotPassed(order)) {
            throw NotRefundAvailableDateOrderException.EXCEPTION;
        }
    }

    /** 무료 주문인지 검증합니다. */
    public void validAmountIsFree(Order order) {
        if (order.isNeedPaid()) {
            throw NotFreeOrderException.EXCEPTION;
        }
    }

    /** 환불 할 수 있는 주문인지 체크합니다. */
    public Boolean isRefundDateNotPassed(Order order) {
        List<Event> events = eventAdaptor.findAllByIds(getEventIds(order));
        return reduceEventRefundAvailable(events);
    }

    private Boolean reduceEventRefundAvailable(List<Event> events) {
        return events.stream()
                .map(Event::isRefundDateNotPassed)
                .reduce(Boolean.TRUE, (Boolean::logicalAnd));
    }

    private List<Long> getEventIds(Order order) {
        return order.getOrderLineItems().stream()
                .map(orderLineItem -> orderLineItem.getOrderItem().getItemGroupId())
                .toList();
    }

    private Boolean isMethodPayment(Order order) {
        return order.getOrderMethod().isPayment();
    }

    /** 주문상태가 철회가능한 상태인지를 반환합니다. */
    public Boolean isStatusWithDraw(OrderStatus orderStatus) {
        return Objects.equals(orderStatus, OrderStatus.CONFIRM)
                || Objects.equals(orderStatus, OrderStatus.APPROVED);
    }

    /** 주문 상태가 취소가능한 상태인지 검증합니다. */
    public void validStatusCanCancel(OrderStatus orderStatus) {
        if (!isStatusWithDraw(orderStatus)) {
            throw CanNotCancelOrderException.EXCEPTION;
        }
    }

    /** 주문 상태가 환불가능한 상태인지 검증합니다. */
    public void validStatusCanRefund(OrderStatus orderStatus) {
        if (!isStatusWithDraw(orderStatus)) {
            throw CanNotRefundOrderException.EXCEPTION;
        }
    }

    /** 주문 상태가 결제방식의 승인 가능한 상태인지 검증합니다. */
    public void validStatusCanPaymentConfirm(OrderStatus orderStatus) {
        if (!Objects.equals(orderStatus, OrderStatus.PENDING_PAYMENT)) {
            throw NotPendingOrderException.EXCEPTION;
        }
    }

    /** 주문 상태가 승인방식의 승인 가능한 상태인지 검증합니다. */
    public void validStatusCanApprove(OrderStatus orderStatus) {
        if (!Objects.equals(orderStatus, OrderStatus.PENDING_APPROVE)) {
            throw NotPendingOrderException.EXCEPTION;
        }
    }

    private OrderStatus getOrderStatus(Order order) {
        return order.getOrderStatus();
    }
}
