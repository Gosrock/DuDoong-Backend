package band.gosrock.api.comment.mapper;


import band.gosrock.api.comment.model.request.CreateCommentRequest;
import band.gosrock.api.comment.model.response.CreateCommentResponse;
import band.gosrock.api.comment.model.response.RetrieveCommentListResponse;
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
    public RetrieveCommentListResponse toRetrieveCommentListResponse(
            CommentCondition commentCondition, Long currentUserId) {
        Slice<Comment> comments = commentAdaptor.searchComment(commentCondition);
        return RetrieveCommentListResponse.of(comments, currentUserId);
    }
}
