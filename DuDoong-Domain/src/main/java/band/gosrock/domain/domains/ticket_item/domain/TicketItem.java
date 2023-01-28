package band.gosrock.domain.domains.ticket_item.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityException;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLargeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_ticket_item")
public class TicketItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_item_id")
    private Long id;

    // 티켓 타입
    @Enumerated(EnumType.STRING)
    private TicketType type;

    // 티켓 이름
    private String name;

    // 티켓 설명
    private String description;

    // 티켓 가격
    private Money price;

    // 티켓 재고
    private Long quantity;

    // 티켓 공급량
    private Long supplyCount;

    // 1인당 구매 매수 제한
    private Long purchaseLimit;

    // 판매 가능 여부
    private Boolean isSellable;

    // 판매 시작 시간
    private LocalDateTime saleStartAt;

    // 판매 종료 시간
    private LocalDateTime saleEndAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @OneToMany(mappedBy = "item")
    private List<ItemOptionGroup> itemOptionGroups = new ArrayList<>();

    @Builder
    public TicketItem(
            TicketType type,
            String name,
            String description,
            Money price,
            Long quantity,
            Long supplyCount,
            Long purchaseLimit,
            Boolean isSellable,
            LocalDateTime saleStartAt,
            LocalDateTime saleEndAt,
            Event event,
            List<ItemOptionGroup> itemOptionGroups) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.supplyCount = supplyCount;
        this.purchaseLimit = purchaseLimit;
        this.isSellable = isSellable;
        this.saleStartAt = saleStartAt;
        this.saleEndAt = saleEndAt;
        this.event = event;
        this.itemOptionGroups = itemOptionGroups;
    }

    public RefundInfoVo getRefundInfoVo() {
        return event.getRefundInfoVo();
    }

    /** 선착순 결제인지 확인하는 메서드 */
    public Boolean isFCFS() {
        return this.type.isFCFS();
    }

    public Boolean hasOption() {
        return !itemOptionGroups.isEmpty();
    }

    public List<Long> getOptionGroupIds() {
        return itemOptionGroups.stream()
                .map(itemOptionGroup -> itemOptionGroup.getOptionGroup().getId())
                .sorted()
                .toList();
    }

    public void addOptionGroup(OptionGroup optionGroup) {
        ItemOptionGroup itemOptionGroup =
                ItemOptionGroup.builder().item(this).optionGroup(optionGroup).build();
        this.itemOptionGroups.add(itemOptionGroup);
    }

    public void reduceQuantity(Long quantity) {
        if (this.quantity < 0) {
            throw TicketItemQuantityException.EXCEPTION;
        }
        validEnoughQuantity(quantity);
        this.quantity = this.quantity - quantity;
    }

    public void validEnoughQuantity(Long quantity) {
        if (this.quantity < quantity) {
            throw TicketItemQuantityLackException.EXCEPTION;
        }
    }

    public void increaseQuantity(Long quantity) {
        if (this.quantity + quantity > supplyCount) {
            throw TicketItemQuantityLargeException.EXCEPTION;
        }
        this.quantity = this.quantity + quantity;
    }

    public Long getEventId() {
        return event.getId();
    }
}
