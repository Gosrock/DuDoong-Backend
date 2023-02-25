package band.gosrock.infrastructure.config.mail.dto;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EmailUserInfo {
    private final String name;
    private final String email;
    private final Boolean receiveAgree;
}
