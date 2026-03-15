package band.gosrock.api.comment.model.request

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreateCommentRequest(
    @field:NotBlank(message = "작성자 닉네임을 입력해주세요.")
    @field:Size(min = 1, max = 10)
    val nickName: String,

    @field:NotBlank(message = "댓글 내용을 입력해주세요.")
    @field:Size(min = 1, max = 150)
    val content: String,
)
