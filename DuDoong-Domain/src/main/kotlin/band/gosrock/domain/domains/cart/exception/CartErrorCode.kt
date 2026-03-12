package band.gosrock.domain.domains.cart.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field
import java.util.Objects

enum class CartErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    @ExplainError("id로 카트를 찾을 때 못 찾으면 발생하는 오류")
    CART_NOT_FOUND(NOT_FOUND, "Cart_404_1", "장바구니를 찾을 수 없습니다."),

    @ExplainError("한 장바구니엔 관련된 한 아이템만 올수 있음")
    CART_INVALID_ITEM_KIND_POLICY(BAD_REQUEST, "Cart_400_1", "장바구니에 아이템을 담는 정책을 위반하였습니다."),
    CART_INVALID_OPTION_ANSWER(BAD_REQUEST, "Cart_400_2", "옵션을 잘못 응답 하였습니다."),
    CART_LINE_NOT_FOUND(BAD_REQUEST, "Cart_400_3", "장바구니 안에 상품을 찾을 수 없습니다."),
    CART_NOT_ALL_ANSWER(BAD_REQUEST, "Cart_400_4", "모든 질문에 답변을 하지 않았습니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason.builder().reason(reason).code(code).status(status).build()

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation: ExplainError? = field.getAnnotation(ExplainError::class.java)
        return if (Objects.nonNull(annotation)) annotation!!.value else this.reason
    }
}
