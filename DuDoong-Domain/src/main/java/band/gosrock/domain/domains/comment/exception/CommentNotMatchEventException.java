package band.gosrock.domain.domains.comment.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CommentNotMatchEventException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CommentNotMatchEventException();

    private CommentNotMatchEventException() {
        super(CommentErrorCode.COMMENT_NOT_MATCH_EVENT);
    }
}
