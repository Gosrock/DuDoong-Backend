package band.gosrock.domain.domains.comment.exception

import band.gosrock.common.exception.DuDoongCodeException

class CommentNotFoundException : DuDoongCodeException(CommentErrorCode.COMMENT_NOT_FOUND) {
    companion object {
        val EXCEPTION: DuDoongCodeException = CommentNotFoundException()
    }
}
