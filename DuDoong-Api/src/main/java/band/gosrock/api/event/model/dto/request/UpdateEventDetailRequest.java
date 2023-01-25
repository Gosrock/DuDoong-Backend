package band.gosrock.api.event.model.dto.request;


import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Getter
@RequiredArgsConstructor
public class UpdateEventDetailRequest {

    // 포스터 이미지 url
    @Schema(defaultValue = "https://s3.dudoong.com/image", description = "포스터 이미지 url")
    @URL(message = "올바른 url 형식을 입력하세요")
    private String posterImageUrl;

    @ArraySchema(
            arraySchema =
                    @Schema(
                            defaultValue =
                                    "[\"https://s3.dudoong.com/img1\", \"https://s3.dudoong.com/img2\", null]"),
            minItems = 3,
            maxItems = 3)
    @NotNull
    @Size(min = 3, max = 3, message = "NULL 포함 3개의 이미지 URL 을 등록해주세요")
    private List<@URL String> detailImageUrl;

    // (마크다운) 공연 상세 내용
    @Schema(defaultValue = "고스락 공연에 여러분을 초대합니다 웰컴웰컴", description = "공연 상세 내용")
    @NotBlank(message = "공연장 상세 내용을 입력하세요")
    private String content;
}
