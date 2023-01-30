package band.gosrock.api.order.docs;


import band.gosrock.common.annotation.ExceptionDoc;
import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.interfaces.SwaggerExampleExceptions;
import band.gosrock.domain.domains.event.exception.EventIsNotOpenStatusException;
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.OrdeItemNotOneTypeException;
import band.gosrock.domain.domains.order.exception.OrderItemOptionChangedException;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException;
import band.gosrock.domain.domains.ticket_item.exception.TicketPurchaseLimitException;

@ExceptionDoc
public class CreateOrderExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("선착순 주문이라 결제가 필요한 주문인데 승인으로 요청하거나 쿠폰이 적용된 주문인데 결제금액이 없는경우 등 잘못된 주문 요청")
    public DuDoongCodeException 잘못된주문생성요청 = InvalidOrderException.EXCEPTION;

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
