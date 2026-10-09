package band.gosrock.domain.domains.host.repository

import band.gosrock.domain.common.util.SliceUtil
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.domain.QEvent.event
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.QHost.host
import band.gosrock.domain.domains.host.domain.QHostUser.hostUser
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.QUser.user
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

class HostCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager,
) : HostCustomRepository {

    override fun findByIdForUpdate(hostId: Long): Host? {
        val found = entityManager.find(Host::class.java, hostId) ?: return null
        // 권한 AOP 등에서 먼저 읽어 캐시된 엔티티일 수 있으므로 락과 함께 다시 읽는다
        entityManager.refresh(found, LockModeType.PESSIMISTIC_WRITE)
        return found
    }

    override fun existsActiveHostMasteredBy(userId: Long, eventStatuses: Collection<EventStatus>): Boolean {
        val otherMember = JPAExpressions.selectOne()
            .from(hostUser, user)
            .where(
                hostUser.host.eq(host),
                hostUserActive(),
                hostUser.userId.ne(userId),
                user.id.eq(hostUser.userId),
                user.accountState.ne(AccountState.DELETED),
            )
        val activeEvent = JPAExpressions.selectOne()
            .from(event)
            .where(event.hostId.eq(host.id), event.status.`in`(eventStatuses))
        return queryFactory
            .selectOne()
            .from(host)
            .where(host.masterUserId.eq(userId), otherMember.exists().or(activeEvent.exists()))
            .fetchFirst() != null
    }

    override fun flushChanges() {
        entityManager.flush()
    }

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

    override fun findAllForAdmin(keyword: String?, pageable: Pageable): Page<Host> {
        val keywordCondition: BooleanExpression? =
            if (!keyword.isNullOrBlank()) host.profile.name.contains(keyword) else null

        val hosts = queryFactory
            .select(host)
            .from(host)
            .where(keywordCondition)
            .offset(pageable.offset)
            .orderBy(hostIdDesc())
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(host.count())
            .from(host)
            .where(keywordCondition)
            .fetchOne() ?: 0L

        return PageImpl(hosts, pageable, total)
    }

    override fun queryPageHostsByActiveUserId(userId: Long, keyword: String?, pageable: Pageable): Page<Host> {
        val keywordCondition: BooleanExpression? =
            if (!keyword.isNullOrBlank()) host.profile.name.contains(keyword) else null

        val hosts = queryFactory
            .select(host)
            .from(host, hostUser)
            .where(hostUserIdEq(userId), host.hostUsers.contains(hostUser), hostUserActive(), keywordCondition)
            .offset(pageable.offset)
            .orderBy(hostIdDesc())
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(host.count())
            .from(host, hostUser)
            .where(hostUserIdEq(userId), host.hostUsers.contains(hostUser), hostUserActive(), keywordCondition)
            .fetchOne() ?: 0L

        return PageImpl(hosts, pageable, total)
    }

    private fun hostUserIdEq(userId: Long): BooleanExpression = hostUser.userId.eq(userId)
    private fun hostUserActive(): BooleanExpression = hostUser.active.isTrue
    private fun hostIdDesc(): OrderSpecifier<Long> = host.id.desc()
}
