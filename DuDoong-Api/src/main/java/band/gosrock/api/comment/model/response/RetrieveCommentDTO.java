package band.gosrock.api.comment.model.response;


import band.gosrock.domain.common.vo.CommentInfoVo;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetrieveCommentDTO {

    private final CommentInfoVo commentInfo;

    private final UserInfoVo userInfo;

    public static RetrieveCommentDTO of(Comment comment, User user) {
        return RetrieveCommentDTO.builder()
                .commentInfo(comment.toCommentInfoVo())
                .userInfo(user.toUserInfoVo())
                .build();
    }
}
