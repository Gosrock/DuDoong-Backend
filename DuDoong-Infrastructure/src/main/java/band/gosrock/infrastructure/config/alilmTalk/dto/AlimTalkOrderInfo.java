package band.gosrock.infrastructure.config.alilmTalk.dto;


import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AlimTalkOrderInfo {
    private final String name;
    private final Long quantity;
    private final String money;
    private final LocalDateTime createAt;
}
