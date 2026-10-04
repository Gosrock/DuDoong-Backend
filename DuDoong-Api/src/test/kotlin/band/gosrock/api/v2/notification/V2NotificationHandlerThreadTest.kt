package band.gosrock.api.v2.notification

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.notification.handler.V2NotificationAsyncConfig
import band.gosrock.api.v2.notification.handler.V2NotificationEventHandler
import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import java.util.concurrent.ConcurrentHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionalEventListener

/**
 * DEC-023 #2 (#721): 알림 저장 핸들러는 기존 `@Async` 기본 풀(`async-`)이 아니라 알림 전용 풀(`notification-`)에서 돈다.
 * `@Async(NOTIFICATION_EXECUTOR)` 를 `@Async` 로 바꾸면 기본 풀 스레드에서 실행되어 실패한다.
 * 서비스는 목으로 바꿔 호출 스레드 이름만 기록한다 ([V2NotificationFailureIsolationTest] 와 같은 컨텍스트 구성 — 캐시 재사용).
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 알림센터 - 핸들러 실행 스레드")
class V2NotificationHandlerThreadTest : V2OperationTestSupport() {

    @MockBean private lateinit var notificationDomainService: V2NotificationDomainService

    @Autowired private lateinit var handler: V2NotificationEventHandler

    @Test
    fun `멤버 추가·주문 생성·승인·거절 알림 저장은 notification- 스레드에서 실행된다`() {
        val threads = ConcurrentHashMap<String, String>()
        fun record(name: String): Int = 0.also { threads[name] = Thread.currentThread().name }
        given(notificationDomainService.notifyHostMembersAdded(anyLong(), anyList())).willAnswer { record("membersAdded") }
        given(notificationDomainService.notifyOrderPendingApprove(anyString())).willAnswer { record("pendingApprove") }
        given(notificationDomainService.notifyOrderApproved(anyString())).willAnswer { record("approved") }
        given(notificationDomainService.notifyOrderRefused(anyString())).willAnswer { record("refused") }

        val shop = Shop()
        val approved = shop.approved(newBuyer())
        val refused = shop.order(newBuyer())
        refuse(shop.team.manager, shop.eventId, refused, "SOLD_OUT").andExpect { status { isOk() } }

        verify(notificationDomainService, timeout(10_000)).notifyHostMembersAdded(anyLong(), anyList())
        verify(notificationDomainService, timeout(10_000)).notifyOrderApproved(approved)
        verify(notificationDomainService, timeout(10_000)).notifyOrderPendingApprove(refused)
        verify(notificationDomainService, timeout(10_000)).notifyOrderRefused(refused)

        assertEquals(setOf("membersAdded", "pendingApprove", "approved", "refused"), threads.keys)
        threads.forEach { (name, thread) ->
            assertTrue(thread.startsWith("notification-"), "$name 알림이 전용 풀이 아닌 스레드에서 실행됨: $thread")
        }
    }

    /** 실행 경로를 만들지 않은 핸들러(사용자 취소·환불 요청 등)까지 포함해, 리스너 메서드 전부가 전용 executor 를 지정했는지 본다 */
    @Test
    fun `모든 이벤트 리스너 메서드는 @Async(notificationExecutor) 를 지정한다`() {
        val listeners = handler.javaClass.declaredMethods.filter {
            AnnotatedElementUtils.hasAnnotation(it, TransactionalEventListener::class.java)
        }
        assertEquals(5, listeners.size, "리스너 수가 바뀌면 이 테스트와 실행 스레드 테스트를 갱신: ${listeners.map { it.name }}")
        listeners.forEach {
            val async = AnnotatedElementUtils.findMergedAnnotation(it, Async::class.java)
            assertEquals(V2NotificationAsyncConfig.NOTIFICATION_EXECUTOR, async?.value, "${it.name} 의 @Async executor")
        }
    }
}
