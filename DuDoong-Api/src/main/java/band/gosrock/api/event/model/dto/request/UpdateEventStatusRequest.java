package band.gosrock.api.event.model.dto.request;


import band.gosrock.common.annotation.Enum;
import band.gosrock.domain.domains.event.domain.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateEventStatusRequest {

    @Schema(defaultValue = "OPEN", description = "오픈 상태")
    @Enum(message = "올바른 값을 입력해주세요.")
    private EventStatus status;
}
