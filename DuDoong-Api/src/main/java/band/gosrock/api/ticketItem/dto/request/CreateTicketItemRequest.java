package band.gosrock.api.ticketItem.dto.request;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@Getter
@RequiredArgsConstructor
public class CreateTicketItemRequest {

    @NotNull
    @Schema(nullable = false, example = "1")
    private Long eventId;

    @NotNull
    @Schema(nullable = false, defaultValue = "FIRST_COME_FIRST_SERVED")
    private TicketType type;

    @NotNull
    @Schema(nullable = false, example = "일반 티켓")
    private String name;

    @Nullable
    @Schema(nullable = true, example = "일반 입장 티켓입니다.")
    private String description;

    @NotNull
    @Schema(nullable = false, example = "4000")
    private Long price;

    @NotNull
    @Schema(nullable = false, example = "100")
    private Long supplyCount;

    @NotNull
    @Schema(nullable = false, example = "1")
    private Long purchaseLimit;

    public TicketItem toEntity(Event event) {

        return TicketItem.builder()
                .type(type)
                .name(name)
                .description(description)
                .price(Money.wons(price))
                .quantity(supplyCount)
                .supplyCount(supplyCount)
                .purchaseLimit(purchaseLimit)
                .isSellable(true)
                .event(event)
                .build();
    }
}
