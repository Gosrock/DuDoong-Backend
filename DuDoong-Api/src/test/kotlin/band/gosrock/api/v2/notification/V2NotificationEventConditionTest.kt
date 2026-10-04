package band.gosrock.api.v2.notification

import band.gosrock.api.v2.notification.handler.V2NotificationEventHandler
import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.common.events.order.CreateOrderEvent
import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.host.service.v2.V2HostMembersAddedEvent
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate

/**
 * 알림 핸들러의 `@TransactionalEventListener(condition)` 판정 (#714). 큐에 넣기 전에 거르는 조건을 실제 Spring 리스너 처리로 검증한다.
 * 작은 컨텍스트(@EnableAsync 없음 → 동기 호출)에서 트랜잭션 안에서 이벤트를 발행하고, 커밋 후 서비스 호출 여부를 본다.
 */
@DisplayName("v2 알림센터 - 리스너 condition")
class V2NotificationEventConditionTest {

    private val service: V2NotificationDomainService = SERVICE

    @AfterEach
    fun tearDown() = reset(service)

    private fun publishInTransaction(event: DomainEvent, commit: Boolean = true) {
        TransactionTemplate(CONTEXT.getBean(PlatformTransactionManager::class.java)).executeWithoutResult {
            CONTEXT.publishEvent(event)
            if (!commit) it.setRollbackOnly()
        }
    }

    private fun createOrder(method: OrderMethod) = newEvent(CreateOrderEvent::class.java, "uuid", 1L, false, method, null)

    private fun doneOrder(method: OrderMethod) = newEvent(DoneOrderEvent::class.java, "uuid", 1L, method, null, 1L)

    private fun withDraw(method: OrderMethod, status: OrderStatus) =
        newEvent(WithDrawOrderEvent::class.java, "uuid", 1L, method, status, true, status == OrderStatus.REFUND, null, 1L, false, null)

    @Test
    fun `주문 생성 - 승인형만 호출, 결제형은 큐에 넣지 않음`() {
        publishInTransaction(createOrder(OrderMethod.PAYMENT))
        verifyNoInteractions(service)
        publishInTransaction(createOrder(OrderMethod.APPROVAL))
        verify(service).notifyOrderPendingApprove("uuid")
    }

    @Test
    fun `주문 완료 - 승인형만 호출 (결제 확정·무료 확정 제외)`() {
        publishInTransaction(doneOrder(OrderMethod.PAYMENT))
        verifyNoInteractions(service)
        publishInTransaction(doneOrder(OrderMethod.APPROVAL))
        verify(service).notifyOrderApproved("uuid")
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus::class, names = ["CANCELED"], mode = EnumSource.Mode.EXCLUDE)
    fun `주문 철회 - CANCELED 가 아니면(환불 등) 호출 안 함`(status: OrderStatus) {
        publishInTransaction(withDraw(OrderMethod.APPROVAL, status))
        verifyNoInteractions(service)
    }

    @Test
    fun `주문 철회 - 승인형 CANCELED 만 호출, 결제형 CANCELED 는 호출 안 함`() {
        publishInTransaction(withDraw(OrderMethod.PAYMENT, OrderStatus.CANCELED))
        verifyNoInteractions(service)
        publishInTransaction(withDraw(OrderMethod.APPROVAL, OrderStatus.CANCELED))
        verify(service).notifyOrderRefused("uuid")
    }

    @Test
    fun `멤버 추가 - 조건 없이 호출, 롤백되면 호출 안 함`() {
        publishInTransaction(V2HostMembersAddedEvent(hostId = 1L, userIds = listOf(2L)), commit = false)
        verify(service, never()).notifyHostMembersAdded(1L, listOf(2L))
        publishInTransaction(V2HostMembersAddedEvent(hostId = 1L, userIds = listOf(2L)))
        verify(service).notifyHostMembersAdded(1L, listOf(2L))
    }

    /** 커밋·롤백만 흉내 내는 트랜잭션 매니저 (트랜잭션 동기화 = AFTER_COMMIT 리스너 실행) */
    class NoOpTransactionManager : AbstractPlatformTransactionManager() {
        override fun doGetTransaction(): Any = Any()
        override fun doBegin(transaction: Any, definition: org.springframework.transaction.TransactionDefinition) {}
        override fun doCommit(status: DefaultTransactionStatus) {}
        override fun doRollback(status: DefaultTransactionStatus) {}
    }

    /** @Configuration 을 붙이지 않는다: 통합 테스트 컴포넌트 스캔(band.gosrock)에 잡히면 다른 컨텍스트에 빈이 섞인다 */
    @EnableTransactionManagement
    class Config {
        @Bean fun transactionManager(): PlatformTransactionManager = NoOpTransactionManager()

        @Bean fun service(): V2NotificationDomainService = SERVICE

        @Bean fun handler(service: V2NotificationDomainService) = V2NotificationEventHandler(service)
    }

    companion object {
        private val SERVICE: V2NotificationDomainService = mock(V2NotificationDomainService::class.java)
        private val CONTEXT by lazy { AnnotationConfigApplicationContext(Config::class.java) }

        /** 도메인 이벤트는 private 생성자 + 엔티티 팩토리라, 필드 값만 지정해 만든다 */
        private fun <T> newEvent(type: Class<T>, vararg args: Any?): T =
            type.declaredConstructors.single { it.parameterCount == args.size }.let {
                it.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                it.newInstance(*args) as T
            }
    }
}
