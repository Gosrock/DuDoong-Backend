package band.gosrock.api.event.model.dto.request;


import band.gosrock.common.annotation.DateFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@RequiredArgsConstructor
public class UpdateEventBasicRequest {
    @Schema(defaultValue = "고스락 제 22회 정기공연", description = "공연 이름")
    @NotBlank(message = "공연 이름을 입력하세요")
    @Length(max = 25)
    private String name;

    @Schema(
            type = "string",
            pattern = "yyyy.MM.dd HH:mm",
            defaultValue = "2023.03.20 12:00",
            description = "공연 시작 시각")
    @NotNull(message = "공연 시작 시각을 입력하세요")
    @DateFormat
    private LocalDateTime startAt;

    @Schema(defaultValue = "90", description = "공연 진행시간")
    @Positive(message = "공연 진행 예상 소요시간(분)을 입력하세요")
    private Long runTime;

    // 공연 장소
    @Schema(defaultValue = "롤링홀", description = "공연장 이름")
    @NotBlank(message = "공연장 이름을 입력하세요")
    private String placeName;

    // 공연 상세 주소
    @Schema(defaultValue = "서울 마포구 어울마당로 35", description = "공연장 주소")
    @NotBlank(message = "공연장 상세주소를 입력하세요")
    private String placeAddress;

    // (지도 정보) 경도 - x
    @Schema(defaultValue = "126.920036", description = "공연장 위치 경도")
    @Positive(message = "공연장 경도 정보를 입력하세요")
    private Double longitude;

    // (지도 정보) 위도 - y
    @Schema(defaultValue = "37.548369", description = "공연장 위치 위도")
    @Positive(message = "공연장 위도 정보를 입력하세요")
    private Double latitude;
}
