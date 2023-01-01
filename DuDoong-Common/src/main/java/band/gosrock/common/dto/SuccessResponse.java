package band.gosrock.common.dto;


import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class SuccessResponse {

    private final boolean success = true;
    private final int status;
    private final Object data;
    private final LocalDateTime timeStamp;

    public SuccessResponse(int status, Object data) {
        this.status = status;
        this.data = data;
        this.timeStamp = LocalDateTime.now();
    }
}
