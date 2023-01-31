package band.gosrock.api.order.docs;


import band.gosrock.common.annotation.ExceptionDoc;
import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.interfaces.SwaggerExampleExceptions;
import band.gosrock.domain.domains.event.exception.EventIsNotOpenStatusException;
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException;
import band.gosrock.domain.domains.order.exception.NotFreeOrderException;
import band.gosrock.domain.domains.order.exception.NotPaymentOrderException;
import band.gosrock.domain.domains.order.exception.NotPendingOrderException;
import band.gosrock.domain.domains.order.exception.OrdeItemNotOneTypeException;
import band.gosrock.domain.domains.order.exception.OrderItemOptionChangedException;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException;
import band.gosrock.domain.domains.ticket_item.exception.TicketPurchaseLimitException;

@ExceptionDoc
public class FreeOrderExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("주문 금액이 0원인지 검증합니다.")
    public DuDoongCodeException 주문_0원_검증 = NotFreeOrderException.EXCEPTION;

    @ExplainError("주문 방식이 결제 방식인지 검증합니다.")
    public DuDoongCodeException 주문방식_결제방식_검증 = NotPaymentOrderException.EXCEPTION;

    @ExplainError("주문상태가 대기중인 상태가 아닐 때")
    public DuDoongCodeException 주문상태_검증 = NotPendingOrderException.EXCEPTION;

    @ExplainError("이벤트가 열린 상태가 아닐때")
    public DuDoongCodeException 이벤트_닫힘 = EventIsNotOpenStatusException.EXCEPTION;

    @ExplainError("이벤트 티켓팅 시간이 지났을때.")
    public DuDoongCodeException 티켓팅_시간지남 = EventTicketingTimeIsPassedException.EXCEPTION;

    @ExplainError("티켓 아이템이 한 종류가 아닐 떄")
    public DuDoongCodeException 아이템은_한종류여야함 = OrdeItemNotOneTypeException.EXCEPTION;

    @ExplainError("아이템의 재고가 부족한 상태일 때")
    public DuDoongCodeException 티켓팅_재고부족 = TicketItemQuantityLackException.EXCEPTION;

    @ExplainError("티켓당 1인당 구매갯수 제한을 넘었을때")
    public DuDoongCodeException 티켓팅_구매갯수제한 = TicketPurchaseLimitException.EXCEPTION;

    @ExplainError("주문 과정속에서 아이템의 옵션이 변화했을때 오류")
    public DuDoongCodeException 아이템_옵션_변화 = OrderItemOptionChangedException.EXCEPTION;
}
