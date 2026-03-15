package band.gosrock.domain.domains.comment.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.comment.domain.Comment
import org.springframework.transaction.annotation.Transactional

@DomainService
class CommentDomainService {

    @Transactional
    fun deleteComment(comment: Comment, eventId: Long) {
        comment.checkEvent(eventId)
        comment.delete()
    }
}
