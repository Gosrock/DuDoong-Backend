package band.gosrock.domain.domains.issuedTicket.domain;

import static band.gosrock.common.consts.DuDoongStatic.NO_START_NUMBER;

import band.gosrock.domain.common.aop.domainEvent.Events;
import band.gosrock.domain.common.events.issuedTicket.EntranceIssuedTicketEvent;
import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelEntranceException;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelException;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotEntranceException;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketAlreadyEntranceException;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.mail.dto.EmailIssuedTicketInfo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_issued_ticket")
public class IssuedTicket extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_ticket_id")
    private Long id;

    private String issuedTicketNo;

    private Long eventId;

    @Embedded private IssuedTicketUserInfoVo userInfo;

    @Embedded private IssuedTicketItemInfoVo itemInfo;

    /*
    ?????? ?????? ?????? id (?????????)
     */
    private String orderUuid;

    private LocalDateTime enteredAt;

    /*
    ?????? ????????? ?????? ??? (?????????)
     */
    private Long orderLineId;

    /*
    ?????? ????????? ????????? (?????????)
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "issued_ticket_id")
    private List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers = new ArrayList<>();

    public void addOptionAnswers(List<IssuedTicketOptionAnswer> answers) {
        issuedTicketOptionAnswers.addAll(answers);
    }

    /*
    ?????? ?????? uuid
     */
    @Column(nullable = false)
    private String uuid;

    /*
    ?????? ?????? ??????
     */
    @Enumerated(EnumType.STRING)
    private IssuedTicketStatus issuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE;

    /*
    ????????? ?????? ?????? ?????? ??? List??? ??? ????????? ?????? ?????????
    new ArrayList<>()??? ??? ??? ????????? ???????????? NPE??? ?????? ??? ??? ??????
     */
    @Builder
    public IssuedTicket(
            Long eventId,
            IssuedTicketUserInfoVo userInfo,
            String orderUuid,
            Long orderLineId,
            IssuedTicketItemInfoVo itemInfo,
            IssuedTicketStatus issuedTicketStatus,
            List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers) {
        this.eventId = eventId;
        this.userInfo = userInfo;
        this.itemInfo = itemInfo;
        this.orderUuid = orderUuid;
        this.orderLineId = orderLineId;
        this.issuedTicketStatus = issuedTicketStatus;
        this.issuedTicketOptionAnswers.addAll(issuedTicketOptionAnswers);
    }

    /** ---------------------------- ?????? ?????? ????????? ---------------------------------- */

    /*
    issuedTicket ??????????????? UUID ??????
     */
    @PrePersist
    public void createUUID() {
        this.uuid = UUID.randomUUID().toString();
    }

    /*
    issuedTicket ??????????????? ?????? ?????? ??????
     */
    @PostPersist
    public void createIssuedTicketNo() {
        this.issuedTicketNo = "T" + Long.sum(NO_START_NUMBER, this.id);
    }

    /*
    ?????? ?????? ????????? ??? ??????
     */
    public Money sumOptionPrice() {
        return issuedTicketOptionAnswers.stream()
                .map(IssuedTicketOptionAnswer::getAdditionalPrice)
                .reduce(Money.ZERO, Money::plus);
    }

    /*
    issuedTicket VO ?????? ?????????
     */
    public IssuedTicketInfoVo toIssuedTicketInfoVo() {
        return IssuedTicketInfoVo.from(this);
    }

    public static IssuedTicket create(
            TicketItem ticketItem,
            User user,
            Order order,
            Long eventId,
            OrderLineItem orderLineItem) {
        List<OrderOptionAnswer> orderOptionAnswers = orderLineItem.getOrderOptionAnswers();
        return IssuedTicket.builder()
                .issuedTicketOptionAnswers(
                        orderOptionAnswers.stream().map(IssuedTicketOptionAnswer::from).toList())
                .itemInfo(IssuedTicketItemInfoVo.from(ticketItem))
                .orderLineId(orderLineItem.getId())
                .orderUuid(order.getUuid())
                .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
                .userInfo(IssuedTicketUserInfoVo.from(user))
                .eventId(eventId)
                .build();
    }

    public static List<IssuedTicket> orderLineToIssuedTicket(
            TicketItem ticketItem,
            User user,
            Order order,
            Long eventId,
            OrderLineItem orderLineItem) {
        Long quantity = orderLineItem.getQuantity();
        return LongStream.range(0, quantity)
                .mapToObj(i -> IssuedTicket.create(ticketItem, user, order, eventId, orderLineItem))
                .toList();
    }

    /*
    ?????? ????????? ????????? ?????? Dto??? ???????????? ?????????
     */
    public EmailIssuedTicketInfo toEmailIssuedTicketInfo() {
        return new EmailIssuedTicketInfo(
                this.getIssuedTicketNo(),
                this.getItemInfo().getTicketName(),
                this.getCreatedAt(),
                this.getIssuedTicketStatus().getKr(),
                this.getItemInfo().getPrice().toString());
    }

    /** ---------------------------- ?????? ?????? ?????? ????????? ---------------------------------- */

    /*
    ?????? ?????? ?????? ?????????
    ????????? ?????? ????????? ????????? ????????? ?????? ??? ??? ??????
     */
    public void cancel() {
        if (!this.issuedTicketStatus.isBeforeEntrance()) {
            throw CanNotCancelException.EXCEPTION;
        }
        this.issuedTicketStatus = IssuedTicketStatus.CANCELED;
    }

    /*
    ?????? ???????????? ?????? ??? ?????? ?????? ?????????
    ????????? ?????? ????????? ????????? ????????? ?????? ??? ??? ??????
    (?????? ?????? ????????? ????????? ??????)
     */
    public void entrance() {
        if (this.issuedTicketStatus.isCanceled()) {
            throw CanNotEntranceException.EXCEPTION;
        }
        if (this.issuedTicketStatus.isAfterEntrance()) {
            throw IssuedTicketAlreadyEntranceException.EXCEPTION;
        }
        this.issuedTicketStatus = IssuedTicketStatus.ENTRANCE_COMPLETED;
        this.enteredAt = LocalDateTime.now();
        Events.raise(EntranceIssuedTicketEvent.from(this));
    }

    /*
    ?????? ?????? ?????? ?????????
    ????????? ?????? ?????? ????????? ????????? ?????? ?????? ??? ??? ??????
     */
    public void entranceCancel() {
        if (!this.issuedTicketStatus.isAfterEntrance()) {
            throw CanNotCancelEntranceException.EXCEPTION;
        }
        this.issuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE;
    }

    public Long getUserId() {
        return this.userInfo.getUserId();
    }
}
