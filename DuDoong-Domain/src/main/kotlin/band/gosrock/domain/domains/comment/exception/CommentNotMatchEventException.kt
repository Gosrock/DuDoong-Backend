package band.gosrock.domain.domains.comment.exception

import band.gosrock.common.exception.DuDoongCodeException

class CommentNotMatchEventException : DuDoongCodeException(CommentErrorCode.COMMENT_NOT_MATCH_EVENT) {
    companion object {
        val EXCEPTION: DuDoongCodeException = CommentNotMatchEventException()
    }
}
