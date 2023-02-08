package band.gosrock.api.comment.model.response;


import band.gosrock.domain.common.vo.CommentInfoVo;
import band.gosrock.domain.domains.comment.domain.Comment;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetrieveRandomCommentResponse {

    private final List<CommentInfoVo> commentInfos;

    public static RetrieveRandomCommentResponse of(List<Comment> comments) {
        return RetrieveRandomCommentResponse.builder()
                .commentInfos(comments.stream().map(Comment::toCommentInfoVo).toList())
                .build();
    }
}
