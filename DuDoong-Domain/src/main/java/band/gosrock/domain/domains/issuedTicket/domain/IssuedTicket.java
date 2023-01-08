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

    private Long eventId;

    private Long orderLineId;

    private Long userId;

    private Long ticketItemId;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "issued_ticket_id")
    private List<IssuedTicketOptionGroup> issuedTicketOptionGroups = new ArrayList<>();

    @Column(nullable = false)
    private String uuid;

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
            Long ticketItemId,
            IssuedTicketStatus issuedTicketStatus) {
        return IssuedTicket.builder()
                .eventId(eventId)
                .orderLineId(orderLineId)
                .userId(userId)
                .ticketItemId(ticketItemId)
                .issuedTicketStatus(issuedTicketStatus)
                .build();
    }

    @PrePersist
    public void createUUID() {
        this.uuid = UUID.randomUUID().toString();
    }
}
