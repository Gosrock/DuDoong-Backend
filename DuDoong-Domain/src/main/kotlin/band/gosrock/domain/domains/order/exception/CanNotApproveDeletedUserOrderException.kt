package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class CanNotApproveDeletedUserOrderException private constructor() : DuDoongCodeException(OrderErrorCode.CAN_NOT_DELETED_USER_APPROVE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CanNotApproveDeletedUserOrderException()
    }
}
