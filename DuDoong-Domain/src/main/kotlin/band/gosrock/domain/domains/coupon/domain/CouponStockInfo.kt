package band.gosrock.domain.domains.coupon.domain

import band.gosrock.domain.domains.coupon.exception.NoCouponStockLeftException
import jakarta.persistence.Embeddable

@Embeddable
class CouponStockInfo(
    var issuedAmount: Long? = null,
    var remainingAmount: Long? = null,
) {
    fun checkCouponLeft() {
        if (remainingAmount!! < 1) {
            throw NoCouponStockLeftException.EXCEPTION
        }
    }

    fun decreaseCouponStock() {
        checkCouponLeft()
        this.remainingAmount = remainingAmount!! - 1
    }
}
