package band.gosrock.domain.common.vo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RefundInfoVoTest {

    @Test
    void 환불가능_시간_테스() {
        // given
        LocalDateTime endAt = LocalDateTime.MAX;
        RefundInfoVo refundInfoVo = RefundInfoVo.from(endAt);

        // when
        Boolean availAble = refundInfoVo.getAvailAble();
        // then
        assertTrue(availAble);
    }

    @Test
    void 환불불가_시간_테스트() {
        // given
        LocalDateTime endAt = LocalDateTime.MIN;
        RefundInfoVo refundInfoVo = RefundInfoVo.from(endAt);

        // when
        Boolean availAble = refundInfoVo.getAvailAble();
        // then
        assertFalse(availAble);
    }
}
