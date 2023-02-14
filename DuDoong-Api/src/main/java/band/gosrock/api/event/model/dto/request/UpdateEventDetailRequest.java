package band.gosrock.api.event.model.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateEventDetailRequest {
    // 포스터 이미지 url
    @Schema(defaultValue = "test/event/5/aa.jpeg", description = "포스터 이미지 key")
    @NotEmpty(message = "포스터 이미지 키 값을 입력해주세요")
    private String posterImageKey;

    // (마크다운) 공연 상세 내용
    @Schema(defaultValue = "고스락 공연에 여러분을 초대합니다 웰컴웰컴", description = "공연 상세 내용")
    @NotBlank(message = "공연장 상세 내용을 입력하세요")
    private String content;
}
