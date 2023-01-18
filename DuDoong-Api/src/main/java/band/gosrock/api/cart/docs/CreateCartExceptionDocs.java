package band.gosrock.api.cart.docs;


import band.gosrock.common.annotation.ExceptionDoc;
import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.interfaces.SwaggerExampleExceptions;
import band.gosrock.domain.domains.cart.exception.CartInvalidItemKindPolicyException;
import band.gosrock.domain.domains.cart.exception.CartInvalidOptionAnswerException;

@ExceptionDoc
public class CreateCartExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("아이템에 대한 옵션에 대한 응답을 올바르게 안했을때 ( 한 옵션그룹에 한 응답, 옵션그룹에 응답안했을때)")
    public DuDoongCodeException 응답_올바르게_안했을_때 = CartInvalidOptionAnswerException.EXCEPTION;

    @ExplainError("카트를 담을 때 한 아이템 종류 ( 한 아이디로만 담아주세요 ), 정책 위반입니다.")
    public DuDoongCodeException 한_종류_아이템만_장바구니에 = CartInvalidItemKindPolicyException.EXCEPTION;
}
