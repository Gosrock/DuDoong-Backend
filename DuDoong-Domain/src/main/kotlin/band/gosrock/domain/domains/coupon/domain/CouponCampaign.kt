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
class CouponCampaign(
    var userId: Long? = null,

    @Enumerated(EnumType.STRING)
    var discountType: DiscountType? = null,

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ALL'")
    var applyTarget: ApplyTarget? = null,

    var validTerm: Long? = null,

    @Embedded
    var dateTimePeriod: DateTimePeriod? = null,

    @Embedded
    var couponStockInfo: CouponStockInfo? = null,

    var discountAmount: Long? = null,

    var couponCode: String? = null,

    var minimumCost: Long? = 10000L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_campaign_id")
    var id: Long? = null
        protected set

    @OneToMany(mappedBy = "couponCampaign", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var issuedCoupons: MutableList<IssuedCoupon> = mutableListOf()
        protected set

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
}
