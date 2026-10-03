package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field

enum class TicketItemErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    @ExplainError("요청에서 보내준 티켓 상품 id 값이 올바르지 않을 때 발생하는 오류입니다.")
    TICKET_ITEM_NOT_FOUND(NOT_FOUND, "Ticket_Item_404_1", "티켓 아이템을 찾을 수 없습니다."),

    @ExplainError("요청에서 보내준 옵션 id 값이 올바르지 않을 때 발생하는 오류입니다.")
    OPTION_NOT_FOUND(NOT_FOUND, "Option_404_1", "옵션을 찾을 수 없습니다."),

    @ExplainError("주문 요청한 티켓 상품 재고가 부족할 때 발생하는 오류입니다.")
    TICKET_ITEM_QUANTITY_LACK(
        BAD_REQUEST,
        "Ticket_Item_400_1",
        "티켓 상품 재고가 부족합니다. ( 승인 대기 또는 앞선 주문으로 인해, 재고가 있어도 주문이 불가할 수 있습니다.)"
    ),

    @ExplainError("주문 및 승인 요청 시 티켓 상품 재고보다 많은 양을 주문 시 발생하는 오류입니다.")
    TICKET_ITEM_QUANTITY_LESS_THAN_ZERO(BAD_REQUEST, "Ticket_Item_400_2", "티켓 아이템 재고가 0보다 작을 수 없습니다."),

    @ExplainError("설정할수 없는 티켓 가격일때 발생하는 오류입니다.")
    INVALID_TICKET_PRICE(BAD_REQUEST, "Ticket_Item_400_3", "설정할 수 없는 티켓 가격입니다."),

    @ExplainError("예매 취소 및 티켓 취소 요청 시 티켓 상품 공급량보다 많은 양이 반환될 때 발생하는 오류입니다.")
    TICKET_ITEM_QUANTITY_LARGER_THAN_SUPPLY_COUNT(BAD_REQUEST, "Ticket_Item_400_4", "공급량보다 많은 티켓 아이템 재고가 설정되었습니다."),

    @ExplainError("요청에서 보내준 옵션그룹 id 값이 올바르지 않을 때 발생하는 오류입니다.")
    OPTION_GROUP_NOT_FOUND(NOT_FOUND, "Option_Group_404_1", "옵션그룹을 찾을 수 없습니다."),

    @ExplainError("적용할 옵션이 해당 이벤트 소속이 아닐 때 발생하는 오류입니다.")
    INVALID_OPTION_GROUP(BAD_REQUEST, "Option_Group_400_1", "해당 이벤트 소속 옵션그룹이 아닙니다."),

    @ExplainError("옵션을 적용할 상품이 해당 이벤트 소속이 아닐 때 발생하는 오류입니다.")
    INVALID_TICKET_ITEM(BAD_REQUEST, "Ticket_Item_400_5", "해당 이벤트 소속 티켓상품이 아닙니다."),

    @ExplainError("해당 티켓상품에 이미 적용된 옵션일 경우 발생하는 오류입니다.")
    DUPLICATED_ITEM_OPTION_GROUP(BAD_REQUEST, "Item_Option_Group_400_1", "이미 적용된 옵션입니다."),

    OPTION_ANSWER_NOT_CORRECT(BAD_REQUEST, "Option_400_1", "옵션에 대한 답변이 올바르지 않습니다. T/F형일 경우 예 아니요 로 보내주세요."),

    TICKET_ITEM_PURCHASE_LIMIT(BAD_REQUEST, "Ticket_Item_400_6", "해당 티켓상품 최대 구매 가능 갯수를 넘었습니다."),

    @ExplainError("이미 재고가 감소되어 옵션 변경이 불가능할 경우 발생하는 오류입니다.")
    FORBIDDEN_OPTION_CHANGE(BAD_REQUEST, "Item_Option_Group_400_2", "옵션 변경이 불가능한 상태입니다."),

    @ExplainError("이미 재고가 감소되어 티켓상품 삭제가 불가능할 경우 발생하는 오류입니다.")
    FORBIDDEN_TICKET_ITEM_DELETE(BAD_REQUEST, "Ticket_Item_400_7", "티켓상품 삭제가 불가능한 상태입니다."),

    @ExplainError("이미 적용되어 옵션그룹 삭제가 불가능할 경우 발생하는 오류입니다.")
    FORBIDDEN_OPTION_GROUP_DELETE(BAD_REQUEST, "Option_Group_400_2", "옵션그룹 삭제가 불가능한 상태입니다."),

    @ExplainError("두둥티켓 타입에 계좌번호가 입력되지 않았을 경우 발생하는 오류입니다.")
    EMPTY_ACCOUNT_INFO(BAD_REQUEST, "Ticket_Item_400_8", "계좌정보가 필요합니다."),

    @ExplainError("티켓 지불방식과 승인방식이 불가능한 조합일때 발생하는 오류입니다.")
    INVALID_TICKET_TYPE(BAD_REQUEST, "Ticket_Item_400_9", "잘못된 티켓 승인타입입니다."),

    @ExplainError("제휴되지 않은 호스트가 유료티켓 생성을 요청했을때 발생하는 오류입니다.")
    INVALID_PARTNER(BAD_REQUEST, "Ticket_Item_400_3", "제휴된 호스트가 아닙니다."),

    @ExplainError("해당 티켓상품에 적용되지 않은 옵션을 취소 시도할 경우 발생하는 오류입니다.")
    NOT_APPLIED_ITEM_OPTION_GROUP(BAD_REQUEST, "Item_Option_Group_400_3", "적용되지 않은 옵션입니다."),

    @ExplainError("무료 티켓에 유료 옵션을 적용하려고 했을 때 발생하는 오류입니다.")
    FORBIDDEN_OPTION_PRICE(BAD_REQUEST, "Item_Option_Group_400_4", "유료 옵션을 적용할 수 없습니다."),

    @ExplainError("옵션 추가가격이 음수일 때 발생하는 오류입니다.")
    INVALID_OPTION_PRICE(BAD_REQUEST, "Option_Group_400_3", "설정할 수 없는 추가 가격입니다."),

    @ExplainError("판매 중단(isSellable=false)됐거나 판매 기간(saleStartAt~saleEndAt, 설정된 경우) 밖인 티켓을 장바구니·주문하려는 경우. 판매 기간이 없는 v1 티켓은 해당 없음")
    TICKET_ITEM_NOT_ON_SALE(BAD_REQUEST, "Ticket_Item_400_10", "지금은 판매 중인 티켓이 아닙니다."),

    // v2
    @ExplainError("v2 API 로 유료(PG, PRICE) 티켓을 만들거나 수정하려는 경우. v2 는 두둥티켓(DUDOONG)·무료티켓(FREE)만 다룹니다")
    UNSUPPORTED_V2_TICKET_PAY_TYPE(BAD_REQUEST, "Ticket_Item_400_11", "v2 에서는 두둥티켓·무료티켓만 만들거나 수정할 수 있습니다."),

    @ExplainError("티켓 없음(hasTicket=false) 공연에 티켓을 만들려는 경우")
    TICKET_DISABLED_EVENT(BAD_REQUEST, "Ticket_Item_400_12", "티켓 없음 공연에는 티켓을 만들 수 없습니다."),

    @ExplainError("판매 시작이 종료보다 늦거나 같은 경우, 판매 시작·종료가 공연 시작 이후인 경우, 새로 정한 판매 종료가 현재 이전인 경우")
    INVALID_TICKET_SALE_PERIOD(BAD_REQUEST, "Ticket_Item_400_13", "판매 기간이 올바르지 않습니다."),

    @ExplainError("판매된(재고 감소) 티켓의 티켓 종류·이름·가격·계좌·승인 여부를 바꾸거나 수량을 줄이려는 경우 (DEC-006)")
    FORBIDDEN_SOLD_TICKET_ITEM_CHANGE(BAD_REQUEST, "Ticket_Item_400_14", "판매된 티켓은 설명·판매기간·재고공개·매수제한·수량 증가만 수정할 수 있습니다."),

    @ExplainError("v2 에서 지원하지 않는 옵션 응답 형식(객관식)으로 만들려는 경우")
    UNSUPPORTED_V2_OPTION_TYPE(BAD_REQUEST, "Option_Group_400_4", "v2 에서는 주관식·네/아니오 옵션만 만들 수 있습니다."),

    @ExplainError("판매된 티켓에 붙은 옵션의 추가 금액을 바꾸려는 경우 (DEC-012). 이름·설명만 수정 가능")
    FORBIDDEN_LOCKED_OPTION_CHANGE(BAD_REQUEST, "Option_Group_400_5", "판매된 티켓에 붙은 옵션은 이름·설명만 수정할 수 있습니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation: ExplainError? = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: this.reason
    }
}
