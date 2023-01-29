package band.gosrock.domain.common.vo;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.comment.domain.Comment;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentInfoVo {

    private final Long commentId;

    private final String nickName;

    private final String content;

    @DateFormat private final LocalDateTime createdAt;

    private final Long eventId;

    private final Long userId;

    public static CommentInfoVo from(Comment comment) {
        return CommentInfoVo.builder()
                .commentId(comment.getId())
                .nickName(comment.getNickName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .eventId(comment.getEventId())
                .userId(comment.getUser().getId())
                .build();
    }
}
