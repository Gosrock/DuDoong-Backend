package band.gosrock.domain.domains.comment.service;

import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.comment.domain.Comment;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CommentDomainService {

    public void deleteComment(Comment comment, Long eventId) {
        comment.checkEvent(eventId);
        comment.delete();
    }
}
