package io.github.depromeet.knockknockbackend.global.utils.api.exception;


import io.github.depromeet.knockknockbackend.global.error.exception.ErrorCode;
import io.github.depromeet.knockknockbackend.global.error.exception.KnockException;

public class OtherServerUnauthorizedException extends KnockException {

    public static final KnockException EXCEPTION = new OtherServerUnauthorizedException();

    private OtherServerUnauthorizedException() {
        super(ErrorCode.OTHER_SERVER_UNAUTHORIZED);
    }
}
