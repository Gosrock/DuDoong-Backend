package band.gosrock.api.order.docs

import band.gosrock.common.annotation.ExceptionDoc
import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.interfaces.SwaggerExampleExceptions
import band.gosrock.domain.domains.event.exception.EventNotOpenException
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException
import band.gosrock.domain.domains.order.exception.CanNotCancelOrderException
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException

@ExceptionDoc
class CancelOrderExceptionDocs : SwaggerExampleExceptions {

    @ExplainError("주문이 환불 가능한 시점인지 확인합니다.")
    val 주문_환불가능_시점확인: DuDoongCodeException = NotRefundAvailableDateOrderException.EXCEPTION

    @ExplainError("주문상태가 취소가 가능한 상태여야 합니다.")
    val 주문상태_취소가능_검증: DuDoongCodeException = CanNotCancelOrderException.EXCEPTION

    @ExplainError("이벤트가 열린 상태가 아닐때")
    val 이벤트_닫힘: DuDoongCodeException = EventNotOpenException.EXCEPTION

    @ExplainError("이벤트 티켓팅 시간이 지났을때.")
    val 티켓팅_시간지남: DuDoongCodeException = EventTicketingTimeIsPassedException.EXCEPTION
}
