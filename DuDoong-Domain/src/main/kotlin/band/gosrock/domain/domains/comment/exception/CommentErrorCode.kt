package band.gosrock.domain.domains.comment.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field
import java.util.Objects

enum class CommentErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    COMMENT_NOT_FOUND(NOT_FOUND, "Comment_404_1", "응원글을 찾을 수 없습니다."),

    @ExplainError(value = "eventId 경로변수와 commentId가 맞지 않을 때 발생하는 에러입니다.")
    COMMENT_NOT_MATCH_EVENT(BAD_REQUEST, "Comment_400_1", "응원글과 이벤트가 맞지 않습니다."),

    COMMENT_ALREADY_DELETE(BAD_REQUEST, "Comment_400_2", "이미 삭제된 응원글입니다."),
    RETRIEVE_RANDOM_COMMENT_NOT_FOUND(NOT_FOUND, "Comment_404_2", "랜덤 응원글을 찾을 수 없습니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason.builder().reason(reason).code(code).status(status).build()

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation: ExplainError? = field.getAnnotation(ExplainError::class.java)
        return if (Objects.nonNull(annotation)) annotation!!.value else this.reason
    }
}
