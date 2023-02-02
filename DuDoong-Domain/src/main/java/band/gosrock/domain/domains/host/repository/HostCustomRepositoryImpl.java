package band.gosrock.domain.domains.host.repository;

import static band.gosrock.domain.domains.host.domain.QHost.host;
import static band.gosrock.domain.domains.host.domain.QHostUser.hostUser;

import band.gosrock.domain.common.util.QueryDslUtil;
import band.gosrock.domain.domains.host.domain.Host;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class HostCustomRepositoryImpl implements HostCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Host> querySliceHostsByUserId(Long userId, Pageable pageable) {
        OrderSpecifier[] orders = QueryDslUtil.getOrderSpecifiers(Host.class, pageable);
        List<Host> comments =
                queryFactory
                        .select(host)
                        .from(host, hostUser)
                        .where(hostUser.userId.eq(userId), host.hostUsers.contains(hostUser))
                        .offset(pageable.getOffset())
                        .orderBy(orders)
                        .limit(pageable.getPageSize() + 1)
                        .fetch();

        return checkLastPage(comments, pageable);
    }

    private Slice<Host> checkLastPage(List<Host> hosts, Pageable pageable) {
        boolean hasNext = false;
        if (hosts.size() > pageable.getPageSize()) {
            hasNext = true;
            hosts.remove(pageable.getPageSize());
        }
        return new SliceImpl<>(hosts, pageable, hasNext);
    }
}
