package band.gosrock.api.cart.docs;


import band.gosrock.common.annotation.ExceptionDoc;
import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.interfaces.SwaggerExampleExceptions;
import band.gosrock.domain.domains.cart.exception.CartInvalidOptionAnswerException;
import band.gosrock.domain.domains.cart.exception.CartItemNotOneTypeException;
import band.gosrock.domain.domains.cart.exception.CartNotAnswerAllOptionGroupException;
import band.gosrock.domain.domains.event.exception.EventIsNotOpenStatusException;
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException;
import band.gosrock.domain.domains.ticket_item.exception.TicketPurchaseLimitException;

@ExceptionDoc
public class CreateCartExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("아이템에 대한 옵션에 대한 응답을 올바르게 안했을때 ( 한 옵션그룹에 한 응답, 옵션그룹에 응답안했을때)")
    public DuDoongCodeException 응답_올바르게_안했을_때 = CartInvalidOptionAnswerException.EXCEPTION;

    @ExplainError("모든 질문지에 대답을 안했을 때")
    public DuDoongCodeException 응답_다대답_안했을때 = CartNotAnswerAllOptionGroupException.EXCEPTION;

    @ExplainError("이벤트가 열린 상태가 아닐때")
    public DuDoongCodeException 이벤트_닫힘 = EventIsNotOpenStatusException.EXCEPTION;

    @ExplainError("이벤트 티켓팅 시간이 지났을때.")
    public DuDoongCodeException 티켓팅_시간지남 = EventTicketingTimeIsPassedException.EXCEPTION;

    @ExplainError("티켓 아이템이 한 종류가 아닐 떄")
    public DuDoongCodeException 아이템은_한종류여야함 = CartItemNotOneTypeException.EXCEPTION;

    @ExplainError("아이템의 재고가 부족한 상태일 때")
    public DuDoongCodeException 티켓팅_재고부족 = TicketItemQuantityLackException.EXCEPTION;

    @ExplainError("티켓당 1인당 구매갯수 제한을 넘었을때")
    public DuDoongCodeException 티켓팅_구매갯수제한 = TicketPurchaseLimitException.EXCEPTION;
}
