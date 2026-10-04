package band.gosrock.domain.domains.gift.exception

import band.gosrock.common.exception.DuDoongCodeException

/** 티켓 선물 예외 (#719) */
class GiftNotFoundException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftNotFoundException()
    }
}

class GiftNotGiftableException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_NOT_GIFTABLE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftNotGiftableException()
    }
}

class GiftAlreadyPendingException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_ALREADY_PENDING) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftAlreadyPendingException()
    }
}

class GiftNotPendingException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_NOT_PENDING) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftNotPendingException()
    }
}

class GiftOwnLinkException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_OWN_LINK) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftOwnLinkException()
    }
}

class GiftExpiredException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_EXPIRED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftExpiredException()
    }
}

class GiftOrderInvalidException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_ORDER_INVALID) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftOrderInvalidException()
    }
}

class GiftCannotReturnException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_CANNOT_RETURN) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftCannotReturnException()
    }
}

class GiftNotReceivedTicketException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_NOT_RECEIVED_TICKET) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftNotReceivedTicketException()
    }
}

class GiftInvalidMemoException private constructor() : DuDoongCodeException(TicketGiftErrorCode.GIFT_INVALID_MEMO) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = GiftInvalidMemoException()
    }
}
