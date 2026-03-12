package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class OrdeItemNotOneTypeException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_INVALID_ITEM_KIND_POLICY) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OrdeItemNotOneTypeException()
    }
}
