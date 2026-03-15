package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.domains.event.domain.Event

class IssuedTickets(val issuedTickets: List<IssuedTicket>) {

    companion object {
        @JvmStatic
        fun from(issuedTickets: List<IssuedTicket>): IssuedTickets = IssuedTickets(issuedTickets)
    }

    fun getNos(): List<String?> = issuedTickets.map { it.issuedTicketNo }

    fun getTotalQuantity(): Int = issuedTickets.size

    fun getTicketNoName(): String {
        val nos = getNos()
        val size = nos.size
        return when {
            size == 0 -> ""
            size == 1 -> String.format("%s (%d매)", nos[0], size)
            else -> String.format("%s ~ %s (%d매)", nos[0], nos[size - 1], size)
        }
    }

    fun getIssuedTicketsStage(event: Event): IssuedTicketsStage {
        if (getTotalQuantity() == 0) return IssuedTicketsStage.APPROVE_WAITING
        val issuedTicketStatuses = getIssuedTicketStatuses()
        if (isCanceled(issuedTicketStatuses)) return IssuedTicketsStage.CANCELED
        if (event.isClosed()) return IssuedTicketsStage.PASSED_EVENT
        if (isBeforeEntrance(issuedTicketStatuses)) return IssuedTicketsStage.BEFORE_ENTRANCE
        if (isAfterEntrance(issuedTicketStatuses)) return IssuedTicketsStage.AFTER_ENTRANCE
        return IssuedTicketsStage.ENTERING
    }

    fun getIssuedTicketStatuses(): List<IssuedTicketStatus> =
        issuedTickets.map { it.issuedTicketStatus }

    fun getIssuedTicketInfoVos(): List<IssuedTicketInfoVo> =
        issuedTickets.map { it.toIssuedTicketInfoVo() }

    private fun isCanceled(statuses: List<IssuedTicketStatus>): Boolean =
        statuses.any { it.isCanceled() }

    private fun isBeforeEntrance(statuses: List<IssuedTicketStatus>): Boolean =
        statuses.all { it.isBeforeEntrance() }

    private fun isAfterEntrance(statuses: List<IssuedTicketStatus>): Boolean =
        statuses.all { it.isAfterEntrance() }
}
