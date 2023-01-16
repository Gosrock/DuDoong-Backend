package band.gosrock.api.coupon.controller;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.coupon.dto.reqeust.*;
import band.gosrock.api.coupon.dto.response.*;
import band.gosrock.api.coupon.service.CreateCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "쿠폰 관련 컨트롤러")
@RestController
@RequestMapping("/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;

    @Operation(summary = "쿠폰 캠페인 생성 API")
    @PostMapping("/campaigns")
    public CreateCouponCampaignResponse createCouponCampaign(
            @RequestBody @Valid CreateCouponCampaignRequest createCouponCampaignRequest) {
        Long userId = SecurityUtils.getCurrentUserId();
        return createCouponUseCase.execute(userId, createCouponCampaignRequest);
    }
}
