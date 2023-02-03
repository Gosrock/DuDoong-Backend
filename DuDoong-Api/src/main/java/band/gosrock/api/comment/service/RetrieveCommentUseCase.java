package band.gosrock.api.comment.service;


import band.gosrock.api.comment.mapper.CommentMapper;
import band.gosrock.api.comment.model.response.RetrieveCommentDTO;
import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.slice.SliceResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@UseCase
@RequiredArgsConstructor
public class RetrieveCommentUseCase {

    private final CommentMapper commentMapper;

    private final EventAdaptor eventAdaptor;

    private final UserUtils userUtils;

    public SliceResponse<RetrieveCommentDTO> execute(Long eventId, Pageable pageable) {
        Event event = eventAdaptor.findById(eventId);
        Long currentUserId = userUtils.getCurrentUserId();
        CommentCondition commentCondition = new CommentCondition(event.getId(), pageable);
        return commentMapper.toRetrieveCommentListResponse(commentCondition, currentUserId);
    }
}
