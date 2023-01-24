package band.gosrock.domain.domains.comment.repository;

import static band.gosrock.domain.domains.comment.domain.QComment.comment;
import static band.gosrock.domain.domains.user.domain.QUser.user;

import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

@RequiredArgsConstructor
public class CommentCustomRepositoryImpl implements CommentCustomRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Comment> searchToPage(CommentCondition commentCondition, Pageable pageable) {
        List<Comment> comments = queryFactory.selectFrom(comment).leftJoin(comment.user, user)
            .fetchJoin()
            .where(eventIdEq(commentCondition.getEventId()), lastIdLessThanEqual(
                commentCondition.getLastId())).offset(pageable.getOffset()).limit(
                pageable.getPageSize()).fetch();

        JPAQuery<Long> countQuery = queryFactory.select(comment.count()).from(comment)
            .where(eventIdEq(commentCondition.getEventId()));

        return PageableExecutionUtils.getPage(comments, pageable, countQuery::fetchOne);
    }

    private BooleanExpression eventIdEq(Long eventId) {
        return eventId == null ? null : comment.eventId.eq(eventId);
    }

    private BooleanExpression lastIdLessThanEqual(Long lastId) {
        return lastId == null ? null : comment.id.loe(lastId);
    }
}
