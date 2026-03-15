package band.gosrock.domain.domains.comment.repository

import band.gosrock.domain.domains.comment.domain.Comment
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition
import org.springframework.data.domain.Slice

interface CommentCustomRepository {
    fun searchToPage(commentCondition: CommentCondition): Slice<Comment>
    fun countComment(eventId: Long): Long
}
