package band.gosrock.api.comment.model.response;

import band.gosrock.domain.domains.comment.domain.Comment;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class RetrieveCommentListResponse {

    private final int page;

    private final int totalPage;

    private final Long offset;

    private final List<RetrieveCommentDTO> comments;

    public static RetrieveCommentListResponse of(Page<Comment> comments) {
        return RetrieveCommentListResponse.builder().page(comments.getPageable().getPageNumber())
            .totalPage(
                comments.getTotalPages()).offset(comments.getPageable().getOffset()).comments(
                comments.stream().map(comment -> RetrieveCommentDTO.of(comment, comment.getUser()))
                    .toList()).build();
    }
}
