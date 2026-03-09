package band.gosrock.domain.domains.host.repository

import band.gosrock.domain.common.util.SliceUtil
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.QHost.host
import band.gosrock.domain.domains.host.domain.QHostUser.hostUser
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

class HostCustomRepositoryImpl(private val queryFactory: JPAQueryFactory) : HostCustomRepository {

    override fun querySliceHostsByUserId(userId: Long, pageable: Pageable): Slice<Host> {
        val hosts = queryFactory
            .select(host)
            .from(host, hostUser)
            .where(hostUserIdEq(userId), host.hostUsers.contains(hostUser))
            .offset(pageable.offset)
            .orderBy(hostIdDesc())
            .limit((pageable.pageSize + 1).toLong())
            .fetch()
        return SliceUtil.valueOf(hosts, pageable)
    }

    override fun queryHostsByActiveUserId(userId: Long): List<Host> =
        queryFactory
            .select(host)
            .from(host, hostUser)
            .where(hostUserIdEq(userId), host.hostUsers.contains(hostUser), hostUserActive())
            .fetch()

    override fun querySliceHostsByActiveUserId(userId: Long, pageable: Pageable): Slice<Host> {
        val hosts = queryFactory
            .select(host)
            .from(host, hostUser)
            .where(hostUserIdEq(userId), host.hostUsers.contains(hostUser), hostUserActive())
            .offset(pageable.offset)
            .orderBy(hostIdDesc())
            .limit((pageable.pageSize + 1).toLong())
            .fetch()
        return SliceUtil.valueOf(hosts, pageable)
    }

    private fun hostUserIdEq(userId: Long): BooleanExpression = hostUser.userId.eq(userId)
    private fun hostUserActive(): BooleanExpression = hostUser.active.isTrue
    private fun hostIdDesc(): OrderSpecifier<Long> = host.id.desc()
}
