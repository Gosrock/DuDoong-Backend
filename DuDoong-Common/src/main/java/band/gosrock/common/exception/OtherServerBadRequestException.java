package io.github.depromeet.knockknockbackend.global.utils.api.exception;


import io.github.depromeet.knockknockbackend.global.error.exception.ErrorCode;
import io.github.depromeet.knockknockbackend.global.error.exception.KnockException;

public class OtherServerBadRequestException extends KnockException {

    public static final KnockException EXCEPTION = new OtherServerBadRequestException();

    private OtherServerBadRequestException() {
        super(ErrorCode.OTHER_SERVER_BAD_REQUEST);
    }
}
