package band.gosrock.api.admin.service

import band.gosrock.api.admin.model.dto.response.AdminCommentResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.comment.repository.CommentRepository
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetCommentsUseCase(
    private val commentRepository: CommentRepository,
    private val eventAdaptor: EventAdaptor,
) {

    fun execute(keyword: String?, pageable: Pageable): Page<AdminCommentResponse> {
        return commentRepository.findAllForAdmin(keyword, pageable)
            .map { comment ->
                val eventName = comment.eventId?.let {
                    runCatching { eventAdaptor.findById(it).eventBasic?.name }.getOrNull()
                }
                AdminCommentResponse.of(comment, eventName)
            }
    }
}
