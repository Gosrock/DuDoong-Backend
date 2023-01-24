package band.gosrock.api.host.model.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 호스트 슬랙 알람 URL 수정 요청 DTO */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InviteHostRequest {
    @Schema(defaultValue = "kls1998@naver.com", description = "초대할 유저 이메일 주소")
    @Email(message = "올바른 이메일을 입력해주세요")
    private String email;
}
