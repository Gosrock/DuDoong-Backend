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

    @DateFormat private final LocalDateTime startAt;
    private final Boolean availAble;

    @Builder
    public RefundInfoVo(LocalDateTime startAt, Boolean availAble) {
        this.startAt = startAt;
        this.availAble = availAble;
    }

    public static RefundInfoVo from(LocalDateTime startAt) {
        boolean before = nowIsBefore(startAt);
        return RefundInfoVo.builder().startAt(startAt).availAble(before).build();
    }

    private static boolean nowIsBefore(LocalDateTime startAt) {
        return LocalDateTime.now().isBefore(startAt);
    }

    public static RefundInfoVo of(LocalDateTime startAt, OrderStatus orderStatus) {
        Boolean availAble =
                nowIsBefore(startAt) == Boolean.TRUE ? orderStatus.isCanWithDraw() : Boolean.FALSE;
        return RefundInfoVo.builder().startAt(startAt).availAble(availAble).build();
    }
}
