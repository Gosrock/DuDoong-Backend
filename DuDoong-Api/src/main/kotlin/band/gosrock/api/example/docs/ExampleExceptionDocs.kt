package band.gosrock.api.example.docs

import band.gosrock.common.annotation.ExceptionDoc
import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.interfaces.SwaggerExampleExceptions
import band.gosrock.domain.domains.order.exception.InvalidOrderException
import band.gosrock.domain.domains.order.exception.OrderNotFoundException
import band.gosrock.domain.domains.user.exception.UserNotFoundException

@ExceptionDoc
class ExampleExceptionDocs : SwaggerExampleExceptions {

    @ExplainError("유저검색시에 안나올 때 나오는 에러입니다.")
    val 유저없을때: DuDoongCodeException = UserNotFoundException.EXCEPTION

    @ExplainError("오더 낫파운드")
    val 한글도된다: DuDoongCodeException = OrderNotFoundException.EXCEPTION

    @ExplainError("인밸리드 오더")
    val 오류가났을때: DuDoongCodeException = InvalidOrderException.EXCEPTION
}
