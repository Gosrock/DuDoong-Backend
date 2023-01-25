package band.gosrock.api.host.model.dto.request;


import band.gosrock.common.annotation.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 호스트 간편 생성 요청 DTO */
@Getter
@RequiredArgsConstructor
public class CreateHostRequest {
    @Schema(defaultValue = "고스락", description = "호스트 이름")
    @NotEmpty(message = "호스트 이름을 입력해주세요")
    private String name;

    @Schema(defaultValue = "gosrock@gsrk.com", description = "마스터 이메일")
    @Email(message = "올바른 형식의 이메일을 입력하세요")
    private String contactEmail;

    @Schema(defaultValue = "010-1111-3333", description = "마스터 전화번호")
    @Phone(message = "올바른 형식의 번호를 입력하세요")
    private String contactNumber;
}
