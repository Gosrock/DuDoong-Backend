package io.github.depromeet.knockknockbackend.global.utils.api.exception;


import io.github.depromeet.knockknockbackend.global.error.exception.ErrorCode;
import io.github.depromeet.knockknockbackend.global.error.exception.KnockException;

public class OtherServerExpiredTokenException extends KnockException {

    public static final KnockException EXCEPTION = new OtherServerExpiredTokenException();

    private OtherServerExpiredTokenException() {
        super(ErrorCode.OTHER_SERVER_EXPIRED_TOKEN);
    }
}
