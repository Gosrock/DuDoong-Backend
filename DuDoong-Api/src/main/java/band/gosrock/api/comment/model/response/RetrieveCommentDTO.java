package band.gosrock.api.comment.model.response;


import band.gosrock.domain.common.vo.CommentInfoVo;
import band.gosrock.domain.domains.comment.domain.Comment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetrieveCommentDTO {

    private final CommentInfoVo commentInfo;

    public static RetrieveCommentDTO of(Comment comment) {
        return RetrieveCommentDTO.builder().commentInfo(comment.toCommentInfoVo()).build();
    }
}
