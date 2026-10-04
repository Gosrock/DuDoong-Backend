package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class ExportTooManyIssuedTicketsException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.EXPORT_TOO_MANY_ISSUED_TICKETS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ExportTooManyIssuedTicketsException()
    }
}
