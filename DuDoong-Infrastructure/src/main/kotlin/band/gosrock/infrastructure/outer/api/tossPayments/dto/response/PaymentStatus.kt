package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

enum class PaymentStatus(val value: String) {
    READY("READY"),
    IN_PROGRESS("IN_PROGRESS"),
    WAITING_FOR_DEPOSIT("WAITING_FOR_DEPOSIT"),
    DONE("DONE"),
    CANCELED("CANCELED"),
    PARTIAL_CANCELED("PARTIAL_CANCELED"),
    ABORTED("ABORTED"),
    EXPIRED("EXPIRED"),
}
