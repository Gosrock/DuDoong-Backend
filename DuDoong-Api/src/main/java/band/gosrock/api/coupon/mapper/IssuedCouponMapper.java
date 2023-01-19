package band.gosrock.api.coupon.mapper;


import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import lombok.RequiredArgsConstructor;

@Mapper
@RequiredArgsConstructor
public class IssuedCouponMapper {

    private final IssuedCouponAdaptor issuedCouponAdaptor;
}
