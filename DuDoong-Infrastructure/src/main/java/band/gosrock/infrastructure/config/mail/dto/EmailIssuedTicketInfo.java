package band.gosrock.infrastructure.config.mail.dto;


import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EmailIssuedTicketInfo {

    private final String issuedTicketNo;

    private final String ticketName;

    private final LocalDateTime createdAt;

    private final String issuedTicketStatus;

    private final String money;
}
