package band.gosrock.domain.domains.comment.repository

import band.gosrock.domain.common.util.SliceUtil
import band.gosrock.domain.domains.comment.domain.Comment
import band.gosrock.domain.domains.comment.domain.CommentStatus
import band.gosrock.domain.domains.comment.domain.QComment.comment
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition
import band.gosrock.domain.domains.user.domain.QUser.user
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Slice

class CommentCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CommentCustomRepository {

    override fun searchToPage(commentCondition: CommentCondition): Slice<Comment> {
        val comments = queryFactory
            .selectFrom(comment)
            .leftJoin(comment.user, user)
            .fetchJoin()
            .where(
                eventIdEq(commentCondition.eventId),
                comment.commentStatus.eq(CommentStatus.ACTIVE),
            )
            .orderBy(comment.id.desc())
            .offset(commentCondition.pageable.offset)
            .limit((commentCondition.pageable.pageSize + 1).toLong())
            .fetch()

        return SliceUtil.valueOf(comments, commentCondition.pageable)
    }

    private fun eventIdEq(eventId: Long?): BooleanExpression? =
        eventId?.let { comment.eventId.eq(it) }

    override fun countComment(eventId: Long): Long =
        queryFactory
            .select(comment.count())
            .from(comment)
            .where(eventIdEq(eventId), comment.commentStatus.eq(CommentStatus.ACTIVE))
            .fetchOne() ?: 0L
}
