package band.gosrock.domain.domains.comment.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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

    private Long userId;

    private Long eventId;

    @Builder
    public Comment(String content, String nickName, Long userId, Long eventId) {
        this.content = content;
        this.nickName = nickName;
        this.userId = userId;
        this.eventId = eventId;
    }

    public static Comment create(String content, String nickName, Long userId, Long eventId) {
        return Comment.builder()
                .content(content)
                .nickName(nickName)
                .userId(userId)
                .eventId(eventId)
                .build();
    }
}
