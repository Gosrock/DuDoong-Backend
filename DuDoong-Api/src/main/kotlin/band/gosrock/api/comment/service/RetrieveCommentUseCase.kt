package band.gosrock.api.comment.service

import band.gosrock.api.comment.mapper.CommentMapper
import band.gosrock.api.comment.model.response.RetrieveCommentDTO
import band.gosrock.api.common.UserUtils
import band.gosrock.api.common.slice.SliceResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import org.springframework.data.domain.Pageable

@UseCase
class RetrieveCommentUseCase(
    private val commentMapper: CommentMapper,
    private val eventAdaptor: EventAdaptor,
    private val userUtils: UserUtils,
) {
    fun execute(eventId: Long, pageable: Pageable): SliceResponse<RetrieveCommentDTO> {
        val event = eventAdaptor.findById(eventId)
        val currentUserId = userUtils.getCurrentUserId()
        val commentCondition = CommentCondition(event.id!!, pageable)
        return commentMapper.toRetrieveCommentListResponse(commentCondition, currentUserId)
    }
}
