package band.gosrock.domain.domains.comment.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class RetrieveRandomCommentNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION =
            new RetrieveRandomCommentNotFoundException();

    private RetrieveRandomCommentNotFoundException() {
        super(CommentErrorCode.RETRIEVE_RANDOM_COMMENT_NOT_FOUND);
    }
}
