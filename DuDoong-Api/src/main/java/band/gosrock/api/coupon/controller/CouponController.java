package band.gosrock.api.coupon.controller;


import band.gosrock.api.coupon.dto.reqeust.*;
import band.gosrock.api.coupon.dto.response.*;
import band.gosrock.api.coupon.service.CreateCouponUseCase;
import band.gosrock.api.coupon.service.CreateUserCouponUseCase;
import band.gosrock.api.coupon.service.ReadIssuedCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@Tag(name = "a0. 쿠폰 관련 컨트롤러")
@RestController
@RequestMapping("/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;
    private final CreateUserCouponUseCase createUserCouponUseCase;
    private final ReadIssuedCouponUseCase readIssuedCouponUseCase;

    @Operation(summary = "쿠폰 캠페인 생성 API")
    @PostMapping("/campaigns")
    public CreateCouponCampaignResponse createCouponCampaign(
            @RequestBody @Valid CreateCouponCampaignRequest createCouponCampaignRequest) {
        return createCouponUseCase.execute(createCouponCampaignRequest);
    }

    @Operation(summary = "유저 쿠폰 발급 API")
    @PostMapping("/campaigns/{coupon_code}")
    public CreateUserCouponResponse createUserCoupon(
            @PathVariable("coupon_code") String couponCode) {
        return createUserCouponUseCase.execute(couponCode);
    }

    @Operation(summary = "내 쿠폰 조회 API")
    @GetMapping("")
    public ReadIssuedCouponResponse getAllMyIssuedCoupons(
            @RequestParam(required = false, defaultValue = "true") Boolean expired) {
        return readIssuedCouponUseCase.execute(expired);
    }
}
