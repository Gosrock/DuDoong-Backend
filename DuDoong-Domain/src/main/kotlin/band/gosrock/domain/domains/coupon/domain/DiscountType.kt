package band.gosrock.domain.domains.coupon.domain

import band.gosrock.common.annotation.EnumClass

@EnumClass
enum class DiscountType(val value: String) {
    AMOUNT("AMOUNT"),
    PERCENTAGE("PERCENTAGE")
}
