package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field

enum class IssuedTicketErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    ISSUED_TICKET_NOT_FOUND(NOT_FOUND, "IssuedTicket_404_1", "티켓을 찾을 수 없습니다."),
    ISSUED_TICKET_NOT_MATCHED_USER(BAD_REQUEST, "IssuedTicket_400_1", "IssuedTicket User Not Matched"),
    CAN_NOT_CANCEL(BAD_REQUEST, "IssuedTicket_400_2", "티켓을 취소 할 수 있는 상태가 아닙니다."),
    CAN_NOT_CANCEL_ENTRANCE(BAD_REQUEST, "IssuedTicket_400_3", "티켓이 입장 취소 할 수 있는 상태가 아닙니다."),
    CAN_NOT_ENTRANCE(BAD_REQUEST, "IssuedTicket_400_4", "티켓이 입장 할 수 있는 상태가 아닙니다."),
    ISSUED_TICKET_ALREADY_ENTRANCE(BAD_REQUEST, "IssuedTicket_400_5", "이미 입장 처리된 티켓입니다."),
    ISSUED_TICKET_NOT_MATCHED_EVENT(BAD_REQUEST, "IssuedTicket_400_6", "이 티켓은 해당 이벤트에서 발급된 티켓이 아닙니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation: ExplainError? = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: this.reason
    }
}
