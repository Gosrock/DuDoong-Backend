package band.gosrock.api.comment.model.response

import band.gosrock.domain.common.vo.CommentInfoVo
import band.gosrock.domain.domains.comment.domain.Comment

data class RetrieveRandomCommentResponse(
    val commentInfos: List<CommentInfoVo?>,
) {
    companion object {
        @JvmStatic
        fun of(comments: List<Comment>): RetrieveRandomCommentResponse =
            RetrieveRandomCommentResponse(
                commentInfos = comments.map { it.toCommentInfoVo() },
            )
    }
}
