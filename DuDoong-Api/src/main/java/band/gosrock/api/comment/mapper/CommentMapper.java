package band.gosrock.api.comment.mapper;


import band.gosrock.api.comment.model.request.CreateCommentRequest;
import band.gosrock.api.comment.model.response.CreateCommentResponse;
import band.gosrock.api.comment.model.response.RetrieveCommentCountResponse;
import band.gosrock.api.comment.model.response.RetrieveCommentDTO;
import band.gosrock.api.comment.model.response.RetrieveRandomCommentResponse;
import band.gosrock.api.common.slice.SliceResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class CommentMapper {

    private final CommentAdaptor commentAdaptor;

    public Comment toEntity(User user, Event event, CreateCommentRequest createDTO) {
        return Comment.create(createDTO.getContent(), createDTO.getNickName(), user, event.getId());
    }

    @Transactional(readOnly = true)
    public CreateCommentResponse toCreateCommentResponse(Comment comment, User user) {
        return CreateCommentResponse.of(comment, user);
    }

    @Transactional(readOnly = true)
    public SliceResponse<RetrieveCommentDTO> toRetrieveCommentListResponse(
            CommentCondition commentCondition, Long currentUserId) {
        Slice<Comment> comments = commentAdaptor.searchComment(commentCondition);
        return SliceResponse.of(
                comments.map(comment -> toRetrieveCommentDTO(comment, currentUserId)));
    }

    @Transactional(readOnly = true)
    public Comment retrieveComment(Long commentId) {
        return commentAdaptor.queryComment(commentId);
    }

    public RetrieveCommentCountResponse toRetrieveCommentCountResponse(Long commentCount) {
        return RetrieveCommentCountResponse.of(commentCount);
    }

    public RetrieveRandomCommentResponse toRetrieveRandomCommentResponse(Comment comment) {
        return RetrieveRandomCommentResponse.of(comment);
    }

    private RetrieveCommentDTO toRetrieveCommentDTO(Comment comment, Long currentUserId) {
        return RetrieveCommentDTO.of(comment, currentUserId);
    }
}
