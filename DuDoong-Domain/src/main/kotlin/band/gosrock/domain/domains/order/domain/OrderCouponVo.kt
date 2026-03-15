package band.gosrock.domain.domains.order.domain

import band.gosrock.common.consts.DuDoongStatic.MINIMUM_PAYMENT_WON
import band.gosrock.common.consts.DuDoongStatic.ZERO
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon
import band.gosrock.domain.domains.order.exception.LessThanMinmumPaymentOrderException
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class OrderCouponVo() {

    @Column(name = "coupon_name")
    var name: String = "사용하지 않음"
        protected set

    var discountAmount: Money = Money.ZERO
        protected set

    var couponId: Long = ZERO
        protected set

    companion object {
        @JvmStatic
        fun of(coupon: IssuedCoupon, orderSupplyAmount: Money): OrderCouponVo =
            OrderCouponVo().apply {
                couponId = coupon.getIssuedCouponId()!!
                discountAmount = coupon.getDiscountAmount(orderSupplyAmount)
                name = coupon.getCouponName()!!
            }

        @JvmStatic
        fun empty(): OrderCouponVo = OrderCouponVo()
    }

    fun validMinimumPaymentAmount(supplyAmount: Money) {
        val paymentAmount = supplyAmount.minus(discountAmount)
        if (paymentAmount != Money.ZERO && paymentAmount.isLessThan(Money.wons(MINIMUM_PAYMENT_WON))) {
            throw LessThanMinmumPaymentOrderException.EXCEPTION
        }
    }

    fun isDefault(): Boolean = couponId == ZERO
}
