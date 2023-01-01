package band.gosrock.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuDoongCodeException extends RuntimeException{
    private ErrorCode errorCode;
}
