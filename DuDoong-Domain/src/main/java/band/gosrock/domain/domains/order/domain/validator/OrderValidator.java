package band.gosrock.domain.domains.order.domain.validator;


import band.gosrock.common.annotation.Validator;
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
import band.gosrock.domain.domains.order.exception.OrderInvalidItemKindPolicyException;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * 주문 영역에 관한 검증용 메서드의 집합 레퍼런스 : https://www.youtube.com/watch?v=dJ5C4qRqAgA&t=4691s 1시 19분쯤 다른객체의
 * 참조가 필요한 상황이므로 밸리데이터를 다른 객체로 뺌.
 */
@Validator
@RequiredArgsConstructor
public class OrderValidator {

    private final EventAdaptor eventAdaptor;
    private final TicketItemAdaptor itemAdaptor;

    /** 주문을 생성할 수 있는지에 대한검증 */
    public void validCanCreate(Order order) {
        TicketItem item = getItem(order);
        Event event = getEvent(order);
        // 이벤트가 열려있는 상태인지
        validEventIsOpen(event);
        // 티켓 예매 가능 시간이 아직 안지났는지
        validTicketingTime(event);
        // 재고가 충분히 있는지 ( 추후 티켓 발급하면서도 2차로 검증함 )
        validItemStockEnough(order, item);
        // 아이템의 종류가 1종류인지
        validItemKindIsOneType(order);
        // 아이템 구매 가능 갯수를 넘지 않았는지.
        validItemPurchaseLimit(order, item);
    }

    /** 승인 가능한 주문인지 검증합니다. */
    public void validCanApproveOrder(Order order) {
        validMethodIsCanApprove(order);
        validStatusCanApprove(getOrderStatus(order));
        validCanDone(order);
    }

    /** 결제 방식의 주문을 승인할수있는지 확인합니다. */
    public void validCanConfirmPayment(Order order) {
        validMethodIsPaymentOrder(order);
        validStatusCanPaymentConfirm(getOrderStatus(order));
        validCanDone(order);
    }

    /** 선착순 방식 , 무료 주문을 바로 승인 할 수 있는 지 검증합니다 */
    public void validCanFreeConfirm(Order order) {
        validAmountIsFree(order);
        validStatusCanPaymentConfirm(getOrderStatus(order));
        validCanDone(order);
    }

    /** 취소할 수 있는 주문인지 검증합니다. */
    public void validCanCancel(Order order) {
        validAvailableRefundDate(order);
        validStatusCanCancel(getOrderStatus(order));
        validCanWithDraw(order);
    }

    /** 환불 할 수 있는 주문인지 검증합니다. */
    public void validCanRefund(Order order) {
        validAvailableRefundDate(order);
        validStatusCanRefund(getOrderStatus(order));
        validCanWithDraw(order);
    }

    /** ----------------------재료가 될 검증 메서드 ---------------------------- */

    /** 주문을 완료할 수 있는지에 대한 공통검증 */
    public void validCanDone(Order order) {
        TicketItem item = getItem(order);
        Event event = getEvent(order);

        // 이벤트가 열려있는 상태인지
        validEventIsOpen(event);
        // 티켓 예매 가능 시간이 아직 안지났는지
        validTicketingTime(event);
        // 재고가 충분히 있는지 ( 추후 티켓 발급하면서도 2차로 검증함 )
        validItemStockEnough(order, item);
        // 아이템 구매 가능 횟수를 넘지 않는지.
        validItemPurchaseLimit(order, item);
    }

    /** 주문을 철회할 수 있는 상태인지에대한 공통 검증 */
    public void validCanWithDraw(Order order) {
        Event event = getEvent(order);
        // 이벤트가 열려있는 상태인지
        validEventIsOpen(event);
        // 티켓 예매 가능 시간이 아직 안지났는지
        validTicketingTime(event);
    }

    /** 아이템의 구매갯수 제한을 넘지 않았는지 */
    public void validItemPurchaseLimit(Order order, TicketItem item) {
        item.validPurchaseLimit(order.getTotalQuantity());
    }

    /** 이벤트가 열려있는 상태인지 */
    public void validEventIsOpen(Event event) {
        event.validStatusOpen();
    }

    /** 아이템의 종류가 1종류인지. */
    public void validItemKindIsOneType(Order order) {
        List<Long> itemIds = order.getDistinctItemIds();
        if (itemIds.size() != 1) {
            throw OrderInvalidItemKindPolicyException.EXCEPTION;
        }
    }

    /** 티켓 예매 가능 시간이 아직 안지났는지. */
    public void validTicketingTime(Event event) {
        event.validTicketingTime();
    }

    /** 아이템의 재고가 충분한지 확인합니다. */
    public void validItemStockEnough(Order order, TicketItem item) {
        item.validEnoughQuantity(order.getTotalQuantity());
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
    public Boolean isStatusCanWithDraw(OrderStatus orderStatus) {
        return Objects.equals(orderStatus, OrderStatus.CONFIRM)
                || Objects.equals(orderStatus, OrderStatus.APPROVED);
    }

    /** 주문 상태가 취소가능한 상태인지 검증합니다. */
    public void validStatusCanCancel(OrderStatus orderStatus) {
        if (!isStatusCanWithDraw(orderStatus)) {
            throw CanNotCancelOrderException.EXCEPTION;
        }
    }

    /** 주문 상태가 환불가능한 상태인지 검증합니다. */
    public void validStatusCanRefund(OrderStatus orderStatus) {
        if (!isStatusCanWithDraw(orderStatus)) {
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

    private Event getEvent(Order order) {
        Long itemGroupId = order.getItemGroupId();
        return eventAdaptor.findById(itemGroupId);
    }

    private TicketItem getItem(Order order) {
        Long itemId = order.getItemId();
        return itemAdaptor.queryTicketItem(itemId);
    }
}
