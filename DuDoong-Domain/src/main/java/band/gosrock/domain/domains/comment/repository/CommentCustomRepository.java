package band.gosrock.domain.domains.comment.repository;

import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentCustomRepository {

    Page<Comment> searchToPage(CommentCondition commentCondition, Pageable pageable);
}
