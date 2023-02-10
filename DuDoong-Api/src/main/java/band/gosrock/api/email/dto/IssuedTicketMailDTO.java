package band.gosrock.api.email.dto;


import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo;
import band.gosrock.infrastructure.config.mail.dto.EmailIssuedTicketInfo;
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IssuedTicketMailDTO {

    private final EmailUserInfo userInfo;

    private final EmailIssuedTicketInfo issuedTicketInfo;

    private final EmailEventInfo eventInfo;
}
