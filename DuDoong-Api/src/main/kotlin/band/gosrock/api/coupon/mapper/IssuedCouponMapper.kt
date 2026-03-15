package band.gosrock.api.coupon.mapper

import band.gosrock.api.coupon.dto.response.CreateUserCouponResponse
import band.gosrock.api.coupon.dto.response.ReadIssuedCouponResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.common.vo.IssuedCouponInfoVo
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor
import band.gosrock.domain.domains.coupon.domain.CouponCampaign
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon

@Mapper
class IssuedCouponMapper(
    private val issuedCouponAdaptor: IssuedCouponAdaptor,
) {
    fun toEntity(couponCampaign: CouponCampaign, userId: Long): IssuedCoupon {
        return IssuedCoupon.builder().couponCampaign(couponCampaign).userId(userId).build()
    }

    fun toCreateUserCouponResponse(issuedCoupon: IssuedCoupon, couponCampaign: CouponCampaign): CreateUserCouponResponse {
        return CreateUserCouponResponse(
            issuedCouponId = issuedCoupon.id!!,
            couponCampaignId = couponCampaign.id!!,
            couponCode = couponCampaign.couponCode!!,
            validTerm = issuedCoupon.calculateValidTerm(),
            discountType = couponCampaign.discountType!!,
            discountAmount = couponCampaign.discountAmount!!,
        )
    }

    fun toReadIssuedCouponMyPageResponse(
        availableIssuedCoupons: List<IssuedCoupon>,
        expiredIssuedCoupons: List<IssuedCoupon>,
    ): ReadIssuedCouponResponse {
        val available = availableIssuedCoupons.ifEmpty { emptyList() }
        return ReadIssuedCouponResponse(
            availableCouponNum = available.size.toLong(),
            availableCouponInfoList = available.map { IssuedCouponInfoVo.of(it) },
            expiredCouponNum = expiredIssuedCoupons.size.toLong(),
            expiredCouponInfoList = expiredIssuedCoupons.map { IssuedCouponInfoVo.of(it) },
        )
    }
}
