package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.comment.domain.Comment;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentInfoVo {

    private final Long commentId;

    private final String nickName;

    private final String content;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Seoul")
    private final LocalDateTime createdAt;

    public static CommentInfoVo from(Comment comment) {
        return CommentInfoVo.builder()
                .commentId(comment.getId())
                .nickName(comment.getNickName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
