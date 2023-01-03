package band.gosrock.common.dto;


import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final boolean success = false;
    private final int status;
    private final String code;
    private final String reason;
    private final LocalDateTime timeStamp;

    private final String path;

    public ErrorResponse(ErrorReason errorReason, String path) {
        this.status = errorReason.getStatus();
        this.code = errorReason.getCode();
        this.reason = errorReason.getReason();
        this.timeStamp = LocalDateTime.now();
        this.path = path;
    }
}
