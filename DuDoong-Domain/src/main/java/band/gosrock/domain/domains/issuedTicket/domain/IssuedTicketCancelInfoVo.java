package band.gosrock.domain.domains.issuedTicket.domain;


import java.time.LocalDateTime;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class IssuedTicketCancelInfoVo {

    private LocalDateTime canceledAt;

    @Enumerated(EnumType.STRING)
    private IssuedTicketCancelReason cancelReason;

    @Builder
    public IssuedTicketCancelInfoVo(
            LocalDateTime canceledAt, IssuedTicketCancelReason cancelReason) {
        this.canceledAt = canceledAt;
        this.cancelReason = cancelReason;
    }

    public static IssuedTicketCancelInfoVo of(
            LocalDateTime canceledAt, IssuedTicketCancelReason cancelReason) {
        return IssuedTicketCancelInfoVo.builder()
                .canceledAt(canceledAt)
                .cancelReason(cancelReason)
                .build();
    }
}
