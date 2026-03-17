package band.gosrock.api.coupon.controller

import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.api.coupon.dto.reqeust.CreateCouponCampaignRequest
import band.gosrock.api.coupon.dto.response.CreateCouponCampaignResponse
import band.gosrock.api.coupon.dto.response.CreateUserCouponResponse
import band.gosrock.api.coupon.dto.response.ReadIssuedCouponResponse
import band.gosrock.api.coupon.service.CreateCouponUseCase
import band.gosrock.api.coupon.service.CreateUserCouponUseCase
import band.gosrock.api.coupon.service.ReadIssuedCouponUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "a0. [쿠폰]")
@RestController
@RequestMapping("/api/v1/coupons")
class CouponController(
    private val createCouponUseCase: CreateCouponUseCase,
    private val createUserCouponUseCase: CreateUserCouponUseCase,
    private val readIssuedCouponUseCase: ReadIssuedCouponUseCase,
) {
    @Operation(summary = "쿠폰 캠페인 생성 API")
    @PostMapping("/campaigns")
    fun createCouponCampaign(
        @CurrentUserId userId: Long,
        @RequestBody @Valid createCouponCampaignRequest: CreateCouponCampaignRequest,
    ): CreateCouponCampaignResponse {
        return createCouponUseCase.execute(userId, createCouponCampaignRequest)
    }

    @Operation(summary = "유저 쿠폰 발급 API")
    @PostMapping("/campaigns/{coupon_code}")
    fun createUserCoupon(
        @CurrentUserId userId: Long,
        @PathVariable("coupon_code") couponCode: String,
    ): CreateUserCouponResponse {
        return createUserCouponUseCase.execute(userId, couponCode)
    }

    @Operation(summary = "내 쿠폰 조회 API")
    @GetMapping("")
    fun getAllMyIssuedCoupons(
        @CurrentUserId userId: Long,
        @RequestParam(required = false, defaultValue = "true") expired: Boolean,
    ): ReadIssuedCouponResponse {
        return readIssuedCouponUseCase.execute(userId, expired)
    }
}
