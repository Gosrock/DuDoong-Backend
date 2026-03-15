package band.gosrock.domain.domains.comment.exception

import band.gosrock.common.exception.DuDoongCodeException

class RetrieveRandomCommentNotFoundException : DuDoongCodeException(CommentErrorCode.RETRIEVE_RANDOM_COMMENT_NOT_FOUND) {
    companion object {
        val EXCEPTION: DuDoongCodeException = RetrieveRandomCommentNotFoundException()
    }
}
