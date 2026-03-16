package band.gosrock.domain.domains.coupon.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.DateTimePeriod
import band.gosrock.domain.domains.coupon.exception.NotIssuingCouponPeriodException
import band.gosrock.domain.domains.coupon.exception.WrongDiscountAmountException
import java.time.LocalDateTime
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.DynamicInsert

@DynamicInsert
@Entity(name = "tbl_coupon_campaign")
class CouponCampaign() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_campaign_id")
    var id: Long? = null
        protected set

    var userId: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    var discountType: DiscountType? = null
        protected set

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ALL'")
    var applyTarget: ApplyTarget? = null
        protected set

    var validTerm: Long? = null
        protected set

    @Embedded
    var dateTimePeriod: DateTimePeriod? = null
        protected set

    @Embedded
    var couponStockInfo: CouponStockInfo? = null
        protected set

    var discountAmount: Long? = null
        protected set

    var couponCode: String? = null
        protected set

    var minimumCost: Long? = 10000L
        protected set

    @OneToMany(mappedBy = "couponCampaign", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var issuedCoupons: MutableList<IssuedCoupon> = mutableListOf()
        protected set

    constructor(
        userId: Long?,
        discountType: DiscountType?,
        applyTarget: ApplyTarget?,
        validTerm: Long?,
        dateTimePeriod: DateTimePeriod?,
        couponStockInfo: CouponStockInfo?,
        discountAmount: Long?,
        couponCode: String?,
        minimumCost: Long?,
    ) : this() {
        this.userId = userId
        this.discountType = discountType
        this.applyTarget = applyTarget
        this.validTerm = validTerm
        this.dateTimePeriod = dateTimePeriod
        this.couponStockInfo = couponStockInfo
        this.discountAmount = discountAmount
        this.couponCode = couponCode
        this.minimumCost = minimumCost
    }

    fun validatePercentageAmount(discountType: DiscountType, discountAmount: Long) {
        if (discountType == DiscountType.PERCENTAGE && discountAmount > 100) {
            throw WrongDiscountAmountException.EXCEPTION
        }
    }

    fun decreaseCouponStock() {
        couponStockInfo!!.decreaseCouponStock()
    }

    fun validateIssuePeriod() {
        val nowTime = LocalDateTime.now()
        if (!dateTimePeriod!!.contains(nowTime)) {
            throw NotIssuingCouponPeriodException.EXCEPTION
        }
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var userId: Long? = null
        private var discountType: DiscountType? = null
        private var applyTarget: ApplyTarget? = null
        private var validTerm: Long? = null
        private var dateTimePeriod: DateTimePeriod? = null
        private var couponStockInfo: CouponStockInfo? = null
        private var discountAmount: Long? = null
        private var couponCode: String? = null
        private var minimumCost: Long? = null

        fun userId(userId: Long?) = apply { this.userId = userId }
        fun discountType(discountType: DiscountType?) = apply { this.discountType = discountType }
        fun applyTarget(applyTarget: ApplyTarget?) = apply { this.applyTarget = applyTarget }
        fun validTerm(validTerm: Long?) = apply { this.validTerm = validTerm }
        fun dateTimePeriod(dateTimePeriod: DateTimePeriod?) = apply { this.dateTimePeriod = dateTimePeriod }
        fun couponStockInfo(couponStockInfo: CouponStockInfo?) = apply { this.couponStockInfo = couponStockInfo }
        fun discountAmount(discountAmount: Long?) = apply { this.discountAmount = discountAmount }
        fun couponCode(couponCode: String?) = apply { this.couponCode = couponCode }
        fun minimumCost(minimumCost: Long?) = apply { this.minimumCost = minimumCost }

        fun build(): CouponCampaign = CouponCampaign(
            userId, discountType, applyTarget, validTerm, dateTimePeriod,
            couponStockInfo, discountAmount, couponCode, minimumCost,
        )
    }
}
