package band.gosrock.api.comment.model.response

import band.gosrock.domain.domains.comment.domain.Comment
import org.springframework.data.domain.Slice

data class RetrieveCommentListResponse(
    val hasNext: Boolean?,
    val comments: List<RetrieveCommentDTO>,
) {
    companion object {
        @JvmStatic
        fun of(comments: Slice<Comment>, currentUserId: Long?): RetrieveCommentListResponse =
            RetrieveCommentListResponse(
                hasNext = comments.hasNext(),
                comments = comments.map { RetrieveCommentDTO.of(it, currentUserId) }.toList(),
            )
    }
}
