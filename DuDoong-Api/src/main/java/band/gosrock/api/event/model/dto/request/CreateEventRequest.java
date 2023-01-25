package band.gosrock.api.event.model.dto.request;


import band.gosrock.common.annotation.DateFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
            defaultValue = "2023-03-20 12:00",
            description = "공연 시작 시각")
    @NotNull(message = "공연 시작 시각을 입력하세요")
    @DateFormat
    private LocalDateTime startAt;

    @Schema(defaultValue = "90", description = "공연 진행시간")
    @Positive(message = "공연 진행 예상 소요시간(분)을 입력하세요")
    private Long runTime;
}
