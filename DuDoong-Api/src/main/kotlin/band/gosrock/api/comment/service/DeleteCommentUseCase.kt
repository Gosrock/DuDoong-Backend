package band.gosrock.api.comment.service

import band.gosrock.api.comment.mapper.CommentMapper
import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.comment.service.CommentDomainService
import band.gosrock.domain.domains.event.service.EventService
import org.springframework.transaction.annotation.Transactional

@UseCase
class DeleteCommentUseCase(
    private val commentMapper: CommentMapper,
    private val eventService: EventService,
    private val commentDomainService: CommentDomainService,
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long, commentId: Long) {
        val comment = commentMapper.retrieveComment(commentId)
        commentDomainService.deleteComment(comment, eventId)
    }
}
