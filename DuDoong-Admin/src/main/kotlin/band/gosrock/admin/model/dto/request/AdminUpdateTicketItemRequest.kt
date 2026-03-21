package band.gosrock.admin.model.dto.request

import java.math.BigDecimal

data class AdminUpdateTicketItemRequest(
    val name: String? = null,
    val description: String? = null,
    val price: BigDecimal? = null,
    val quantity: Long? = null,
    val purchaseLimit: Long? = null,
)
