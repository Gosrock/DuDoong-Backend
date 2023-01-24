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
public class CreateEventDetailRequest {

    // 분단위 입니다
    @Schema(defaultValue = "90", description = "공연 진행시간")
    @Positive(message = "공연 진행 소요시간(분)을 입력하세요")
    private Long runTime;

    // 포스터 이미지 url
    @Schema(defaultValue = "https://s3.dudoong.com/image", description = "포스터 이미지 url")
    @URL(message = "올바른 url 형식을 입력하세요")
    private String posterImageUrl;

    // 상세 이미지 url
    @Schema(defaultValue = "https://s3.dudoong.com/img1", description = "공연 상세 이미지 url1")
    @URL(message = "올바른 url 형식을 입력하세요")
    private String detailImageUrl;
    // todo :: 리스트로 받던가, 3개 필드로 받기

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
