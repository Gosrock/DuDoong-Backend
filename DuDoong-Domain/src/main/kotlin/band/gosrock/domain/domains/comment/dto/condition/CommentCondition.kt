package band.gosrock.domain.domains.comment.dto.condition

import org.springframework.data.domain.Pageable

data class CommentCondition(
    val eventId: Long,
    val pageable: Pageable,
)
