package band.gosrock.api.example.docs

import band.gosrock.common.annotation.ExceptionDoc
import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.exception.InvalidTokenException
import band.gosrock.common.interfaces.SwaggerExampleExceptions
import band.gosrock.domain.domains.order.exception.InvalidOrderException
import band.gosrock.domain.domains.order.exception.OrderNotFoundException

@ExceptionDoc
class ExampleException2Docs : SwaggerExampleExceptions {

    @ExplainError("어쩌구 저쩌구")
    val 유저없을때: DuDoongCodeException = InvalidTokenException.EXCEPTION

    @ExplainError("오더 낫파운드")
    val 한글도된다: DuDoongCodeException = OrderNotFoundException.EXCEPTION

    @ExplainError("인밸리드 오더")
    val 오류가났을때: DuDoongCodeException = InvalidOrderException.EXCEPTION
}
