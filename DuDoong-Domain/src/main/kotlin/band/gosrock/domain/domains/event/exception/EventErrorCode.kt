package band.gosrock.domain.domains.event.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field

enum class EventErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    EVENT_NOT_FOUND(NOT_FOUND, "Event_404_1", "이벤트를 찾을 수 없습니다."),

    HOST_NOT_AUTH_EVENT(BAD_REQUEST, "Event_400_1", "Host Not Auth Event."),
    EVENT_CANNOT_END_BEFORE_START(BAD_REQUEST, "Event_400_2", "시작 시각은 종료 시각보다 빨라야 합니다."),
    EVENT_URL_NAME_ALREADY_EXIST(BAD_REQUEST, "Event_400_3", "중복된 URL 표시 이름입니다."),
    CANNOT_MODIFY_OPEN_EVENT(BAD_REQUEST, "Event_400_4", "오픈된 이벤트 정보는 수정할 수 없습니다."),
    EVENT_NOT_OPEN(BAD_REQUEST, "Event_400_5", "아직 오픈되지 않은 이벤트에는 접근할 수 없습니다."),
    EVENT_TICKETING_TIME_IS_PASSED(BAD_REQUEST, "Event_400_6", "이벤트 시작시간이 지나 티켓팅을 할 수 없습니다."),
    CANNOT_OPEN_EVENT(BAD_REQUEST, "Event_400_7", "이벤트 오픈 조건을 충족하지 않았습니다."),
    ALREADY_OPEN_STATUS(BAD_REQUEST, "Event_400_8", "이미 오픈 중인 이벤트입니다."),
    ALREADY_CALCULATING_STATUS(BAD_REQUEST, "Event_400_9", "이미 정산중인 이벤트입니다."),
    ALREADY_CLOSE_STATUS(BAD_REQUEST, "Event_400_10", "이미 닫은 이벤트입니다."),
    ALREADY_PREPARING_STATUS(BAD_REQUEST, "Event_400_11", "이미 준비중인 이벤트입니다."),
    ALREADY_DELETED_STATUS(BAD_REQUEST, "Event_400_12", "이미 삭제된 이벤트입니다."),
    CANNOT_DELETE_BY_ISSUED_TICKET(BAD_REQUEST, "Event_400_13", "발급 티켓이 있는 이벤트는 삭제할 수 없습니다."),
    CANNOT_DELETE_BY_OPEN_EVENT(BAD_REQUEST, "Event_400_14", "오픈 상태인 이벤트는 삭제할 수 없습니다."),
    OPEN_TIME_EXPIRED(BAD_REQUEST, "Event_400_15", "오픈 예정 시간이 현재 시간보다 빠릅니다."),

    INVALID_EVENT_STATUS_TRANSITION(BAD_REQUEST, "Event_400_16", "허용되지 않는 상태 전이입니다."),

    // v2
    @ExplainError("정산중(CALCULATING) / 지난공연(CLOSED) 공연의 기본 정보·섹션·이미지를 수정하려는 경우. 준비중·등록된(OPEN) 공연만 수정할 수 있습니다.")
    CANNOT_MODIFY_ENDED_EVENT(BAD_REQUEST, "Event_400_17", "종료된 공연은 수정할 수 없습니다."),
    @ExplainError("등록(OPEN) 이후 티켓 여부(hasTicket)를 바꾸려는 경우 (DEC-007). 같은 값은 허용")
    CANNOT_CHANGE_HAS_TICKET(BAD_REQUEST, "Event_400_18", "등록된 공연은 티켓 여부를 바꿀 수 없습니다."),
    @ExplainError("준비중이 아닌 공연을 v2 삭제 API 로 삭제하려는 경우")
    CANNOT_DELETE_NOT_PREPARING_EVENT(BAD_REQUEST, "Event_400_19", "준비중인 공연만 삭제할 수 있습니다."),
    @ExplainError("문의처가 최대 개수를 넘거나 값이 비었거나 너무 긴 경우")
    INVALID_EVENT_CONTACT(BAD_REQUEST, "Event_400_20", "문의처 형식이 올바르지 않습니다."),
    @ExplainError("섹션이 0개이거나 최대 개수 초과, 제목이 비었거나 20자 초과, 본문이 너무 긴 경우")
    INVALID_EVENT_SECTION(BAD_REQUEST, "Event_400_21", "섹션 형식이 올바르지 않습니다."),
    @ExplainError("이미지 key 가 빈 문자열이 아니면서 이 공연의 이미지 업로드 API 가 발급한 경로(event/{eventId}/)로 시작하지 않는 경우")
    INVALID_EVENT_IMAGE_KEY(BAD_REQUEST, "Event_400_22", "이 공연에 업로드한 이미지가 아닙니다."),
    @ExplainError("존재하지 않는 태그 id 가 있거나 태그 최대 개수를 넘는 경우")
    INVALID_EVENT_TAG(BAD_REQUEST, "Event_400_23", "태그가 올바르지 않습니다."),
    @ExplainError("등록(OPEN)된 공연의 시작 시각을 현재 이전으로 바꾸려는 경우 (같은 값은 허용)")
    CANNOT_MOVE_OPEN_EVENT_START_TO_PAST(BAD_REQUEST, "Event_400_24", "등록된 공연의 시작 시각은 현재 이후여야 합니다."),
    @ExplainError("유효 티켓이 있는 공연을 '티켓 없음'(hasTicket=false) 으로 바꾸려는 경우")
    CANNOT_DISABLE_TICKET_WITH_TICKETS(BAD_REQUEST, "Event_400_25", "티켓이 있는 공연은 티켓 없음으로 바꿀 수 없습니다."),
    @ExplainError("셀프 체크인 QR 토큰이 어느 공연(삭제 제외)의 토큰과도 일치하지 않는 경우")
    INVALID_CHECK_IN_TOKEN(BAD_REQUEST, "Event_400_26", "유효하지 않은 체크인 QR 입니다."),

    USE_OTHER_API(BAD_REQUEST, "Event_400_8", "잘못된 접근입니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation: ExplainError? = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: this.reason
    }
}
