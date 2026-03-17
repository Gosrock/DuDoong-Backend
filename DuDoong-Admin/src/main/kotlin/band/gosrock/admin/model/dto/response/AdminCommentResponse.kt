package band.gosrock.admin.model.dto.response

import band.gosrock.domain.domains.comment.domain.Comment
import band.gosrock.domain.domains.comment.domain.CommentStatus
import java.time.LocalDateTime

data class AdminCommentResponse(
    val id: Long,
    val userName: String?,
    val eventName: String?,
    val content: String?,
    val commentStatus: CommentStatus?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(comment: Comment, eventName: String?): AdminCommentResponse =
            AdminCommentResponse(
                id = comment.id!!,
                userName = comment.nickName,
                eventName = eventName,
                content = comment.content,
                commentStatus = comment.commentStatus,
                createdAt = comment.createdAt,
            )
    }
}
