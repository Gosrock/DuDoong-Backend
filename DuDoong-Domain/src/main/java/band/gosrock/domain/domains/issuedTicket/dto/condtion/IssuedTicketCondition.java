package band.gosrock.domain.domains.issuedTicket.dto.condtion;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IssuedTicketCondition {

    private Long eventId;

    private String userName;

    private String phoneNumber;
}
