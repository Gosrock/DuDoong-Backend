package band.gosrock.api.comment.service

import band.gosrock.api.comment.mapper.CommentMapper
import band.gosrock.api.comment.model.response.RetrieveRandomCommentResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class RetrieveRandomCommentUseCase(
    private val commentAdaptor: CommentAdaptor,
    private val commentMapper: CommentMapper,
    private val eventAdaptor: EventAdaptor,
) {
    @Transactional(readOnly = true)
    fun execute(eventId: Long, limit: Long): RetrieveRandomCommentResponse {
        val event = eventAdaptor.findById(eventId)
        val comments = commentAdaptor.queryRandomComment(event.id!!, limit)
        return commentMapper.toRetrieveRandomCommentResponse(comments)
    }
}
