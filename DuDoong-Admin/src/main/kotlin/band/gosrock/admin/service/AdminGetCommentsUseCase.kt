package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminCommentResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.comment.repository.CommentRepository
import band.gosrock.domain.domains.event.repository.EventRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetCommentsUseCase(
    private val commentRepository: CommentRepository,
    private val eventRepository: EventRepository,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, keyword: String?, eventId: Long?, pageable: Pageable): Page<AdminCommentResponse> {
        adminAuthValidator.validateManagerOrAbove(userId)
        val commentPage = commentRepository.findAllForAdmin(keyword, eventId, pageable)

        // batch fetch events to avoid N+1
        val eventIds = commentPage.content.mapNotNull { it.eventId }
        val eventMap = eventRepository.findAllByIdIn(eventIds).associateBy { it.id }

        return commentPage.map { comment ->
            val eventName = comment.eventId?.let { eventMap[it]?.eventBasic?.name }
            AdminCommentResponse.of(comment, eventName)
        }
    }
}
