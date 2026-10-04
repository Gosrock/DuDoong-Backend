package band.gosrock.domain.domains.host.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import band.gosrock.common.exception.DuDoongDynamicException
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

    // v2
    @ExplainError("멤버 추가·역할 변경에서 MASTER 를 지정한 경우. 마스터는 양도 API 로만 바꿀 수 있습니다.")
    CANNOT_ASSIGN_MASTER_ROLE(BAD_REQUEST, "HOST_400_10", "마스터 역할은 지정할 수 없습니다. 마스터 양도를 이용해주세요."),
    @ExplainError("마스터가 아닌 요청자가 매니저를 추가·삭제하려는 경우 (v2 에서는 403)")
    MANAGER_CAN_MANAGE_GUEST_ONLY(BAD_REQUEST, "HOST_400_11", "매니저는 일반 멤버만 추가·삭제할 수 있습니다."),
    CANNOT_REMOVE_MASTER(BAD_REQUEST, "HOST_400_12", "마스터는 삭제할 수 없습니다."),
    @ExplainError("가입되지 않은 이메일이 있는 경우. reason 뒤에 해당 이메일 목록이 붙습니다.")
    MEMBER_EMAIL_NOT_FOUND(BAD_REQUEST, "HOST_400_13", "가입되지 않은 이메일입니다."),
    @ExplainError("한 요청 안에 같은 이메일이 두 번 이상 있는 경우. reason 뒤에 해당 이메일 목록이 붙습니다.")
    DUPLICATED_MEMBER_EMAIL(BAD_REQUEST, "HOST_400_14", "중복된 이메일이 있습니다."),
    @ExplainError("이미 멤버(또는 v1 초대 대기)인 이메일이 있는 경우. reason 뒤에 해당 이메일 목록이 붙습니다.")
    ALREADY_HOST_MEMBER_EMAIL(BAD_REQUEST, "HOST_400_15", "이미 호스트 멤버인 이메일입니다."),
    @ExplainError("연락처가 0개 또는 최대 개수 초과, 값이 비었거나 너무 긴 경우 (전화번호는 15자 이하)")
    INVALID_HOST_CONTACT(BAD_REQUEST, "HOST_400_16", "연락처 형식이 올바르지 않습니다."),
    @ExplainError("이미지 key 가 빈 문자열이 아니면서 이 호스트의 이미지 업로드 API 가 발급한 경로(host/{hostId}/)로 시작하지 않는 경우")
    INVALID_HOST_IMAGE_KEY(BAD_REQUEST, "HOST_400_17", "이 호스트에 업로드한 이미지가 아닙니다."),
    @ExplainError("같은 이메일로 가입된 정상 계정이 여러 개라 멤버를 특정할 수 없는 경우. reason 뒤에 해당 이메일 목록이 붙습니다.")
    AMBIGUOUS_MEMBER_EMAIL(BAD_REQUEST, "HOST_400_18", "같은 이메일로 가입된 계정이 여러 개입니다."),

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

    /** reason 뒤에 상세 내용(예: 문제 이메일 목록)을 붙인 예외. code / status 는 그대로 */
    fun toDetailException(detail: String): DuDoongDynamicException =
        DuDoongDynamicException(status = status, code = code, reason = "$reason ($detail)")
}
