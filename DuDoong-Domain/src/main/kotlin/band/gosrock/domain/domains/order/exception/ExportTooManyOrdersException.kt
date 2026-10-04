package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class ExportTooManyOrdersException private constructor() : DuDoongCodeException(OrderErrorCode.EXPORT_TOO_MANY_ORDERS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ExportTooManyOrdersException()
    }
}
