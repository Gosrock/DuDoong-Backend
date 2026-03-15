package band.gosrock.api.comment.mapper

import band.gosrock.api.comment.model.request.CreateCommentRequest
import band.gosrock.api.comment.model.response.CreateCommentResponse
import band.gosrock.api.comment.model.response.RetrieveCommentCountResponse
import band.gosrock.api.comment.model.response.RetrieveCommentDTO
import band.gosrock.api.comment.model.response.RetrieveRandomCommentResponse
import band.gosrock.api.common.slice.SliceResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor
import band.gosrock.domain.domains.comment.domain.Comment
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.user.domain.User
import org.springframework.transaction.annotation.Transactional

@Mapper
class CommentMapper(
    private val commentAdaptor: CommentAdaptor,
) {
    fun toEntity(user: User, event: Event, createDTO: CreateCommentRequest): Comment =
        Comment.create(createDTO.content, createDTO.nickName, user, event.id!!)

    @Transactional(readOnly = true)
    fun toCreateCommentResponse(comment: Comment, user: User): CreateCommentResponse =
        CreateCommentResponse.of(comment, user)

    @Transactional(readOnly = true)
    fun toRetrieveCommentListResponse(
        commentCondition: CommentCondition,
        currentUserId: Long?,
    ): SliceResponse<RetrieveCommentDTO> {
        val comments = commentAdaptor.searchComment(commentCondition)
        return SliceResponse.of(comments.map { toRetrieveCommentDTO(it, currentUserId) })
    }

    @Transactional(readOnly = true)
    fun retrieveComment(commentId: Long): Comment =
        commentAdaptor.queryComment(commentId)

    fun toRetrieveCommentCountResponse(commentCount: Long?): RetrieveCommentCountResponse =
        RetrieveCommentCountResponse.of(commentCount)

    fun toRetrieveRandomCommentResponse(comments: List<Comment>): RetrieveRandomCommentResponse =
        RetrieveRandomCommentResponse.of(comments)

    private fun toRetrieveCommentDTO(comment: Comment, currentUserId: Long?): RetrieveCommentDTO =
        RetrieveCommentDTO.of(comment, currentUserId)
}
