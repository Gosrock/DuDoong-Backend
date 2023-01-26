package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Embeddable
@RequiredArgsConstructor
public class OrderItemVo {

    // 상품 이름 ( 추후에 상품 이름이 변해도 주문엔 그대로 있어야함. )
    private String name;

    // 상품 가격 ( 추후에 상품 가격이 변해도 주문엔 그대로 있어야함. )
    private Money price;

    // 아이템 그룹 아이디 ( 이벤트 아이디(상품들의 그룹) )
    private Long itemGroupId;

    private Long itemId;

    @Builder
    public OrderItemVo(String name, Money price, Long itemGroupId, Long itemId) {
        this.name = name;
        this.price = price;
        this.itemGroupId = itemGroupId;
        this.itemId = itemId;
    }

    public static OrderItemVo from(TicketItem ticketItem) {
        return OrderItemVo.builder()
                .itemGroupId(ticketItem.getEventId())
                .itemId(ticketItem.getId())
                .price(ticketItem.getPrice())
                .name(ticketItem.getName())
                .build();
    }
}
