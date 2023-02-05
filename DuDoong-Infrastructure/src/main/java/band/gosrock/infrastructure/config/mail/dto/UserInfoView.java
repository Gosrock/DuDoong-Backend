package band.gosrock.infrastructure.config.mail.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserInfoView {
    private final String name;
    private final String email;
}
