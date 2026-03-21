package band.gosrock.admin.model.dto.response

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import java.time.LocalDateTime

data class AdminIssuedTicketResponse(
    val id: Long,
    val issuedTicketNo: String?,
    val userName: String?,
    val ticketName: String?,
    val orderUuid: String?,
    val enteredAt: LocalDateTime?,
    val status: IssuedTicketStatus,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(issuedTicket: IssuedTicket): AdminIssuedTicketResponse =
            AdminIssuedTicketResponse(
                id = issuedTicket.id!!,
                issuedTicketNo = issuedTicket.issuedTicketNo,
                userName = issuedTicket.userInfo?.userName,
                ticketName = issuedTicket.itemInfo?.ticketName,
                orderUuid = issuedTicket.orderUuid,
                enteredAt = issuedTicket.enteredAt,
                status = issuedTicket.issuedTicketStatus,
                createdAt = issuedTicket.createdAt,
            )
    }
}
