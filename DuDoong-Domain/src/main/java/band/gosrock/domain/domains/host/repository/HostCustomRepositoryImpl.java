package band.gosrock.domain.domains.host.repository;

import static band.gosrock.domain.domains.host.domain.QHost.host;
import static band.gosrock.domain.domains.host.domain.QHostUser.hostUser;

import band.gosrock.domain.common.util.SliceUtil;
import band.gosrock.domain.domains.host.domain.Host;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@RequiredArgsConstructor
public class HostCustomRepositoryImpl implements HostCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Host> querySliceHostsByUserId(Long userId, Pageable pageable) {
        List<Host> hosts =
                queryFactory
                        .select(host)
                        .from(host, hostUser)
                        .where(hostUser.userId.eq(userId), host.hostUsers.contains(hostUser))
                        .offset(pageable.getOffset())
                        .orderBy(host.id.desc())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();

        return SliceUtil.valueOf(hosts, pageable);
    }
}
