package band.gosrock.api.ticketItem.dto.response;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OptionResponse {
    @Schema(description = "옵션 id")
    private final Long optionId;

    @Schema(description = "응답")
    private final String answer;

    @Schema(description = "추가 금액")
    private final Money additionalPrice;

    public static OptionResponse from(Option option) {

        return OptionResponse.builder()
                .optionId(option.getId())
                .answer(option.getAnswer())
                .additionalPrice(option.getAdditionalPrice())
                .build();
    }
}
