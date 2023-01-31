package band.gosrock.api.comment.service;


import band.gosrock.api.comment.mapper.CommentMapper;
import band.gosrock.api.comment.model.response.RetrieveCommentCountResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RetrieveCommentCountUseCase {

    private final CommentAdaptor commentAdaptor;

    private final CommentMapper commentMapper;

    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public RetrieveCommentCountResponse execute(Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        Long commentCount = commentAdaptor.queryCommentCount(event.getId());
        return commentMapper.toRetrieveCommentCountResponse(commentCount);
    }
}
