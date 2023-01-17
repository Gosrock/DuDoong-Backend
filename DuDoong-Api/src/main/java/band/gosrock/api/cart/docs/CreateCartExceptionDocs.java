package band.gosrock.api.cart.docs;


import band.gosrock.common.annotation.ExceptionDoc;
import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.interfaces.SwaggerExampleExceptions;
import band.gosrock.domain.domains.cart.exception.CartInvalidItemKindPolicyException;

@ExceptionDoc
public class CreateCartExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("카트를 담을 때 한 아이템 종류 ( 한 아이디로만 담아주세요 ), 정책 위반입니다.")
    public DuDoongCodeException 한_종류_아이템만_장바구니에 = CartInvalidItemKindPolicyException.EXCEPTION;
}
