package band.gosrock.admin.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.FORBIDDEN
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field

enum class AdminErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    @ExplainError("어드민 권한이 없는 유저가 어드민 기능에 접근하려는 경우")
    ADMIN_FORBIDDEN(FORBIDDEN, "ADMIN_403_1", "어드민 권한이 필요합니다. MANAGER 이상의 역할이 필요합니다."),

    @ExplainError("SUPER_ADMIN 전용 기능에 일반 관리자가 접근하려는 경우")
    ADMIN_SUPER_ADMIN_REQUIRED(FORBIDDEN, "ADMIN_403_2", "SUPER_ADMIN 권한이 필요합니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: reason
    }
}
