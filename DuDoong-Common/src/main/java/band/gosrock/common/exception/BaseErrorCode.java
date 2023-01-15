package band.gosrock.common.exception;


import band.gosrock.common.annotation.ErrorCode;
import band.gosrock.common.dto.ErrorReason;

@ErrorCode
public interface BaseErrorCode {
    public ErrorReason getErrorReason();
}
