package band.gosrock.api.event.model.dto.request;


import band.gosrock.common.annotation.DateFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Getter
@RequiredArgsConstructor
public class CreateEventRequest {
    @Schema(defaultValue = "1", description = "호스트 고유 아이디")
    @Positive
    private Long hostId;

    @Schema(defaultValue = "고스락 제 22회 정기공연", description = "공연 이름")
    @NotBlank(message = "공연 이름을 입력하세요")
    private String name;

    @Schema(
            type = "string",
            pattern = "yyyy-MM-dd HH:mm",
            defaultValue = "2023-03-24 19:00",
            description = "공연 시작 시간")
    @NotNull(message = "공연 시작 시간을 입력하세요")
    @DateFormat
    private LocalDateTime startAt;

    // 분단위 입니다
    @Schema(defaultValue = "90", description = "공연 진행시간")
    @Positive(message = "공연 진행 소요시간(분)을 입력하세요")
    private Long runTime;

    // (지도 정보) 경도 - x
    @Schema(defaultValue = "126.920036", description = "공연장 위치 경도")
    @Positive(message = "공연장 경도 정보를 입력하세요")
    private Double longitude;

    // (지도 정보) 위도 - y
    @Schema(defaultValue = "37.548369", description = "공연장 위치 위도")
    @Positive(message = "공연장 위도 정보를 입력하세요")
    private Double latitude;

    // 포스터 이미지 url
    @Schema(defaultValue = "https://s3.dudoong.com/image", description = "포스터 이미지 url")
    @URL(message = "올바른 url 형식을 입력하세요")
    private String posterImageUrl;

    // url 표시 이름 (unique)
    @Schema(defaultValue = "https://dudoong.com/gosrockband", description = "url 이름")
    @URL(message = "표시하고 싶은 url 별칭을 입력하세요")
    private String aliasUrl;

    // 공연 장소
    @Schema(defaultValue = "롤링홀", description = "공연장 이름")
    @NotBlank(message = "공연장 이름을 입력하세요")
    private String placeName;

    // 공연 상세 주소
    @Schema(defaultValue = "서울 마포구 어울마당로 35", description = "공연장 주소")
    @NotBlank(message = "공연장 상세주소를 입력하세요")
    private String placeAddress;

    // (마크다운) 공연 상세 내용
    @Schema(defaultValue = "고스락 공연에 여러분을 초대합니다 웰컴웰컴", description = "공연 상세 내용")
    @NotBlank(message = "공연장 상세 내용을 입력하세요")
    private String content;

    // 예매 시작 시각
    @Schema(
            type = "string",
            pattern = "yyyy-MM-dd HH:mm",
            defaultValue = "2023-03-10 12:00",
            description = "예매 시작 시간")
    @NotNull(message = "예매 시작 시간을 입력하세요")
    @DateFormat
    private LocalDateTime ticketingStartAt;

    // 예매 종료 시각
    @Schema(
            type = "string",
            pattern = "yyyy-MM-dd HH:mm",
            defaultValue = "2023-03-24 18:00",
            description = "예매 종료 시간")
    @NotNull(message = "예매 종료 시간을 입력하세요")
    @DateFormat
    private LocalDateTime ticketingEndAt;
}
