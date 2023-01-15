package band.gosrock.api.example.docs;


import band.gosrock.common.annotation.ExceptionDoc;
import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.InvalidTokenException;
import band.gosrock.common.interfaces.SwaggerExampleExceptions;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.OrderNotFoundException;

public class ExampleException2Docs implements SwaggerExampleExceptions {

    @ExplainError("어쩌구 저쩌구")
    public DuDoongCodeException 유저없을때 = InvalidTokenException.EXCEPTION;

    @ExplainError("오더 낫파운드")
    public DuDoongCodeException 한글도된다 = OrderNotFoundException.EXCEPTION;

    @ExplainError("인밸리드 오더")
    public DuDoongCodeException 오류가났을때 = InvalidOrderException.EXCEPTION;
}
