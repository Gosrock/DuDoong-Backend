package band.gosrock.domain.domains.coupon.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.coupon.exception.AlreadyRecoveredCouponException
import band.gosrock.domain.domains.coupon.exception.AlreadyUsedCouponException
import band.gosrock.domain.domains.coupon.exception.NotMyCouponException
import band.gosrock.domain.domains.coupon.exception.SupplyLessThenDiscountException
import band.gosrock.domain.domains.coupon.exception.SupplyLessThenMinimumException
import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity(name = "tbl_issued_coupon")
class IssuedCoupon() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_coupon_id")
    var id: Long? = null
        protected set

    var userId: Long? = null
        protected set

    var usageStatus: Boolean = false
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_campaign_id", nullable = false)
    var couponCampaign: CouponCampaign? = null
        protected set

    constructor(couponCampaign: CouponCampaign?, userId: Long?) : this() {
        this.couponCampaign = couponCampaign
        this.userId = userId
        this.usageStatus = false
    }

    fun getIssuedCouponId(): Long? = this.id

    fun getCouponName(): String? = this.couponCampaign?.couponCode

    fun getDiscountAmount(supplyAmount: Money): Money {
        return if (couponCampaign!!.discountType == DiscountType.AMOUNT) {
            checkSupplyAmount(supplyAmount, couponCampaign!!.discountAmount!!, couponCampaign!!.minimumCost!!)
        } else {
            val discountAmount = supplyAmount.getDiscountAmountByPercentage(supplyAmount, couponCampaign!!.discountAmount!!)
            checkSupplyAmount(supplyAmount, discountAmount, couponCampaign!!.minimumCost!!)
        }
    }

    fun checkSupplyAmount(supply: Money, discount: Long, minimum: Long): Money {
        if (supply.isLessThan(Money.wons(discount))) {
            throw SupplyLessThenDiscountException.EXCEPTION
        }
        if (supply.isLessThan(Money.wons(minimum))) {
            throw SupplyLessThenMinimumException.EXCEPTION
        }
        return Money.wons(discount)
    }

    fun validMine(userId: Long) {
        if (userId != this.userId) {
            throw NotMyCouponException.EXCEPTION
        }
    }

    fun use() {
        if (usageStatus) {
            throw AlreadyUsedCouponException.EXCEPTION
        }
        usageStatus = true
    }

    fun recovery() {
        if (!usageStatus) {
            throw AlreadyRecoveredCouponException.EXCEPTION
        }
        usageStatus = false
    }

    fun isAvailableTerm(): Boolean =
        !LocalDateTime.now().isAfter(createdAtKt().plusDays(this.couponCampaign!!.validTerm!!))

    fun calculateValidTerm(): LocalDateTime =
        createdAtKt().plusDays(this.couponCampaign!!.validTerm!!)

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var couponCampaign: CouponCampaign? = null
        private var userId: Long? = null

        fun couponCampaign(couponCampaign: CouponCampaign?) = apply { this.couponCampaign = couponCampaign }
        fun userId(userId: Long?) = apply { this.userId = userId }

        fun build(): IssuedCoupon = IssuedCoupon(couponCampaign, userId)
    }
}
