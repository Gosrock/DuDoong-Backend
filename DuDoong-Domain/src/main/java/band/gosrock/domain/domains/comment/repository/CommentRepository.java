package band.gosrock.domain.domains.comment.repository;


import band.gosrock.domain.domains.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {}
