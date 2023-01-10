package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.dto.request.PostIssuedTicketRequest;
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
    Todo: 발급 티켓이 굳이 order line 을 알아야 할까?
     */
    //    private Long orderLineId;

    /*
    티켓 발급 유저 id
     */
    private Long userId;

    /*
    발급 티켓의 item (양방향)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_item_id")
    private TicketItem ticketItem;

    /*
    발급 티켓의 옵션들 (양방향)
     */
    @OneToMany(mappedBy = "issuedTicket", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<IssuedTicketOptionGroup> issuedTicketOptionGroups = new ArrayList<>();

    public void insertIssuedTicketOptionGroups(IssuedTicketOptionGroup issuedTicketOptionGroup) {
        issuedTicketOptionGroups.add(issuedTicketOptionGroup);
    }

    /*
    발급 티켓 uuid
     */
    @Column(nullable = false)
    private String uuid;

    /*
    발급 티켓 가격
     */
    private Long price;

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
            Long userId,
            TicketItem ticketItem,
            Long price,
            IssuedTicketStatus issuedTicketStatus,
            List<IssuedTicketOptionGroup> issuedTicketOptionGroups) {
        this.event = event;
        this.userId = userId;
        this.ticketItem = ticketItem;
        this.price = price;
        this.issuedTicketStatus = issuedTicketStatus;
        this.issuedTicketOptionGroups = issuedTicketOptionGroups;
    }

    public static IssuedTicket create(PostIssuedTicketRequest dto) {
        return IssuedTicket.builder()
            .event(dto.getEvent())
            .userId(dto.getUserId())
            .ticketItem(dto.getTicketItem())
            .price(dto.getPrice())
            .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
            .issuedTicketOptionGroups(new ArrayList<>())
            .build();
    }

    @PrePersist
    public void createUUID() {
        this.uuid = UUID.randomUUID().toString();
    }

    @PostPersist
    public void createIssuedTicketNo() {
        this.issuedTicketNo = "T" + this.id;
    }
}
