package band.gosrock.api.ticketItem.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApplyTicketOptionRequest {

    @Schema(nullable = false, example = "1")
    @Positive(message = "올바른 옵션그룹 고유 아이디를 입력해주세요")
    private Long optionGroupId;
}
