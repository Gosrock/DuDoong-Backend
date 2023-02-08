package band.gosrock.domain.domains.comment.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import band.gosrock.domain.domains.comment.exception.CommentNotFoundException;
import band.gosrock.domain.domains.comment.repository.CommentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;

@Adaptor
@RequiredArgsConstructor
public class CommentAdaptor {

    private final CommentRepository commentRepository;

    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    public Slice<Comment> searchComment(CommentCondition commentCondition) {
        return commentRepository.searchToPage(commentCondition);
    }

    public Comment queryComment(Long commentId) {
        return commentRepository
                .findById(commentId)
                .orElseThrow(() -> CommentNotFoundException.EXCEPTION);
    }

    public Long queryCommentCount(Long eventId) {
        return commentRepository.countComment(eventId);
    }

    public List<Comment> queryRandomComment(Long eventId, Long limit) {
        Long countComment = queryCommentCount(eventId);
        //        return commentRepository.queryRandomComment(eventId, countComment, offset);
        return commentRepository.findAllRandom(eventId, limit);
    }
}
