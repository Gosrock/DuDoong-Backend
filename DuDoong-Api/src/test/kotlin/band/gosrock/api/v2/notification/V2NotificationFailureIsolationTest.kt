package band.gosrock.api.v2.notification

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import band.gosrock.domain.domains.order.domain.OrderStatus
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

/**
 * 알림 저장이 실패해도 원 작업(멤버 추가, 주문 생성·승인·거절)은 커밋된 그대로다 (#714).
 * 저장 서비스를 예외를 던지는 목으로 바꾸고, 핸들러가 실제로 호출됐는지(비동기) 확인한다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 알림센터 - 저장 실패 격리")
class V2NotificationFailureIsolationTest : V2OperationTestSupport() {

    @MockBean private lateinit var notificationDomainService: V2NotificationDomainService

    @Autowired private lateinit var hostRepository: HostRepository

    @Test
    fun `알림 저장 예외 - 멤버 추가, v1 주문 생성·승인, v2 거절 모두 성공하고 데이터 유지`() {
        val boom = RuntimeException("알림 저장 실패 (테스트)")
        given(notificationDomainService.notifyHostMembersAdded(anyLong(), anyList())).willThrow(boom)
        given(notificationDomainService.notifyOrderPendingApprove(anyString())).willThrow(boom)
        given(notificationDomainService.notifyOrderApproved(anyString())).willThrow(boom)
        given(notificationDomainService.notifyOrderRefused(anyString())).willThrow(boom)

        val shop = Shop()
        verify(notificationDomainService, timeout(10_000)).notifyHostMembersAdded(anyLong(), anyList())
        val host = hostRepository.findById(shop.team.hostId).get()
        assertTrue(host.isActiveHostUserId(shop.team.manager.id!!) && host.isActiveHostUserId(shop.team.guest.id!!))

        val approved = shop.approved(newBuyer())
        verify(notificationDomainService, timeout(10_000)).notifyOrderApproved(approved)
        assertEquals(OrderStatus.APPROVED, orderRepository.findByUuidIn(listOf(approved)).single().orderStatus)
        assertEquals(1, issuedTicketRepository.findAllByOrderUuid(approved).size)

        val refused = shop.order(newBuyer())
        verify(notificationDomainService, timeout(10_000)).notifyOrderPendingApprove(refused)
        refuse(shop.team.manager, shop.eventId, refused, "SOLD_OUT").andExpect { status { isOk() } }
        verify(notificationDomainService, timeout(10_000)).notifyOrderRefused(refused)
        assertEquals(OrderStatus.CANCELED, orderRepository.findByUuidIn(listOf(refused)).single().orderStatus)
    }
}
