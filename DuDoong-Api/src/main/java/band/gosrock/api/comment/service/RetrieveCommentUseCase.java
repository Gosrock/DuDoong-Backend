package band.gosrock.api.comment.service;

import band.gosrock.api.comment.mapper.CommentMapper;
import band.gosrock.api.comment.model.response.RetrieveCommentListResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.comment.adaptor.CommentAdaptor;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RetrieveCommentUseCase {

    private final CommentMapper commentMapper;

    private final CommentAdaptor commentAdaptor;

    public RetrieveCommentListResponse execute(Long eventId, Long lastId) {
        CommentCondition commentCondition = new CommentCondition(eventId, lastId);
        return commentMapper.toRetrieveCommentListResponse(commentCondition);
    }
}
