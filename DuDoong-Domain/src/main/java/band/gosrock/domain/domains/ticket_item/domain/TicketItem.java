package band.gosrock.domain.domains.ticket_item.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.exception.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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

    // 상태
    @Enumerated(EnumType.STRING)
    @ColumnDefault(value = "'VALID'")
    private TicketItemStatus ticketItemStatus = TicketItemStatus.VALID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private final Set<ItemOptionGroup> itemOptionGroups = new HashSet<>();

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
            Event event) {
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
    }

    public void addItemOptionGroup(OptionGroup optionGroup) {
        // 재고 감소된 티켓상품은 옵션적용 변경 불가
        if (this.isQuantityReduced()) {
            throw ForbiddenOptionChangeException.EXCEPTION;
        }

        // 중복 체크
        if (this.hasItemOptionGroup(optionGroup.getId())) {
            throw DuplicatedItemOptionGroupException.EXCEPTION;
        }
        ItemOptionGroup itemOptionGroup =
                ItemOptionGroup.builder().item(this).optionGroup(optionGroup).build();
        this.itemOptionGroups.add(itemOptionGroup);
    }

    public Boolean hasItemOptionGroup(Long optionGroupId) {
        return this.itemOptionGroups.stream()
                .anyMatch(
                        itemOptionGroup ->
                                itemOptionGroup.getOptionGroup().getId().equals(optionGroupId));
    }

    public void patchTicketItemStatusToDeleted() {
        // 재고 감소된 티켓상품은 삭제 불가
        if (this.isQuantityReduced()) {
            throw ForbiddenTicketItemDeleteException.EXCEPTION;
        }
        this.ticketItemStatus = TicketItemStatus.DELETED;
    }

    public void checkEventId(Long eventId) {
        if (!this.getEvent().getId().equals(eventId)) {
            throw InvalidTicketItemException.EXCEPTION;
        }
    }

    public void checkTicketPrice() {
        if (!Money.ZERO.equals(this.price)) {
            throw InvalidTicketPriceException.EXCEPTION;
        }
    }

    public RefundInfoVo getRefundInfoVo() {
        return event.toRefundInfoVo();
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

    public Boolean isQuantityReduced() {
        return !this.quantity.equals(this.supplyCount);
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

    public void validPurchaseLimit(Long quantity) {
        if (this.purchaseLimit < quantity) {
            throw TicketPurchaseLimitException.EXCEPTION;
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
