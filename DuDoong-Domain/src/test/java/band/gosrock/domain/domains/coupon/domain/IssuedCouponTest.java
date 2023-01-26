package band.gosrock.domain.domains.coupon.domain;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.domain.domains.coupon.exception.AlreadyUsedCouponException;
import band.gosrock.domain.domains.coupon.exception.NotMyCouponException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssuedCouponTest {

    static Long userId = 1L;

    IssuedCoupon issuedCoupon;

    @BeforeEach
    void setUp() {
        issuedCoupon = IssuedCoupon.builder().userId(userId).build();
    }

    @Test
    void 사용가능한_쿠폰_상태_확인() {
        // given
        // when
        // 이미 사용한 상태라면?
        issuedCoupon.use();
        // then
        assertThrows(AlreadyUsedCouponException.class, () -> issuedCoupon.use());
    }

    @Test
    void 내쿠폰_확인() {
        // given
        // when
        issuedCoupon.validMine(userId);
        //
        assertThrows(NotMyCouponException.class, () -> issuedCoupon.validMine(2L));
    }
}
