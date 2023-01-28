package band.gosrock.api.comment.service;

import band.gosrock.api.comment.mapper.CommentMapper;
import band.gosrock.api.common.UserUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.service.CommentDomainService;
import band.gosrock.domain.domains.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final UserUtils userUtils;

    private final CommentMapper commentMapper;

    private final EventService eventService;

    private final CommentDomainService commentDomainService;

    @Transactional
    public void execute(Long eventId, Long commentId) {
        Long currentUserId = userUtils.getCurrentUserId();
        // 권한 검사
        eventService.checkEventHost(currentUserId, eventId);
        Comment comment = commentMapper.retrieveComment(commentId);
        commentDomainService.deleteComment(comment);
    }

}
