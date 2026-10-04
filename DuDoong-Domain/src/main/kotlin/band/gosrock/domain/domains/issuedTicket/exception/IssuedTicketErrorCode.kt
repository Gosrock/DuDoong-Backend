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
    ISSUED_TICKET_NOT_MATCHED_EVENT(BAD_REQUEST, "IssuedTicket_400_6", "이 티켓은 해당 이벤트에서 발급된 티켓이 아닙니다."),

    // v2
    @ExplainError("v2 발급 티켓 엑셀 다운로드 대상이 행 상한(10,000)을 넘는 경우. 입장 필터·검색어로 줄여서 받는다")
    EXPORT_TOO_MANY_ISSUED_TICKETS(BAD_REQUEST, "IssuedTicket_400_7", "엑셀로 내려받을 티켓이 너무 많습니다. 필터로 줄여 주세요."),

    @ExplainError("선물 대기 중인 티켓 (#719). v1 입장 처리·v1 티켓 상세에서 거부. 보낸 사람이 선물을 취소(회수)하면 다시 쓸 수 있다")
    ISSUED_TICKET_GIFT_PENDING(BAD_REQUEST, "IssuedTicket_400_8", "선물 대기 중인 티켓입니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation: ExplainError? = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: this.reason
    }
}
