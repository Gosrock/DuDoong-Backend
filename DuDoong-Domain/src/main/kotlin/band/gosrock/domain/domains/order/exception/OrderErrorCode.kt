package band.gosrock.domain.domains.order.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field

enum class OrderErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    @ExplainError("본인의 주문이 아닐 때 발생하는 오류. 본인의 주문만 상태변경이 가능한 api 들이 있습니다.")
    ORDER_NOT_MINE(BAD_REQUEST, "Order_400_1", "본인의 주문이 아닙니다."),

    @ExplainError("토스 결제 금액과 , 주문금액이 다를 때 등 올바르지 않은 주문 상태를 가질 때 발생하는 오류입니다.")
    ORDER_NOT_VALID(BAD_REQUEST, "Order_400_2", "올바르지 않은 주문입니다."),
    ORDER_NOT_PENDING(BAD_REQUEST, "Order_400_3", "결제,승인 대기중인 주문이 아닙니다."),
    ORDER_NOT_SUPPORTED_METHOD(BAD_REQUEST, "Order_400_4", "지원하지 않는 방식의 주문입니다."),
    ORDER_CANNOT_CANCEL(BAD_REQUEST, "Order_400_5", "주문을 취소할 수 없는 상태입니다."),
    ORDER_CANNOT_REFUND(BAD_REQUEST, "Order_400_6", "주문을 환불할 수 없는 상태입니다."),
    ORDER_NOT_APPROVAL(BAD_REQUEST, "Order_400_7", "승인 주문이 아닙니다."),
    ORDER_NOT_PAYMENT(BAD_REQUEST, "Order_400_8", "결제 주문이 아닙니다."),
    ORDER_NOT_REFUND_DATE(BAD_REQUEST, "Order_400_9", "환불을 할 수 있는 기한을 지났습니다."),
    ORDER_NOT_FOUND(NOT_FOUND, "Order_404_1", "주문을 찾을 수 없습니다."),
    ORDER_LINE_NOT_FOUND(NOT_FOUND, "Order_404_2", "주문 라인을 찾을 수 없습니다."),
    ORDER_NOT_FREE(BAD_REQUEST, "Order_400_10", "무료 주문이 아닙니다."),
    ORDER_LESS_THAN_MINIMUM(BAD_REQUEST, "Order_400_11", "최소 결제금액인 1000원보다 낮은 주문입니다."),

    @ExplainError("한 장바구니엔 관련된 한 아이템만 올수 있음")
    ORDER_INVALID_ITEM_KIND_POLICY(BAD_REQUEST, "Order_400_12", "장바구니에 아이템을 담는 정책을 위반하였습니다."),
    ORDER_OPTION_CHANGED(BAD_REQUEST, "Order_400_13", "주문 과정중 아이템의 옵션이 변화했습니다."),
    CAN_NOT_DELETED_USER_APPROVE(BAD_REQUEST, "Order_400_14", "유저가 탈퇴를 했습니다."),
    APPROVE_WAITING_PURCHASE_LIMIT(BAD_REQUEST, "Order_400_15", "승인 대기중인 주문으로 인해 티켓 최대 구매 가능 횟수를 넘겼습니다.이미 신청한 주문이 승인 될 때까지 기다려주세요."),
    ORDER_CANNOT_REFUSE(BAD_REQUEST, "Order_400_16", "승인 대기중인 주문을 거절할 수 없는 상태입니다."),

    // v2
    @ExplainError("환불 요청 상태(REFUND_REQUESTED)가 아닌 주문을 환불 완료 처리하려는 경우. 이미 환불 완료면 그대로 성공")
    ORDER_REFUND_NOT_REQUESTED(BAD_REQUEST, "Order_400_17", "환불 요청된 주문이 아닙니다."),

    @ExplainError("거절 사유가 기타(ETC)인데 직접 입력 사유가 비었거나 20자를 넘는 경우")
    INVALID_REFUSE_REASON(BAD_REQUEST, "Order_400_18", "거절 사유가 올바르지 않습니다."),

    @ExplainError("v2 주문 엑셀 다운로드 대상이 행 상한(10,000)을 넘는 경우. 상태 필터·검색어로 줄여서 받는다")
    EXPORT_TOO_MANY_ORDERS(BAD_REQUEST, "Order_400_19", "엑셀로 내려받을 주문이 너무 많습니다. 필터로 줄여 주세요."),

    @ExplainError("v2 사용자 주문(O-1)으로 유료(PG, PRICE) 티켓을 주문하려는 경우. v2 는 두둥티켓·무료티켓만 주문할 수 있다 (P-5 isPurchasable=false 와 같은 기준)")
    V2_UNSUPPORTED_ORDER_TICKET(BAD_REQUEST, "Order_400_20", "이 티켓은 앱에서 주문할 수 없습니다."),

    @ExplainError("결제 방식이 티켓과 맞지 않는 경우. 두둥티켓은 BANK_TRANSFER / TOSS_TRANSFER, 무료티켓은 FREE")
    V2_INVALID_PAYMENT_METHOD(BAD_REQUEST, "Order_400_21", "티켓에 맞지 않는 결제 방식입니다."),

    @ExplainError("두둥티켓 주문의 입금자명이 없거나 20자를 넘는 경우 (앞뒤 공백 제외)")
    V2_INVALID_DEPOSITOR_NAME(BAD_REQUEST, "Order_400_22", "입금자명은 1~20자로 입력해 주세요."),

    @ExplainError("옵션 답변이 올바르지 않은 경우: 티켓별 입력(applyToAll=false)인데 답변 묶음 수가 수량과 다름, 티켓에 없는 옵션, 같은 옵션 중복, 옵션 누락, 네/아니오 값이 아님, 주관식 빈 값·255자 초과")
    V2_INVALID_OPTION_ANSWERS(BAD_REQUEST, "Order_400_23", "옵션 답변이 올바르지 않습니다."),

    @ExplainError("사용자 취소(O-4) 불가: 입장한 티켓·본인 소유가 아닌 티켓(선물)이 있거나, 카드(PG) 결제 주문인 경우. 기한·상태는 Order_400_9 / Order_400_5")
    V2_ORDER_CANNOT_CANCEL_BY_USER(BAD_REQUEST, "Order_400_24", "취소할 수 없는 주문입니다."),

    @ExplainError("유료(두둥티켓) 주문을 취소하면서 환불 받을 계좌를 보내지 않은 경우")
    V2_REFUND_ACCOUNT_REQUIRED(BAD_REQUEST, "Order_400_25", "환불 받을 계좌를 입력해 주세요."),

    @ExplainError("같은 사용자가 같은 무료(선착순) 티켓 주문을 짧은 시간에 다시 보냈는데 앞 주문이 아직 발급 중인 경우. 잠시 후 주문내역에서 확인한다")
    V2_DUPLICATE_ORDER_IN_PROGRESS(BAD_REQUEST, "Order_400_26", "같은 주문을 처리하고 있습니다. 잠시 후 주문내역을 확인해 주세요."),

    @ExplainError("환불 계좌를 입력할 수 없는 주문 (#728): 환불 요청이 없음, 무료·0원, 카드(PG) 결제 주문")
    V2_REFUND_ACCOUNT_NOT_ALLOWED(BAD_REQUEST, "Order_400_27", "환불 계좌를 입력할 수 없는 주문입니다."),

    @ExplainError("이미 환불 완료된 주문은 환불 계좌를 바꿀 수 없음 (#728)")
    V2_REFUND_ALREADY_COMPLETED(BAD_REQUEST, "Order_400_28", "이미 환불이 완료된 주문입니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation: ExplainError? = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: this.reason
    }
}
