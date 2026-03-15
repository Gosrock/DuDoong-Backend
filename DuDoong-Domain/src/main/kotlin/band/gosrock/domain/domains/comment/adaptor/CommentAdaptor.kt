package band.gosrock.domain.domains.comment.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.comment.domain.Comment
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition
import band.gosrock.domain.domains.comment.exception.CommentNotFoundException
import band.gosrock.domain.domains.comment.repository.CommentRepository
import org.springframework.data.domain.Slice

@Adaptor
class CommentAdaptor(
    private val commentRepository: CommentRepository,
) {
    fun save(comment: Comment): Comment = commentRepository.save(comment)

    fun searchComment(commentCondition: CommentCondition): Slice<Comment> =
        commentRepository.searchToPage(commentCondition)

    fun queryComment(commentId: Long): Comment =
        commentRepository.findById(commentId).orElseThrow { CommentNotFoundException.EXCEPTION }

    fun queryCommentCount(eventId: Long): Long = commentRepository.countComment(eventId)

    fun queryRandomComment(eventId: Long, limit: Long): List<Comment> {
        queryCommentCount(eventId)
        return commentRepository.findAllRandom(eventId, limit)
    }
}
