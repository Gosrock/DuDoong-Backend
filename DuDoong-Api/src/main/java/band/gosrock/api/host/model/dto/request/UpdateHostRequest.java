package band.gosrock.api.host.model.dto.request;


import band.gosrock.common.annotation.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;

/** 호스트 정보 변경 요청 DTO */
@Getter
@RequiredArgsConstructor
public class UpdateHostRequest {
    @Schema(defaultValue = "고스락", description = "호스트 이름")
    @NotEmpty(message = "호스트 이름을 입력해주세요")
    private String name;

    @Schema(defaultValue = "고슬고슬고스락", description = "호스트 간단 소개")
    @NotNull(message = "간단 소개를 입력해주세요")
    private String introduce;

    @Schema(defaultValue = "1990", description = "호스트 시작년도")
    @Length(min = 4, max = 10, message = "4~10자리 문자열을 입력해주세요")
    private String since;

    @Schema(defaultValue = "https://s3.dudoong.com/img", description = "호스트 프로필 이미지")
    private String profileImageUrl;

    @Schema(defaultValue = "gosrock@gsrk.com", description = "마스터 이메일")
    @Email(message = "올바른 형식의 이메일을 입력하세요")
    private String contactEmail;

    @Schema(defaultValue = "010-1111-3333", description = "마스터 전화번호")
    @Phone(message = "올바른 형식의 번호를 입력하세요")
    private String contactNumber;
}
