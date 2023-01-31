package band.gosrock.api.order.docs;


import band.gosrock.common.annotation.ExceptionDoc;
import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.interfaces.SwaggerExampleExceptions;
import band.gosrock.domain.domains.event.exception.EventIsNotOpenStatusException;
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException;
import band.gosrock.domain.domains.order.exception.CanNotRefundOrderException;
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException;

@ExceptionDoc
public class RefundOrderExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("주문이 환불 가능한 시점인지 확인합니다.")
    public DuDoongCodeException 주문_환불가능_시점확인 = NotRefundAvailableDateOrderException.EXCEPTION;

    @ExplainError("주문상태가 취소가 가능한 상태여야 합니다.")
    public DuDoongCodeException 주문상태_취소가능_검증 = CanNotRefundOrderException.EXCEPTION;

    @ExplainError("이벤트가 열린 상태가 아닐때")
    public DuDoongCodeException 이벤트_닫힘 = EventIsNotOpenStatusException.EXCEPTION;

    @ExplainError("이벤트 티켓팅 시간이 지났을때.")
    public DuDoongCodeException 티켓팅_시간지남 = EventTicketingTimeIsPassedException.EXCEPTION;
}
