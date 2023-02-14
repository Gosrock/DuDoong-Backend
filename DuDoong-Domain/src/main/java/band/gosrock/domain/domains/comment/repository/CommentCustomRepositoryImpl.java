package band.gosrock.domain.domains.comment.repository;

import static band.gosrock.domain.domains.comment.domain.QComment.comment;
import static band.gosrock.domain.domains.user.domain.QUser.user;

import band.gosrock.domain.common.util.SliceUtil;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.domain.CommentStatus;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;

@RequiredArgsConstructor
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Comment> searchToPage(CommentCondition commentCondition) {
        List<Comment> comments =
                queryFactory
                        .selectFrom(comment)
                        .leftJoin(comment.user, user)
                        .fetchJoin()
                        .where(
                                eventIdEq(commentCondition.getEventId()),
                                comment.commentStatus.eq(CommentStatus.ACTIVE))
                        .orderBy(comment.id.desc())
                        .offset(commentCondition.getPageable().getOffset())
                        .limit(commentCondition.getPageable().getPageSize() + 1)
                        .fetch();

        return SliceUtil.valueOf(comments, commentCondition.getPageable());
    }

    private BooleanExpression eventIdEq(Long eventId) {
        return eventId == null ? null : comment.eventId.eq(eventId);
    }

    //    private BooleanExpression lastIdLessThan(Long lastId) {
    //        return lastId == null ? null : comment.id.lt(lastId);
    //    }
    //
    //    private Slice<Comment> checkLastPage(Pageable pageable, List<Comment> comments) {
    //
    //        boolean hasNext = false;
    //
    //        if (comments.size() > pageable.getPageSize()) {
    //            hasNext = true;
    //            comments.remove(pageable.getPageSize());
    //        }
    //
    //        return new SliceImpl<>(comments, pageable, hasNext);
    //    }

    @Override
    public Long countComment(Long eventId) {
        return queryFactory
                .select(comment.count())
                .from(comment)
                .where(eventIdEq(eventId), comment.commentStatus.eq(CommentStatus.ACTIVE))
                .fetchOne();
    }
}
