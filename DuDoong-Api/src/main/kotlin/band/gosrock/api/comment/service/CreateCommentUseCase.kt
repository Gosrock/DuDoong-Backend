package band.gosrock.api.comment.service

import band.gosrock.api.comment.mapper.CommentMapper
import band.gosrock.api.comment.model.request.CreateCommentRequest
import band.gosrock.api.comment.model.response.CreateCommentResponse
import band.gosrock.api.common.UserUtils
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class CreateCommentUseCase(
    private val userUtils: UserUtils,
    private val commentMapper: CommentMapper,
    private val commentAdaptor: CommentAdaptor,
    private val eventAdaptor: EventAdaptor,
) {
    @Transactional
    fun execute(eventId: Long, createDTO: CreateCommentRequest): CreateCommentResponse {
        val currentUser = userUtils.getCurrentUser()
        val event = eventAdaptor.findById(eventId)
        val comment = commentAdaptor.save(commentMapper.toEntity(currentUser, event, createDTO))
        return commentMapper.toCreateCommentResponse(comment, currentUser)
    }
}
