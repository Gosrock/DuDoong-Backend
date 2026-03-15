package band.gosrock.domain.common.vo

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.comment.domain.Comment
import java.time.LocalDateTime

class CommentInfoVo private constructor(
    val commentId: Long?,
    val nickName: String?,
    val content: String?,
    @field:DateFormat val createdAt: LocalDateTime?,
    val eventId: Long?,
    val userId: Long?,
) {
    companion object {
        @JvmStatic
        fun from(comment: Comment): CommentInfoVo =
            CommentInfoVo(
                commentId = comment.id,
                nickName = comment.nickName,
                content = comment.content,
                createdAt = comment.createdAtKt(),
                eventId = comment.eventId,
                userId = comment.user?.id,
            )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var commentId: Long? = null
        private var nickName: String? = null
        private var content: String? = null
        private var createdAt: LocalDateTime? = null
        private var eventId: Long? = null
        private var userId: Long? = null

        fun commentId(v: Long?) = apply { commentId = v }
        fun nickName(v: String?) = apply { nickName = v }
        fun content(v: String?) = apply { content = v }
        fun createdAt(v: LocalDateTime?) = apply { createdAt = v }
        fun eventId(v: Long?) = apply { eventId = v }
        fun userId(v: Long?) = apply { userId = v }
        fun build() = CommentInfoVo(commentId, nickName, content, createdAt, eventId, userId)
    }
}
