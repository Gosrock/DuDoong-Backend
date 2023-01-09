package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
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
import javax.persistence.OneToMany;
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

    /*
    발급 티켓의 이벤트 (양방향)
     */
    private Long eventId;

    /*
    발급 티켓의 주문 행 (단방향)
     */
    private Long orderLineId;

    /*
    티켓 발급 유저 (양방향)
     */
    private Long userId;

    /*
    발급 티켓의 item (양방향)
     */
    private Long ticketItemId;

    /*
    발급 티켓의 옵션들 (양방향)
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<IssuedTicketOptionGroup> issuedTicketOptionGroups = new ArrayList<>();

    /*
    발급 티켓 uuid
     */
    @Column(nullable = false)
    private String uuid;

    /*
    상태
     */
    @Enumerated(EnumType.STRING)
    private IssuedTicketStatus issuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE;

    @Builder
    public IssuedTicket(
            Long eventId,
            Long orderLineId,
            Long userId,
            Long ticketItemId,
            IssuedTicketStatus issuedTicketStatus) {
        this.eventId = eventId;
        this.orderLineId = orderLineId;
        this.userId = userId;
        this.ticketItemId = ticketItemId;
        this.issuedTicketStatus = issuedTicketStatus;
    }

    public static IssuedTicket createIssuedTicket(
            Long eventId,
            Long orderLineId,
            Long userId,
            Long ticketItemId) {
        return IssuedTicket.builder()
                .eventId(eventId)
                .orderLineId(orderLineId)
                .userId(userId)
                .ticketItemId(ticketItemId)
                .build();
    }

    @PrePersist
    public void createUUID() {
        this.uuid = UUID.randomUUID().toString();
    }
}
