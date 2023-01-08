package band.gosrock.common.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuDoongDynamicException extends RuntimeException {
    private final int status;
    private final String code;
    private final String reason;
}
