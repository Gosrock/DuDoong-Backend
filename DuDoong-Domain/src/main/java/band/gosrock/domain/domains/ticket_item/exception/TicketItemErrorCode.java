package band.gosrock.domain.domains.ticket_item.exception;

import static band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST;
import static band.gosrock.common.consts.DuDoongStatic.NOT_FOUND;

import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.exception.BaseErrorCode;
import java.lang.reflect.Field;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketItemErrorCode implements BaseErrorCode {
    @ExplainError("요청에서 보내준 티켓 상품 id 값이 올바르지 않을 때 발생하는 오류입니다.")
    TICKET_ITEM_NOT_FOUND(NOT_FOUND, "Ticket_Item_404_1", "티켓 아이템을 찾을 수 없습니다."),
    @ExplainError("요청에서 보내준 옵션 id 값이 올바르지 않을 때 발생하는 오류입니다.")
    OPTION_NOT_FOUND(NOT_FOUND, "Option_404_1", "옵션을 찾을 수 없습니다."),
    @ExplainError("주문 요청한 티켓 상품 재고가 부족할 때 발생하는 오류입니다.")
    TICKET_ITEM_QUANTITY_LACK(BAD_REQUEST, "Ticket_Item_400_1", "티켓 상품 재고가 부족합니다."),
    @ExplainError("주문 및 승인 요청 시 티켓 상품 재고보다 많은 양을 주문 시 발생하는 오류입니다.")
    TICKET_ITEM_QUANTITY_LESS_THAN_ZERO(
            BAD_REQUEST, "Ticket_Item_400_2", "티켓 아이템 재고가 0보다 작을 수 없습니다."),
    @ExplainError("제휴되지 않은 호스트가 티켓 가격을 0이 아닌 값으로 요청했을때 발생하는 오류입니다.")
    INVALID_TICKET_PRICE(BAD_REQUEST, "Ticket_Item_400_3", "티켓 가격을 설정할 수 없습니다."),
    @ExplainError("예매 취소 및 티켓 취소 요청 시 티켓 상품 공급량보다 많은 양이 반환될 때 발생하는 오류입니다.")
    TICKET_ITEM_QUANTITY_LARGER_THAN_SUPPLY_COUNT(BAD_REQUEST, "Ticket_Item_400_4", "공급량보다 많은 티켓 아이템 재고가 설정되었습니다.");


    private Integer status;
    private String code;
    private String reason;

    @Override
    public ErrorReason getErrorReason() {
        return ErrorReason.builder().reason(reason).code(code).status(status).build();
    }

    @Override
    public String getExplainError() throws NoSuchFieldException {
        Field field = this.getClass().getField(this.name());
        ExplainError annotation = field.getAnnotation(ExplainError.class);
        return Objects.nonNull(annotation) ? annotation.value() : this.getReason();
    }
}
