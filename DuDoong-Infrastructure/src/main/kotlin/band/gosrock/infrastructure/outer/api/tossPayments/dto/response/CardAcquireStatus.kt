package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

enum class CardAcquireStatus(val value: String) {
    READY("READY"),
    REQUESTED("REQUESTED"),
    COMPLETED("COMPLETED"),
    CANCEL_REQUESTED("CANCEL_REQUESTED"),
    CANCELED("CANCELED"),
}
