package band.gosrock.infrastructure.outer.api.tossPayments.dto.request

// 부분 취소 안합니다. 전체 금액 취소입니다.
data class CancelPaymentsRequest(val cancelReason: String? = null)
