package band.gosrock.admin.model.dto.request

data class AdminAdjustTicketStockRequest(
    val delta: Long, // 양수 = 증가, 음수 = 감소
)
