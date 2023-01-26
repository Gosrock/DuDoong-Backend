package band.gosrock.api.comment.model.request;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateCommentRequest {

    @NotBlank(message = "작성자 닉네임을 입력해주세요.")
    @Size(min = 1, max = 15)
    private String nickName;

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(min = 1, max = 200)
    private String content;
}
