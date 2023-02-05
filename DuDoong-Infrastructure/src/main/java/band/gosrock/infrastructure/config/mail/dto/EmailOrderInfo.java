package band.gosrock.infrastructure.config.mail.dto;

import band.gosrock.common.annotation.DateFormat;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EmailOrderInfo {
    private final String name;
    private final Long quantity;
    private final String money;
    private final LocalDateTime createAt;
}
