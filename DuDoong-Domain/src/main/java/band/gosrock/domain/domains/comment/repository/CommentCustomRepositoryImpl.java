package band.gosrock.domain.domains.comment.repository;

import static band.gosrock.domain.domains.comment.domain.QComment.comment;
import static band.gosrock.domain.domains.user.domain.QUser.user;

import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.domain.CommentStatus;
import band.gosrock.domain.domains.comment.dto.condition.CommentCondition;
import band.gosrock.domain.domains.comment.exception.RetrieveRandomCommentNotFoundException;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.support.PageableExecutionUtils;

@RequiredArgsConstructor
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Comment> searchToPage(CommentCondition commentCondition, Pageable pageable) {
        List<Comment> comments =
                queryFactory
                        .selectFrom(comment)
                        .leftJoin(comment.user, user)
                        .fetchJoin()
                        .where(
                                eventIdEq(commentCondition.getEventId()),
                                lastIdLessThanEqual(commentCondition.getLastId()),
                                comment.commentStatus.eq(CommentStatus.ACTIVE))
                        .orderBy(comment.id.desc())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();

        return checkLastPage(pageable, comments);
    }

    private BooleanExpression eventIdEq(Long eventId) {
        return eventId == null ? null : comment.eventId.eq(eventId);
    }

    private BooleanExpression lastIdLessThanEqual(Long lastId) {
        return lastId == null ? null : comment.id.loe(lastId);
    }

    private Slice<Comment> checkLastPage(Pageable pageable, List<Comment> comments) {

        boolean hasNext = false;

        if (comments.size() > pageable.getPageSize()) {
            hasNext = true;
            comments.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(comments, pageable, hasNext);
    }

    @Override
    public Long countComment(Long eventId) {
        return queryFactory
                .select(comment.count())
                .from(comment)
                .where(eventIdEq(eventId), comment.commentStatus.eq(CommentStatus.ACTIVE))
                .fetchOne();
    }

    @Override
    public Comment queryRandomComment(Long eventId, Long countComment) {
        int idx = (int) (Math.random() * countComment);
        PageRequest pageRequest = PageRequest.of(idx, 1);
        List<Comment> comments =
                queryFactory
                        .selectFrom(comment)
                        .where(eventIdEq(eventId), comment.commentStatus.eq(CommentStatus.ACTIVE))
                        .offset(pageRequest.getOffset())
                        .limit(1)
                        .fetch();

        JPAQuery<Long> countQuery =
                queryFactory
                        .select(comment.count())
                        .from(comment)
                        .where(eventIdEq(eventId), comment.commentStatus.eq(CommentStatus.ACTIVE));

        Page<Comment> commentOfPage =
                PageableExecutionUtils.getPage(comments, pageRequest, countQuery::fetchOne);

        if (commentOfPage.hasContent()) {
            return commentOfPage.getContent().get(0);
        }
        throw RetrieveRandomCommentNotFoundException.EXCEPTION;
    }
}
