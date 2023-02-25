package band.gosrock.domain.common.vo;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** 상품 취소 가능 여부를 반환합니다. */
@Getter
@EqualsAndHashCode
public class RefundInfoVo {

    @DateFormat private final LocalDateTime endAt;
    private final Boolean availAble;

    @Builder
    public RefundInfoVo(LocalDateTime endAt, Boolean availAble) {
        this.endAt = endAt;
        this.availAble = availAble;
    }

    public static RefundInfoVo from(LocalDateTime endAt) {
        boolean before = LocalDateTime.now().isBefore(endAt);
        return RefundInfoVo.builder().endAt(endAt).availAble(before).build();
    }

    public static RefundInfoVo of(LocalDateTime endAt, OrderStatus orderStatus) {
        boolean eventAndBefore = LocalDateTime.now().isBefore(endAt);
        Boolean availAble =
                eventAndBefore == Boolean.TRUE ? orderStatus.isCanWithDraw() : Boolean.FALSE;
        return RefundInfoVo.builder().endAt(endAt).availAble(availAble).build();
    }
}
