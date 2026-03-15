package band.gosrock.domain.domains.coupon.domain

import band.gosrock.common.annotation.EnumClass

@EnumClass
enum class ApplyTarget(val value: String) {
    ALL("ALL"),
    SUB("SUB")
}
