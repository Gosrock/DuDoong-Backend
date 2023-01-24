package band.gosrock.api.comment.model.request;


import javax.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateCommentRequest {

    @NotNull(message = "작성자 닉네임을 입력해주세요.")
    private String nickName;

    @NotNull(message = "댓글 내용을 입력해주세요.")
    private String content;
}
