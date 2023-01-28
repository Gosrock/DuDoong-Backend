package band.gosrock.domain.domains.comment.exception;

import band.gosrock.common.exception.DuDoongCodeException;

public class CommentAlreadyDeleteException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CommentAlreadyDeleteException();

    private CommentAlreadyDeleteException() {
        super(CommentErrorCode.COMMENT_ALREADY_DELETE);
    }
}
