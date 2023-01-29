package band.gosrock.api.comment.model.response;


import band.gosrock.domain.common.vo.CommentInfoVo;
import band.gosrock.domain.domains.comment.domain.Comment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetrieveRandomCommentResponse {

    private final CommentInfoVo commentInfo;

    public static RetrieveRandomCommentResponse of(Comment comment) {
        return RetrieveRandomCommentResponse.builder()
                .commentInfo(comment.toCommentInfoVo())
                .build();
    }
}
