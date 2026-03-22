package band.gosrock.api.coupon.mapper

import band.gosrock.api.coupon.dto.reqeust.CreateCouponCampaignRequest
import band.gosrock.api.coupon.dto.response.CreateCouponCampaignResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.common.vo.DateTimePeriod
import band.gosrock.domain.domains.coupon.domain.CouponCampaign
import band.gosrock.domain.domains.coupon.domain.CouponStockInfo
import java.time.LocalDateTime

@Mapper
class CouponCampaignMapper {

    fun toEntity(createCouponCampaignRequest: CreateCouponCampaignRequest, userId: Long): CouponCampaign {
        val couponStockInfo = toCouponStockInfo(createCouponCampaignRequest.issuedAmount)
        val dateTimePeriod = toDateTimePeriod(createCouponCampaignRequest.startAt, createCouponCampaignRequest.endAt)

        return CouponCampaign(
            userId = userId,
            discountType = createCouponCampaignRequest.discountType,
            applyTarget = createCouponCampaignRequest.applyTarget,
            validTerm = createCouponCampaignRequest.validTerm,
            dateTimePeriod = dateTimePeriod,
            couponStockInfo = couponStockInfo,
            discountAmount = createCouponCampaignRequest.discountAmount,
            couponCode = createCouponCampaignRequest.couponCode,
            minimumCost = createCouponCampaignRequest.minimumCost,
        )
    }

    companion object {
        fun toCreateCouponCampaignResponse(couponCampaign: CouponCampaign, userId: Long): CreateCouponCampaignResponse {
            return CreateCouponCampaignResponse(
                couponCampaignId = couponCampaign.id!!,
                couponCode = couponCampaign.couponCode!!,
                issuedAmount = couponCampaign.couponStockInfo!!.issuedAmount!!,
                userId = userId,
            )
        }

        fun toCouponStockInfo(issuedAmount: Long): CouponStockInfo {
            return CouponStockInfo(
                issuedAmount = issuedAmount,
                remainingAmount = issuedAmount,
            )
        }

        fun toDateTimePeriod(startAt: LocalDateTime, endAt: LocalDateTime): DateTimePeriod {
            return DateTimePeriod(startAt, endAt)
        }
    }
}
