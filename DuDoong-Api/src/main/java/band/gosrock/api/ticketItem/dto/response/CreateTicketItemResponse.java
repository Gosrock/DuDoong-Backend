package band.gosrock.api.ticketItem.dto.response;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class CreateTicketItemResponse {
    @Schema(description = "티켓상품 id")
    private final Long ticketItemId;

    @Schema(description = "이름")
    private final String ticketName;

    @Schema(description = "설명")
    private final String description;

    @Schema(description = "가격")
    private final Money price;

    @Schema(description = "티켓 타입")
    private final TicketType type;

    @Schema(description = "1인당 구매 제한 매수")
    private final Long purchaseLimit;

    @Schema(description = "공급량")
    private final Long supplyCount;

    @Schema(description = "재고")
    private final Long quantity;

    public static CreateTicketItemResponse from(TicketItem ticketItem) {

        return CreateTicketItemResponse.builder()
                .ticketItemId(ticketItem.getId())
                .ticketName(ticketItem.getName())
                .description(ticketItem.getDescription())
                .price(ticketItem.getPrice())
                .type(ticketItem.getType())
                .purchaseLimit(ticketItem.getPurchaseLimit())
                .supplyCount(ticketItem.getSupplyCount())
                .quantity(ticketItem.getQuantity())
                .build();
    }
}
