package band.gosrock.api.host.model.dto.request;


import band.gosrock.common.annotation.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.URL;

/** 호스트 정보 변경 요청 DTO */
@Getter
@RequiredArgsConstructor
public class UpdateHostRequest {
    @Schema(defaultValue = "test/host/5/aa.jpg", description = "호스트 프로필 이미지")
    @NotEmpty
    private String profileImageKey;

    @Schema(defaultValue = "고슬고슬고스락", description = "호스트 간단 소개")
    @NotNull(message = "간단 소개를 입력해주세요")
    private String introduce;

    @Schema(defaultValue = "010-1111-3333", description = "마스터 전화번호")
    @Phone(message = "올바른 형식의 번호를 입력하세요")
    private String contactNumber;

    @Schema(defaultValue = "gosrock@gsrk.com", description = "마스터 이메일")
    @Email(message = "올바른 형식의 이메일을 입력하세요")
    private String contactEmail;
}
