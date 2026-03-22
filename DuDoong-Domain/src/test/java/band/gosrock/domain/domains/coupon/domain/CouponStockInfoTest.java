package band.gosrock.domain.domains.coupon.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import band.gosrock.domain.domains.coupon.exception.NoCouponStockLeftException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CouponStockInfoTest {

    CouponStockInfo zeroCouponStockInfo;
    CouponStockInfo leftCouponStockInfo;

    @BeforeEach
    void setUp() {
        zeroCouponStockInfo =
                new CouponStockInfo(3L, 0L);
        leftCouponStockInfo =
                new CouponStockInfo(3L, 1L);
    }

    @Test
    public void 쿠폰_남은_재고_없음() {
        // given
        zeroCouponStockInfo =
                new CouponStockInfo(3L, 0L);
        // when, then
        assertThrows(
                NoCouponStockLeftException.class, () -> zeroCouponStockInfo.decreaseCouponStock());
    }

    @Test
    public void 쿠폰_남은_재고_있음() {
        // given
        leftCouponStockInfo =
                new CouponStockInfo(3L, 1L);
        // when
        leftCouponStockInfo.decreaseCouponStock();
        // then
        assertEquals(leftCouponStockInfo.getRemainingAmount(), 0L);
    }
}
