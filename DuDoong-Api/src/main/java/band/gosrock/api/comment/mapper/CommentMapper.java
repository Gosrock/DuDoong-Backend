package band.gosrock.api.comment.mapper;


import band.gosrock.api.comment.model.request.CreateCommentRequest;
import band.gosrock.api.comment.model.response.CreateCommentResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class CommentMapper {

    private CommentAdaptor commentAdaptor;

    public Comment toEntity(Long userId, Long eventId, CreateCommentRequest createDTO) {
        return Comment.create(createDTO.getContent(), createDTO.getNickName(), userId, eventId);
    }

    @Transactional(readOnly = true)
    public CreateCommentResponse toCreateCommentResponse(Comment comment, User user) {
        return CreateCommentResponse.of(comment, user);
    }
}
