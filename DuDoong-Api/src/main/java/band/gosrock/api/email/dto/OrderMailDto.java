package band.gosrock.api.email.dto;


import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo;
import band.gosrock.infrastructure.config.mail.dto.EmailOrderInfo;
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderMailDto {
    private final EmailUserInfo userInfo;
    private final EmailOrderInfo orderInfo;
    private final EmailEventInfo eventInfo;
}
