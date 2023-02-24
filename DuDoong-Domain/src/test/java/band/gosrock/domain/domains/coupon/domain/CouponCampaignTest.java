package band.gosrock.domain.domains.coupon.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import band.gosrock.domain.common.vo.DateTimePeriod;
import band.gosrock.domain.domains.coupon.exception.NotIssuingCouponPeriodException;
import band.gosrock.domain.domains.coupon.exception.WrongDiscountAmountException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CouponCampaignTest {
    CouponCampaign couponCampaign;

    @BeforeEach
    void setUp() {
        LocalDateTime nowTime = LocalDateTime.now();
        DateTimePeriod dateTimePeriod =
                new DateTimePeriod(nowTime.minusDays(2), nowTime.minusDays(1));

        CouponStockInfo couponStockInfo =
                CouponStockInfo.builder().issuedAmount(3L).remainingAmount(1L).build();
        couponCampaign =
                CouponCampaign.builder()
                        .discountType(DiscountType.PERCENTAGE)
                        .couponStockInfo(couponStockInfo)
                        .dateTimePeriod(dateTimePeriod)
                        .build();
    }

    @Test
    public void testValidatePercentageAmount() {
        assertThrows(
                WrongDiscountAmountException.class,
                () -> {
                    couponCampaign.validatePercentageAmount(DiscountType.PERCENTAGE, 101L);
                });
    }

    @Test
    public void testDecreaseCouponStock() {
        // given
        // when
        couponCampaign.decreaseCouponStock();
        // then
        assertEquals(couponCampaign.getCouponStockInfo().getRemainingAmount(), 0L);
    }

    @Test
    public void testValidateIssuePeriod() {
        assertThrows(
                NotIssuingCouponPeriodException.class,
                () -> {
                    couponCampaign.validateIssuePeriod();
                });
    }
}
