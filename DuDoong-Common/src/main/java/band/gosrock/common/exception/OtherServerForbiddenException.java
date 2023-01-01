package io.github.depromeet.knockknockbackend.global.utils.api.exception;


import io.github.depromeet.knockknockbackend.global.error.exception.ErrorCode;
import io.github.depromeet.knockknockbackend.global.error.exception.KnockException;

public class OtherServerForbiddenException extends KnockException {

    public static final KnockException EXCEPTION = new OtherServerForbiddenException();

    private OtherServerForbiddenException() {
        super(ErrorCode.OTHER_SERVER_FORBIDDEN);
    }
}
