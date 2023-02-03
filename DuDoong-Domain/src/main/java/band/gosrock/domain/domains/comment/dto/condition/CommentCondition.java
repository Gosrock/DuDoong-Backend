package band.gosrock.domain.domains.comment.dto.condition;



import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

@Getter
@AllArgsConstructor
public class CommentCondition {

    private Long eventId;

    private Pageable pageable;
}
