package band.gosrock.api.comment.model.response;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.user.domain.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateCommentResponse {

    private final Long id;

    private final String nickName;

    private final String content;

    @DateFormat private final LocalDateTime createdAt;

    private final UserInfoVo userInfoVo;

    public static CreateCommentResponse of(Comment comment, User user) {
        return CreateCommentResponse.builder()
                .id(comment.getId())
                .nickName(comment.getNickName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .userInfoVo(user.toUserInfoVo())
                .build();
    }
}
