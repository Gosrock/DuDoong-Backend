package band.gosrock.api.admin.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminDeleteCommentUseCase(
    private val commentAdaptor: CommentAdaptor,
) {

    @Transactional
    fun execute(commentId: Long) {
        val comment = commentAdaptor.queryComment(commentId)
        comment.delete()
    }
}
