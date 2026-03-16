package band.gosrock.domain.domains.coupon.domain

import band.gosrock.domain.domains.coupon.exception.NoCouponStockLeftException
import jakarta.persistence.Embeddable

@Embeddable
class CouponStockInfo() {

    var issuedAmount: Long? = null
        protected set

    var remainingAmount: Long? = null
        protected set

    constructor(issuedAmount: Long?, remainingAmount: Long?) : this() {
        this.issuedAmount = issuedAmount
        this.remainingAmount = remainingAmount
    }

    fun checkCouponLeft() {
        if (remainingAmount!! < 1) {
            throw NoCouponStockLeftException.EXCEPTION
        }
    }

    fun decreaseCouponStock() {
        checkCouponLeft()
        this.remainingAmount = remainingAmount!! - 1
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var issuedAmount: Long? = null
        private var remainingAmount: Long? = null

        fun issuedAmount(issuedAmount: Long?) = apply { this.issuedAmount = issuedAmount }
        fun remainingAmount(remainingAmount: Long?) = apply { this.remainingAmount = remainingAmount }

        fun build(): CouponStockInfo = CouponStockInfo(issuedAmount, remainingAmount)
    }
}
