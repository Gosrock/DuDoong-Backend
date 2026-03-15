package band.gosrock.domain.domains.user.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.FORBIDDEN
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field

enum class UserErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    @ExplainError("회원가입시에 이미 회원가입한 유저일시 발생하는 오류. 회원가입전엔 항상 register valid check 를 해주세요")
    USER_ALREADY_SIGNUP(BAD_REQUEST, "USER_400_1", "이미 회원가입한 유저입니다."),

    @ExplainError("정지 처리된 유저일 경우 밣생하는 오류")
    USER_FORBIDDEN(FORBIDDEN, "USER_403_1", "접근이 제한된 유저입니다."),

    @ExplainError("탈퇴한 유저로 접근하려는 경우")
    USER_ALREADY_DELETED(FORBIDDEN, "USER_403_2", "이미 지워진 유저입니다."),

    @ExplainError("유저 정보를 찾을 수 없는 경우")
    USER_NOT_FOUND(NOT_FOUND, "USER_404_1", "유저 정보를 찾을 수 없습니다."),
    USER_PHONE_INVALID(BAD_REQUEST, "USER_400_2", "유저의 휴대폰 전화번호가 올바르지않습니다. 두둥 관리자에게 문의주세요"),

    @ExplainError("알림톡 발송시 보내는 유저의 전화번호 정보가 null이라 알림톡 발송 불가 경우")
    USER_PHONE_EMPTY(BAD_REQUEST, "USER_400_3", "유저의 휴대폰 전화번호가 null입니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: reason
    }

    fun getReason(): String = reason
}
