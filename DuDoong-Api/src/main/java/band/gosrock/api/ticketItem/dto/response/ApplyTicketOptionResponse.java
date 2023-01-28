package band.gosrock.api.ticketItem.dto.response;


import band.gosrock.domain.domains.ticket_item.domain.ItemOptionGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplyTicketOptionResponse {
    @Schema(description = "상품-옵션그룹 id")
    private final Long itemOptionGroupId;

    @Schema(description = "티켓상품 id")
    private final Long ticketItemId;

    @Schema(description = "옵션그룹 id")
    private final Long optionGroupId;

    public static ApplyTicketOptionResponse from(ItemOptionGroup itemOptionGroup) {
        return ApplyTicketOptionResponse.builder()
                .itemOptionGroupId(itemOptionGroup.getId())
                .ticketItemId(itemOptionGroup.getItem().getId())
                .optionGroupId(itemOptionGroup.getOptionGroup().getId())
                .build();
    }
}
