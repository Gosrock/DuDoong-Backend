package band.gosrock.api.comment.model.response;


import band.gosrock.domain.domains.comment.domain.Comment;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;

@Getter
@Builder
public class RetrieveCommentListResponse {

    private final Boolean hasNext;

    private final List<RetrieveCommentDTO> comments;

    public static RetrieveCommentListResponse of(Slice<Comment> comments) {
        return RetrieveCommentListResponse.builder()
                .hasNext(comments.hasNext())
                .comments(comments.stream().map(RetrieveCommentDTO::of).toList())
                .build();
    }
}
