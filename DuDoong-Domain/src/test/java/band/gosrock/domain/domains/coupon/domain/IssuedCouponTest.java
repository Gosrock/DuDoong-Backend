package band.gosrock.domain.domains.coupon.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.coupon.exception.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssuedCouponTest {

    static Long userId = 1L;
    static LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
    static Long validTerm = 7L;
    static Long discountAmount = 10000L;
    static Long minimumCost = 50000L;
    static Long discountPercentage = 30L;
    IssuedCoupon issuedCoupon;
    @Mock CouponCampaign couponCampaign;

    @BeforeEach
    void setUp() {
        issuedCoupon = IssuedCoupon.builder().userId(userId).couponCampaign(couponCampaign).build();
    }

    @Test
    void 사용가능한_쿠폰_상태_확인() {
        // 이미 사용한 상태 쿠폰 사용
        issuedCoupon.use();
        // then
        assertThrows(AlreadyUsedCouponException.class, () -> issuedCoupon.use());
    }

    @Test
    public void use_usageStatusFalse_setUsageStatusToTrue() {
        // 사용 안한 쿠폰 사용
        // when
        issuedCoupon.use();
        // then
        assertTrue(issuedCoupon.getUsageStatus());
    }

    @Test
    void 내쿠폰_확인() {
        // given
        // when
        issuedCoupon.validMine(userId);
        //
        assertThrows(NotMyCouponException.class, () -> issuedCoupon.validMine(2L));
    }

    @Test
    public void validMine_validUserId_noExceptionThrown() {
        // when
        issuedCoupon.validMine(1L);
        // then
        assertEquals(issuedCoupon.getUserId(), 1L);
    }

    //    @Test
    //    void testIsAvailableTerm() {
    //        //given
    //        given(couponCampaign.getValidTerm()).willReturn(validTerm);
    //        //LocalDateTime localDateTime = Mockito.mock(new
    // TypeToken<IssuedCoupon>(){}.getCreatedAt());
    //        given(issuedCoupon.getCreatedAt()).willReturn(createdAt);
    //        //when
    //        Boolean result = issuedCoupon.isAvailableTerm();
    //        //then
    //        assertTrue(result);
    //    }
    //
    //    @Test
    //    public void testCalculateValidTerm() {
    //        // given
    //        given(couponCampaign.getValidTerm()).willReturn(validTerm);
    //        given(issuedCoupon.getCreatedAt()).willReturn(createdAt);
    //        // when
    //        LocalDateTime result = issuedCoupon.calculateValidTerm();
    //        // then
    //        assertEquals(result, createdAt.plusDays(validTerm));
    //    }

    @Test
    void testGetDiscountAmount_withAmountDiscountType() {
        given(couponCampaign.getDiscountType()).willReturn(DiscountType.AMOUNT);
        given(couponCampaign.getDiscountAmount()).willReturn(discountAmount);
        given(couponCampaign.getMinimumCost()).willReturn(minimumCost);

        Money supplyAmount = Money.wons(60000L);
        Money expectedDiscountAmount = Money.wons(10000L);
        Money actualDiscountAmount = issuedCoupon.getDiscountAmount(supplyAmount);

        assertEquals(expectedDiscountAmount, actualDiscountAmount);
    }

    @Test
    void testGetDiscountAmount_withPercentageDiscountType() {
        given(couponCampaign.getDiscountType()).willReturn(DiscountType.PERCENTAGE);
        given(couponCampaign.getDiscountAmount()).willReturn(discountPercentage);
        given(couponCampaign.getMinimumCost()).willReturn(minimumCost);

        Money supplyAmount = Money.wons(60000L);
        Money expectedDiscountAmount = Money.wons(18000L);
        Money actualDiscountAmount = issuedCoupon.getDiscountAmount(supplyAmount);

        assertEquals(expectedDiscountAmount, actualDiscountAmount);
    }

    @Test
    void testCheckSupplyAmount_withLessThanDiscount() {
        Money supply = Money.wons(5000L);
        Long discount = discountAmount;
        Long minimum = minimumCost;

        assertThrows(
                SupplyLessThenDiscountException.class,
                () -> issuedCoupon.checkSupplyAmount(supply, discount, minimum));
    }

    @Test
    void testCheckSupplyAmount_withLessThanMinimum() {
        Money supply = Money.wons(40000L);
        Long discount = discountAmount;
        Long minimum = minimumCost;

        assertThrows(
                SupplyLessThenMinimumException.class,
                () -> issuedCoupon.checkSupplyAmount(supply, discount, minimum));
    }

    @Test
    void testCheckSupplyAmount_withValidSupply() {
        Money supply = Money.wons(60000L);
        Long discount = discountAmount;
        Long minimum = minimumCost;

        Money expectedDiscountAmount = Money.wons(10000L);
        Money actualDiscountAmount = issuedCoupon.checkSupplyAmount(supply, discount, minimum);

        assertEquals(expectedDiscountAmount, actualDiscountAmount);
    }

    @Test
    public void recovery_usageStatusFalse() {
        // 쿠폰 사용 안한 상태
        assertThrows(AlreadyRecoveredCouponException.class, () -> issuedCoupon.recovery());
    }

    @Test
    public void recovery_usageStatusTrue() {
        // 쿠폰 이미 사용한 상태
        issuedCoupon.use();
        issuedCoupon.recovery();
        // then
        assertFalse(issuedCoupon.getUsageStatus());
    }
}
