package band.gosrock.api.common

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.supports.ThreadConnections
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.UserNotFoundException
import band.gosrock.domain.domains.user.repository.UserRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import java.util.Collections
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * `@RedissonLock` 과 트랜잭션 AOP 순서 (#743). 락 메서드가 클래스 `@Transactional(readOnly = true)` 를 가진 도메인 서비스 모양일 때:
 * - 락 획득 → 새 트랜잭션: 락을 얻기 전에는 커넥션을 잡지 않고, 락 안 트랜잭션은 호출 측과 다른 새 트랜잭션·새 영속성 컨텍스트
 * - 락 해제는 커밋(커밋 후 동기화 콜백) 이후
 * - 도메인 이벤트(`Events.raise`)가 락 트랜잭션에 묶여 BEFORE_COMMIT·AFTER_COMMIT 으로 발행된다
 * - open-in-view holder 만 잠시 떼어 내고(SUPPORTS 동기화 holder 는 그대로), 예외여도 되돌린다
 * - 롤백 전파: 락 메서드 예외는 호출 측 트랜잭션을 rollback-only 로 만들지 않는다
 */
@ApiIntegrateSpringBootTest
@Import(RedissonLockTransactionOrderTest.LockedService::class, RedissonLockTransactionOrderTest.Listener::class)
@DisplayName("RedissonLock - 트랜잭션 AOP 순서 (#743)")
class RedissonLockTransactionOrderTest {

    class LockedEvent(val key: String) : DomainEvent()

    companion object {
        const val LOCK_NAME = "락순서테스트"

        val FAILURE: DuDoongCodeException = UserNotFoundException.EXCEPTION

        fun newUser() = User(
            profile = Profile(name = "락", email = "lock-${UUID.randomUUID()}@test.com", phoneNumber = null, profileImage = null),
            oauthInfo = OauthInfo(OauthProvider.KAKAO, "lock-${UUID.randomUUID()}"),
        )
    }

    /** 도메인 서비스와 같은 모양: 클래스 단위 읽기 전용 트랜잭션 + 락 메서드 */
    @Service
    @Transactional(readOnly = true)
    class LockedService(
        private val redissonClient: RedissonClient,
        private val userRepository: UserRepository,
        private val entityManager: EntityManager,
    ) {
        val observed: MutableMap<String, Any?> = Collections.synchronizedMap(mutableMapOf())

        @RedissonLock(LockName = LOCK_NAME, identifier = "key")
        fun locked(key: String, outsider: User?) {
            observed["lockedInside"] = redissonClient.getLock("$LOCK_NAME:$key").isLocked
            observed["connectionsInside"] = ThreadConnections.open
            observed["txActive"] = TransactionSynchronizationManager.isActualTransactionActive()
            observed["readOnly"] = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            observed["txName"] = TransactionSynchronizationManager.getCurrentTransactionName()
            observed["outsiderManaged"] = outsider?.let { entityManager.contains(it) }
            // 클래스가 읽기 전용이어도 락의 새 트랜잭션(읽기·쓰기)에 참여하므로 저장된다
            observed["savedUserId"] = userRepository.save(newUser()).id
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        observed["lockedAfterCommit"] = redissonClient.getLock("$LOCK_NAME:$key").isLocked
                    }
                },
            )
            Events.raise(LockedEvent(key))
        }

        /** 락 안에서 도메인 예외 (저장한 사용자는 락 트랜잭션과 함께 롤백) */
        @RedissonLock(LockName = LOCK_NAME, identifier = "key")
        fun lockedFailing(key: String) {
            observed["failedSavedUserId"] = userRepository.save(newUser()).id
            throw FAILURE
        }
    }

    class Listener(private val redissonClient: RedissonClient) {
        val beforeCommit: MutableList<Pair<String, Boolean>> = Collections.synchronizedList(mutableListOf())
        val afterCommit: MutableList<Pair<String, Boolean>> = Collections.synchronizedList(mutableListOf())

        @TransactionalEventListener(classes = [LockedEvent::class], phase = TransactionPhase.BEFORE_COMMIT)
        fun before(event: LockedEvent) {
            beforeCommit += event.key to redissonClient.getLock("$LOCK_NAME:${event.key}").isLocked
        }

        @TransactionalEventListener(classes = [LockedEvent::class], phase = TransactionPhase.AFTER_COMMIT)
        fun after(event: LockedEvent) {
            afterCommit += event.key to redissonClient.getLock("$LOCK_NAME:${event.key}").isLocked
        }
    }

    @Autowired private lateinit var service: LockedService

    @Autowired private lateinit var listener: Listener

    @Autowired private lateinit var redissonClient: RedissonClient

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var transactionManager: PlatformTransactionManager

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired private lateinit var sharedEntityManager: EntityManager

    @BeforeEach
    fun reset() {
        service.observed.clear()
        ThreadConnections.reset()
    }

    private fun key() = UUID.randomUUID().toString()

    @Test
    fun `호출 측 트랜잭션 없음 - 락을 얻기 전에는 커넥션 0개, 락 안은 새 읽기·쓰기 트랜잭션 1개, 락 해제는 커밋 뒤`() {
        val key = key()
        service.locked(key, null)
        assertEquals(1, ThreadConnections.peak, "클래스 @Transactional 이 락 바깥에서 먼저 시작하면 2 (락 대기 중 점유 + 락 트랜잭션)")
        assertEquals(1, service.observed["connectionsInside"])
        assertEquals(true, service.observed["txActive"])
        assertEquals(false, service.observed["readOnly"], "락의 새 트랜잭션(REQUIRES_NEW)이 바깥 — 클래스의 읽기 전용은 참여만 한다")
        assertTrue(userRepository.findById(service.observed["savedUserId"] as Long).isPresent, "락 트랜잭션에서 저장·커밋됨")
        assertEquals(true, service.observed["lockedInside"])
        assertEquals(true, service.observed["lockedAfterCommit"], "커밋 후 콜백 시점에도 락을 쥐고 있다 (해제는 커밋 이후)")
        assertFalse(redissonClient.getLock("$LOCK_NAME:$key").isLocked, "메서드가 끝나면 해제")
        assertEquals(0, ThreadConnections.open)
    }

    @Test
    fun `호출 측 트랜잭션 있음 - 락 안은 호출 측과 다른 새 트랜잭션, 락 안에서 저장한 것은 호출 측이 롤백돼도 남는다`() {
        val key = key()
        var outerTx: String? = null
        TransactionTemplate(transactionManager).apply { setName("outer-tx") }.execute { status ->
            outerTx = TransactionSynchronizationManager.getCurrentTransactionName()
            service.locked(key, null)
            status.setRollbackOnly()
        }
        assertEquals("outer-tx", outerTx)
        assertTrue(service.observed["txName"] != "outer-tx", "락 안 트랜잭션 이름 = ${service.observed["txName"]}")
        assertTrue(userRepository.findById(service.observed["savedUserId"] as Long).isPresent, "REQUIRES_NEW 로 따로 커밋")
        assertEquals(true, service.observed["lockedAfterCommit"])
    }

    @Test
    fun `open-in-view 처럼 EntityManager 가 스레드에 묶여 있어도 락 트랜잭션은 새 영속성 컨텍스트 (락 전에 읽은 엔티티가 보이지 않는다)`() {
        val em = entityManagerFactory.createEntityManager()
        TransactionSynchronizationManager.bindResource(entityManagerFactory, EntityManagerHolder(em))
        try {
            val outsider = em.find(User::class.java, userRepository.save(newUser()).id)
            assertTrue(em.contains(outsider))
            service.locked(key(), outsider)
            assertEquals(false, service.observed["outsiderManaged"], "락 트랜잭션이 open-in-view EntityManager 를 그대로 쓰면 true")
            assertTrue(TransactionSynchronizationManager.hasResource(entityManagerFactory), "락이 끝나면 원래 EntityManager 를 되돌린다")
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory)
            em.close()
        }
    }

    @Test
    fun `SUPPORTS 범위(트랜잭션 동기화가 묶은 EntityManager)에서 불러도 그 holder 는 건드리지 않는다 - 범위 정리·보류 때 IllegalStateException 없음`() {
        TransactionTemplate(transactionManager).apply { propagationBehavior = TransactionDefinition.PROPAGATION_SUPPORTS }.execute {
            // 공유 EntityManager 를 트랜잭션 없이(SUPPORTS) 쓰면 동기화에 묶인 holder 가 스레드에 묶인다 (저장소 메서드는 자체 트랜잭션이라 안 된다)
            sharedEntityManager.find(User::class.java, userRepository.save(newUser()).id!!)
            val holder = TransactionSynchronizationManager.getResource(entityManagerFactory) as EntityManagerHolder
            assertTrue(holder.isSynchronizedWithTransaction, "SUPPORTS 범위의 동기화 holder")
            service.locked(key(), null)
            assertSame(holder, TransactionSynchronizationManager.getResource(entityManagerFactory))
        }
        assertFalse(TransactionSynchronizationManager.hasResource(entityManagerFactory), "범위가 끝나면 스스로 풀린다")
        assertEquals(true, service.observed["lockedAfterCommit"])
    }

    @Test
    fun `락 메서드가 도메인 예외로 끝나도 open-in-view EntityManager 를 되돌리고 원래 예외를 그대로 던진다`() {
        val em = entityManagerFactory.createEntityManager()
        val holder = EntityManagerHolder(em)
        TransactionSynchronizationManager.bindResource(entityManagerFactory, holder)
        try {
            val thrown = assertThrows<DuDoongCodeException> { service.lockedFailing(key()) }
            assertSame(FAILURE, thrown)
            assertSame(holder, TransactionSynchronizationManager.getResource(entityManagerFactory), "예외여도 원래 holder 복원")
            assertFalse(userRepository.findById(service.observed["failedSavedUserId"] as Long).isPresent, "락 트랜잭션은 롤백")
        } finally {
            TransactionSynchronizationManager.unbindResourceIfPossible(entityManagerFactory)
            em.close()
        }
    }

    @Test
    fun `롤백 전파 - 락 메서드 예외를 호출 측이 잡으면 호출 측 트랜잭션은 rollback-only 가 아니다 (되돌리려면 호출 측이 직접 롤백)`() {
        var savedByCaller: Long? = null
        TransactionTemplate(transactionManager).execute { status ->
            assertThrows<DuDoongCodeException> { service.lockedFailing(key()) }
            assertFalse(status.isRollbackOnly, "예전(트랜잭션 AOP 가 바깥)에는 메서드 @Transactional 이 호출 측 트랜잭션에 참여해 rollback-only 로 표시 → 커밋 때 UnexpectedRollbackException")
            savedByCaller = userRepository.save(newUser()).id
        }
        assertTrue(userRepository.findById(savedByCaller!!).isPresent, "호출 측 트랜잭션은 그대로 커밋")
        assertFalse(userRepository.findById(service.observed["failedSavedUserId"] as Long).isPresent, "락 트랜잭션만 롤백")
    }

    @Test
    fun `도메인 이벤트 - 락 트랜잭션의 BEFORE_COMMIT(락 쥔 채)·AFTER_COMMIT(해제 전) 리스너가 받는다`() {
        val key = key()
        service.locked(key, null)
        assertEquals(listOf(key to true), listener.beforeCommit.filter { it.first == key })
        assertEquals(listOf(key to true), listener.afterCommit.filter { it.first == key })
    }
}
