package band.gosrock.api.comment.model.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetrieveCommentCountResponse {

    private final Long commentCounts;

    public static RetrieveCommentCountResponse of(Long commentCounts) {
        return RetrieveCommentCountResponse.builder().commentCounts(commentCounts).build();
    }
}
