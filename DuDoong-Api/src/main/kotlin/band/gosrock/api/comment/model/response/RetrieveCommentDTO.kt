package band.gosrock.api.comment.model.response

import band.gosrock.domain.common.vo.CommentInfoVo
import band.gosrock.domain.domains.comment.domain.Comment

data class RetrieveCommentDTO(
    val commentInfo: CommentInfoVo?,
    val isMine: Boolean?,
) {
    companion object {
        @JvmStatic
        fun of(comment: Comment, currentUserId: Long?): RetrieveCommentDTO =
            RetrieveCommentDTO(
                commentInfo = comment.toCommentInfoVo(),
                isMine = comment.user?.id == currentUserId,
            )
    }
}
