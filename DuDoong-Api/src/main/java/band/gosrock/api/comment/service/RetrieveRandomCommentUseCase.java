package band.gosrock.api.comment.service;


import band.gosrock.api.comment.mapper.CommentMapper;
import band.gosrock.api.comment.model.response.RetrieveRandomCommentResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RetrieveRandomCommentUseCase {

    private final CommentAdaptor commentAdaptor;

    private final CommentMapper commentMapper;

    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public RetrieveRandomCommentResponse execute(Long eventId, Long limit) {
        Event event = eventAdaptor.findById(eventId);
        List<Comment> comments = commentAdaptor.queryRandomComment(event.getId(), limit);
        return commentMapper.toRetrieveRandomCommentResponse(comments);
    }
}
