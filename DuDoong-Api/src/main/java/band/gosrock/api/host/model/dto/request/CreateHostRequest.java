package band.gosrock.api.host.model.dto.request;


import band.gosrock.common.annotation.Phone;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateHostRequest {
    @Schema(defaultValue = "gosrock@gsrk.com", description = "마스터 이메일")
    @Email(message = "올바른 형식의 이메일을 입력하세요")
    private final String contactEmail;

    @Schema(defaultValue = "010-1111-3333", description = "마스터 전화번호")
    @Phone(message = "올바른 형식의 번호를 입력하세요")
    private final String contactNumber;
}
