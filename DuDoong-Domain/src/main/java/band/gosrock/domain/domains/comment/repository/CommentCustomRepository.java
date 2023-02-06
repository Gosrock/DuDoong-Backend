package band.gosrock.domain.domains.comment.repository;


import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import java.util.List;
import org.springframework.data.domain.Slice;

public interface CommentCustomRepository {

    Slice<Comment> searchToPage(CommentCondition commentCondition);

    Long countComment(Long eventId);

    List<Comment> queryRandomComment(Long eventId, Long count, Long offset);
}
