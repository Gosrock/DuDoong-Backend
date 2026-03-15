package band.gosrock.domain.common.vo

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import java.time.LocalDateTime

data class IssuedTicketInfoVo(
    /** 발급 티켓 id */
    val issuedTicketId: Long? = null,
    /** 발급 티켓 번호 Ex. T10000001 */
    val issuedTicketNo: String? = null,
    /** 발급 티켓 uuid */
    val uuid: String? = null,
    /** 발급 티켓 종류 */
    val ticketName: String? = null,
    /** 발급 티켓 지불 방식 */
    val payType: TicketPayType? = null,
    /** 발급 티켓 가격 */
    val ticketPrice: Money? = null,
    /** 티켓 발급 시간 */
    @DateFormat val createdAt: LocalDateTime? = null,
    @DateFormat val enteredAt: LocalDateTime? = null,
    /** 발급 티켓 상태 */
    val issuedTicketStatus: IssuedTicketStatus? = null,
    /** 발급 티켓 옵션 금액 합계 */
    val optionPrice: Money? = null,
) {
    companion object {
        @JvmStatic
        fun from(issuedTicket: IssuedTicket): IssuedTicketInfoVo =
            IssuedTicketInfoVo(
                issuedTicketId = issuedTicket.id,
                issuedTicketNo = issuedTicket.issuedTicketNo,
                uuid = issuedTicket.uuid,
                ticketName = issuedTicket.itemInfo?.ticketName,
                payType = issuedTicket.itemInfo?.payType,
                ticketPrice = issuedTicket.itemInfo?.price,
                createdAt = issuedTicket.createdAt,
                issuedTicketStatus = issuedTicket.issuedTicketStatus,
                optionPrice = issuedTicket.sumOptionPrice(),
                enteredAt = issuedTicket.enteredAt,
            )
    }
}
