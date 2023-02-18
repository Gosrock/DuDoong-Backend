package band.gosrock.api.ticketItem.dto.response;


import band.gosrock.domain.common.vo.AccountInfoVo;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TicketItemResponse {
    @Schema(description = "티켓상품 id")
    private final Long ticketItemId;

    @Schema(description = "티켓 지불 타입")
    private final TicketPayType payType;

    @Schema(description = "이름")
    private final String ticketName;

    @Schema(description = "설명")
    private final String description;

    @Schema(description = "가격")
    private final Money price;

    @Schema(description = "티켓 승인 타입")
    private final TicketType approveType;

    @Schema(description = "1인당 구매 제한 매수")
    private final Long purchaseLimit;

    @Schema(description = "공급량")
    private final Long supplyCount;

    @Schema(description = "재고")
    private final Long quantity;

    @Schema(description = "재고공개 여부")
    private final Boolean isQuantityPublic;

    @Schema(description = "계좌 정보")
    private final AccountInfoVo accountInfo;

    public static TicketItemResponse from(TicketItem ticketItem, Boolean isAdmin) {

        return TicketItemResponse.builder()
                .ticketItemId(ticketItem.getId())
                .payType(ticketItem.getPayType())
                .ticketName(ticketItem.getName())
                .description(ticketItem.getDescription())
                .price(ticketItem.getPrice())
                .approveType(ticketItem.getType())
                .purchaseLimit(ticketItem.getPurchaseLimit())
                .supplyCount(ticketItem.getSupplyCount())
                .quantity(
                        isAdmin || ticketItem.getIsQuantityPublic()
                                ? ticketItem.getQuantity()
                                : null)
                .isQuantityPublic(ticketItem.getIsQuantityPublic())
                .accountInfo(ticketItem.toAccountInfoVo())
                .build();
    }
}
