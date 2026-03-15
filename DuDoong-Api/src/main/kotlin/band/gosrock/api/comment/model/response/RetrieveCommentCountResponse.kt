package band.gosrock.api.comment.model.response

data class RetrieveCommentCountResponse(
    val commentCounts: Long?,
) {
    companion object {
        @JvmStatic
        fun of(commentCounts: Long?): RetrieveCommentCountResponse =
            RetrieveCommentCountResponse(commentCounts = commentCounts)
    }
}
