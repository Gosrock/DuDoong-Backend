package band.gosrock.domain.domains.comment.repository;


import band.gosrock.domain.domains.comment.domain.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentCustomRepository {

    @Query(
            nativeQuery = true,
            value =
                    "SELECT * "
                            + "FROM tbl_comment as c "
                            + "WHERE c.event_id = :eventId "
                            + "ORDER BY RAND() DESC "
                            + "LIMIT :offset")
    List<Comment> findAllRandom(@Param("eventId") Long eventId, @Param("offset") Long limit);
}
