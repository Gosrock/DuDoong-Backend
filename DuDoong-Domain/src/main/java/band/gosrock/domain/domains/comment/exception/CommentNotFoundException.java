package band.gosrock.domain.domains.comment.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CommentNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CommentNotFoundException();

    private CommentNotFoundException() {
        super(CommentErrorCode.COMMENT_NOT_FOUND);
    }
}
