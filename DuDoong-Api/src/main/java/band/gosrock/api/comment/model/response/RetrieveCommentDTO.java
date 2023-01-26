package band.gosrock.api.comment.model.response;


import band.gosrock.domain.common.vo.CommentInfoVo;
import band.gosrock.domain.domains.comment.domain.Comment;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetrieveCommentDTO {

    private final CommentInfoVo commentInfo;

    private final Boolean isMine;

    public static RetrieveCommentDTO of(Comment comment, Long currentUserId) {
        return RetrieveCommentDTO.builder()
                .commentInfo(comment.toCommentInfoVo())
                .isMine(Objects.equals(comment.getUser().getId(), currentUserId))
                .build();
    }
}
