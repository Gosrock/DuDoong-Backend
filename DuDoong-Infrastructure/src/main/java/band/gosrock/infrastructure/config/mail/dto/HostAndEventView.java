package band.gosrock.infrastructure.config.mail.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HostAndEventView {
    private final String hostName;
    private final String eventName;
}
