package band.gosrock.domain.domains.issuedTicket.dto.response

import band.gosrock.domain.common.vo.EventInfoVo
import band.gosrock.domain.common.vo.IssuedTicketInfoVo

class IssuedTicketDTO private constructor(
    val issuedTicketInfo: IssuedTicketInfoVo,
    val eventInfo: EventInfoVo,
) {
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var issuedTicketInfo: IssuedTicketInfoVo? = null
        private var eventInfo: EventInfoVo? = null

        fun issuedTicketInfo(issuedTicketInfo: IssuedTicketInfoVo) = apply { this.issuedTicketInfo = issuedTicketInfo }
        fun eventInfo(eventInfo: EventInfoVo) = apply { this.eventInfo = eventInfo }

        fun build(): IssuedTicketDTO = IssuedTicketDTO(issuedTicketInfo!!, eventInfo!!)
    }
}
