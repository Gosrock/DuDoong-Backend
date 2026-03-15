package band.gosrock.domain.domains.host.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field

enum class HostErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    NOT_MANAGER_HOST(BAD_REQUEST, "HOST_400_1", "매니저 권한이 없는 유저입니다."),
    FORBIDDEN_HOST(BAD_REQUEST, "HOST_400_2", "해당 호스트에 대한 접근 권한이 없습니다."),
    ALREADY_JOINED_HOST(BAD_REQUEST, "HOST_400_3", "이미 가입되어 있는 유저입니다."),
    NOT_MASTER_HOST(BAD_REQUEST, "HOST_400_4", "마스터 호스트 권한이 없는 유저입니다."),
    CANNOT_MODIFY_MASTER_HOST_ROLE(BAD_REQUEST, "HOST_400_5", "마스터 호스트의 권한은 변경할 수 없습니다."),
    NOT_ACCEPTED_HOST(BAD_REQUEST, "HOST_400_6", "아직 초대를 수락하지 않은 유저입니다."),
    NOT_PARTNER_HOST(BAD_REQUEST, "HOST_400_7", "파트너 호스트만 사용할 수 있는 기능입니다. 제휴 신청을 해주세요."),
    DUPLICATED_SLACK_URL(BAD_REQUEST, "HOST_400_8", "기존과 동일한 슬랙 url 입니다."),
    INVALID_SLACK_URL(BAD_REQUEST, "HOST_400_9", "유효하지 않은 않은 슬랙 url 입니다."),
    HOST_NOT_FOUND(NOT_FOUND, "Host_404_1", "해당 호스트를 찾을 수 없습니다."),
    HOST_USER_NOT_FOUND(NOT_FOUND, "HOST_404_2", "가입된 호스트 유저가 아닙니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: reason
    }

    fun getReason(): String = reason
}
