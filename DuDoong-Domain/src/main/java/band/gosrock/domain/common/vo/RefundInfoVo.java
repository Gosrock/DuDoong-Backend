package band.gosrock.domain.common.vo;


import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 상품 취소 가능 여부를 반환합니다. */
@Getter
public class RefundInfoVo {
    private final LocalDateTime endAt;
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
}
