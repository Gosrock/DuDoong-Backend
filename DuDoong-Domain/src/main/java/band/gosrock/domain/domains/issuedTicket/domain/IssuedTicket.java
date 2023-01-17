package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketRequestForDev;
import band.gosrock.domain.domains.issuedTicket.dto.response.CreateIssuedTicketResponse;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

    /*
    발급 티켓의 이벤트 (양방향)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    /*
    발급 티켓의 주문 행 (단방향)
    Todo: 발급 티켓이 굳이 order line 을 알아야 할까? -찬진 OrderResponse 에서 필요함! 연관관계는 따로안짓고 레지스터리에서 불러올게용ㄴ
     */
    private Long orderLineId;

    /*
    티켓 발급 유저 id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

    /*
    발급 티켓의 item (양방향)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_item_id")
    private TicketItem ticketItem;

    /*
    발급 티켓의 옵션들 (단방향)
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "issued_ticket_id")
    private List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers = new ArrayList<>();

    public void addOptionAnswers(List<IssuedTicketOptionAnswer> answers) {
        issuedTicketOptionAnswers.addAll(answers);
    }

    /*
    발급 티켓 uuid
     */
    @Column(nullable = false)
    private String uuid;

    /*
    발급 티켓 가격
     */
    private Money price;

    /*
    상태
     */
    @Enumerated(EnumType.STRING)
    private IssuedTicketStatus issuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE;

    /*
    빌더를 통해 객체 생성 시 List는 큰 의미를 두지 않지만
    new ArrayList<>()로 한 번 초기화 시켜주면 NPE를 방지 할 수 있음
     */
    @Builder
    public IssuedTicket(
            Event event,
            User user,
            Long orderLineId,
            TicketItem ticketItem,
            Money price,
            IssuedTicketStatus issuedTicketStatus,
            List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers) {
        this.event = event;
        this.user = user;
        this.orderLineId = orderLineId;
        this.ticketItem = ticketItem;
        this.price = price;
        this.issuedTicketStatus = issuedTicketStatus;
        this.issuedTicketOptionAnswers.addAll(issuedTicketOptionAnswers);
    }

    public static IssuedTicket create(CreateIssuedTicketRequestForDev dto) {
        return IssuedTicket.builder()
                .event(dto.getEvent())
                .user(dto.getUser())
                .orderLineId(dto.getOrderLineId())
                .ticketItem(dto.getTicketItem())
                .price(dto.getPrice())
                .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
                .issuedTicketOptionAnswers(new ArrayList<>())
                .build();
    }

    public static IssuedTicket createForDev(CreateIssuedTicketRequestForDev dto) {
        IssuedTicket createIssuedTicket =
                IssuedTicket.builder()
                        .event(dto.getEvent())
                        .user(dto.getUser())
                        .orderLineId(dto.getOrderLineId())
                        .ticketItem(dto.getTicketItem())
                        .price(dto.getPrice())
                        .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
                        .issuedTicketOptionAnswers(new ArrayList<>())
                        .build();
        createIssuedTicket
                .getIssuedTicketOptionAnswers()
                .addAll(dto.getIssuedTicketOptionAnswers());
        return createIssuedTicket;
    }

    @PrePersist
    public void createUUID() {
        this.uuid = UUID.randomUUID().toString();
    }

    @PostPersist
    public void createIssuedTicketNo() {
        this.issuedTicketNo = "T" + this.id;
    }

    public Money sumOptionPrice() {
        return issuedTicketOptionAnswers.stream()
                .map(
                        issuedTicketOptionAnswer ->
                                issuedTicketOptionAnswer.getOption().getAdditionalPrice())
                .reduce(Money.ZERO, Money::plus);
    }

    public IssuedTicketInfoVo toIssuedTicketInfoVo() {
        return IssuedTicketInfoVo.from(this);
    }

    public static CreateIssuedTicketResponse orderLineItemToIssuedTickets(
            CreateIssuedTicketDTO dto) {
        long quantity = dto.getOrderLineItem().getQuantity();
        List<IssuedTicket> createIssuedTickets = new ArrayList<>();
        List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers =
                dto.getOrderLineItem().getOrderOptionAnswer().stream()
                        .map(IssuedTicketOptionAnswer::orderOptionAnswerToIssuedTicketOptionAnswer)
                        .toList();
        for (long i = 1; i <= quantity; i++) {
            createIssuedTickets.add(
                    IssuedTicket.builder()
                            .event(dto.getOrderLineItem().getTicketItem().getEvent())
                            .orderLineId(dto.getOrderLineItem().getId())
                            .user(dto.getUser())
                            .price(dto.getOrderLineItem().getTicketItem().getPrice())
                            .ticketItem(dto.getOrderLineItem().getTicketItem())
                            .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
                            .issuedTicketOptionAnswers(issuedTicketOptionAnswers)
                            .build());
        }
        return new CreateIssuedTicketResponse(createIssuedTickets, issuedTicketOptionAnswers);
    }
}
