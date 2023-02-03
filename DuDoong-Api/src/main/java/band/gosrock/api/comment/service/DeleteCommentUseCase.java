package band.gosrock.api.comment.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.comment.mapper.CommentMapper;
import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
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
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    public void execute(Long eventId, Long commentId) {
        Comment comment = commentMapper.retrieveComment(commentId);
        commentDomainService.deleteComment(comment, eventId);
    }
}
