package band.gosrock.api.comment.model.response

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.common.vo.UserInfoVo
import band.gosrock.domain.domains.comment.domain.Comment
import band.gosrock.domain.domains.user.domain.User
import java.time.LocalDateTime

data class CreateCommentResponse(
    val id: Long?,
    val nickName: String?,
    val content: String?,
    @DateFormat val createdAt: LocalDateTime?,
    val userInfoVo: UserInfoVo?,
) {
    companion object {
        @JvmStatic
        fun of(comment: Comment, user: User): CreateCommentResponse =
            CreateCommentResponse(
                id = comment.id,
                nickName = comment.nickName,
                content = comment.content,
                createdAt = comment.createdAt,
                userInfoVo = user.toUserInfoVo(),
            )
    }
}
