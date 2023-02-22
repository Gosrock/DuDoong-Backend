package band.gosrock.domain.domains.ticket_item.domain;

import static band.gosrock.common.consts.DuDoongStatic.MINIMUM_PAYMENT_WON;

import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.AccountInfoVo;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.exception.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_ticket_item")
public class TicketItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_item_id")
    private Long id;

    // 티켓 지불 타입
    @Enumerated(EnumType.STRING)
    private TicketPayType payType;

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

    // 티켓 승인 타입
    @Enumerated(EnumType.STRING)
    private TicketType type;

    @Embedded private AccountInfoVo accountInfo;

    // 재고 공개 여부
    private Boolean isQuantityPublic;

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

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private final List<ItemOptionGroup> itemOptionGroups = new ArrayList<>();

    @Builder
    public TicketItem(
            TicketPayType payType,
            String name,
            String description,
            Money price,
            Long quantity,
            Long supplyCount,
            Long purchaseLimit,
            TicketType type,
            String bankName,
            String accountNumber,
            String accountHolder,
            Boolean isQuantityPublic,
            Boolean isSellable,
            LocalDateTime saleStartAt,
            LocalDateTime saleEndAt,
            Event event) {
        this.payType = payType;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.supplyCount = supplyCount;
        this.purchaseLimit = purchaseLimit;
        this.type = type;
        this.accountInfo = AccountInfoVo.valueOf(bankName, accountNumber, accountHolder);
        this.isQuantityPublic = isQuantityPublic;
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

    public void removeItemOptionGroup(OptionGroup optionGroup) {
        // 재고 감소된 티켓상품은 옵션적용 변경 불가
        if (this.isQuantityReduced()) {
            throw ForbiddenOptionChangeException.EXCEPTION;
        }
        ItemOptionGroup itemOptionGroup = this.findItemOptionGroup(optionGroup);
        this.itemOptionGroups.remove(itemOptionGroup);
    }

    public ItemOptionGroup findItemOptionGroup(OptionGroup optionGroup) {
        return this.itemOptionGroups.stream()
                .filter(itemOptionGroup -> itemOptionGroup.getOptionGroup().equals(optionGroup))
                .findAny()
                .orElseThrow(() -> NotAppliedItemOptionGroupException.EXCEPTION);
    }

    public Boolean hasItemOptionGroup(Long optionGroupId) {
        return this.itemOptionGroups.stream()
                .anyMatch(
                        itemOptionGroup ->
                                itemOptionGroup.getOptionGroup().getId().equals(optionGroupId));
    }

    public void softDeleteTicketItem() {
        // 재고 감소된 티켓상품은 삭제 불가
        if (this.isQuantityReduced()) {
            throw ForbiddenTicketItemDeleteException.EXCEPTION;
        }
        this.ticketItemStatus = TicketItemStatus.DELETED;
    }

    public void validateEventId(Long eventId) {
        if (!this.getEvent().getId().equals(eventId)) {
            throw InvalidTicketItemException.EXCEPTION;
        }
    }

    public void validateTicketPayType(Boolean isPartner) {
        // 두둥티켓은 무조건 승인 + 계좌정보 필요
        if (payType == TicketPayType.DUDOONG_TICKET) {
            if (type != TicketType.APPROVAL) {
                throw InvalidTicketTypeException.EXCEPTION;
            }
            // 두둥 티켓은 유로 티켓 이어야함.
            if (price.isLessThanOrEqual(Money.ZERO)) {
                throw InvalidTicketPriceException.EXCEPTION;
            }
            if (!StringUtils.hasText(accountInfo.getBankName())
                    || !StringUtils.hasText(accountInfo.getAccountNumber())
                    || !StringUtils.hasText(accountInfo.getAccountHolder())) {
                throw EmptyAccountInfoException.EXCEPTION;
            }
        }
        // 유료티켓은 무조건 선착순 + 제휴 확인 + 1000원 이상
        else if (payType == TicketPayType.PRICE_TICKET) {
            if (type != TicketType.FIRST_COME_FIRST_SERVED) {
                throw InvalidTicketTypeException.EXCEPTION;
            }
            if (!isPartner) {
                throw InvalidPartnerException.EXCEPTION;
            }
            if (price.isLessThan(Money.wons(MINIMUM_PAYMENT_WON))) {
                throw InvalidTicketPriceException.EXCEPTION;
            }
        }
        // 무료티켓은 무조건 0원
        else {
            if (!price.equals(Money.ZERO)) {
                throw InvalidTicketPriceException.EXCEPTION;
            }
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
