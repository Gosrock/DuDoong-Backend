package band.gosrock.api.issuedTicket.dto.response

import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.common.vo.IssuedTicketOptionAnswerVo
import band.gosrock.domain.common.vo.UserInfoVo
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class IssuedTicketAdminTableElement(
    @JsonUnwrapped val issuedTicketInfo: IssuedTicketInfoVo,
    val userInfo: UserInfoVo,
    val orderNo: String?,
    val issuedTicketOptionAnswers: List<IssuedTicketOptionAnswerVo>,
) {
    companion object {
        @JvmStatic
        fun of(issuedTicket: IssuedTicket, user: User, order: Order): IssuedTicketAdminTableElement {
            return IssuedTicketAdminTableElement(
                issuedTicketInfo = issuedTicket.toIssuedTicketInfoVo(),
                userInfo = user.toUserInfoVo(),
                orderNo = order.orderNo,
                issuedTicketOptionAnswers = issuedTicket.issuedTicketOptionAnswers
                    .map(IssuedTicketOptionAnswer::toIssuedTicketOptionAnswerVo),
            )
        }
    }
}
