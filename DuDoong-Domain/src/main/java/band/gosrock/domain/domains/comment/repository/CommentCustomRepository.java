package band.gosrock.domain.domains.comment.repository;


import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CommentCustomRepository {

    Slice<Comment> searchToPage(CommentCondition commentCondition, Pageable pageable);

    Long countComment(Long eventId);
}
