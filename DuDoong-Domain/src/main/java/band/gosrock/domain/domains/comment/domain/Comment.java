package band.gosrock.domain.domains.comment.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.CommentInfoVo;
import band.gosrock.domain.domains.user.domain.User;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_comment")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(length = 200)
    private String content;

    @Column(length = 15)
    private String nickName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

    private Long eventId;

    @Enumerated(EnumType.STRING)
    private CommentStatus commentStatus;

    @Builder
    public Comment(String content, String nickName, User user, Long eventId, CommentStatus commentStatus) {
        this.content = content;
        this.nickName = nickName;
        this.user = user;
        this.eventId = eventId;
        this.commentStatus = commentStatus;
    }

    public static Comment create(String content, String nickName, User user, Long eventId) {
        return Comment.builder()
                .content(content)
                .nickName(nickName)
                .user(user)
                .eventId(eventId)
            .commentStatus(CommentStatus.ACTIVE)
                .build();
    }

    public CommentInfoVo toCommentInfoVo() {
        return CommentInfoVo.from(this);
    }

    public void delete() {
        this.commentStatus = CommentStatus.INACTIVE;
    }
}
