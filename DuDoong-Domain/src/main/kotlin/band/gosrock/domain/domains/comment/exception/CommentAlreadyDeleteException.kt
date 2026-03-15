package band.gosrock.domain.domains.comment.exception

import band.gosrock.common.exception.DuDoongCodeException

class CommentAlreadyDeleteException : DuDoongCodeException(CommentErrorCode.COMMENT_ALREADY_DELETE) {
    companion object {
        val EXCEPTION: DuDoongCodeException = CommentAlreadyDeleteException()
    }
}
