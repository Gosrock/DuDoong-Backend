package band.gosrock.api.v2.gift

import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.supports.ThreadConnections
import band.gosrock.domain.common.events.event.EventDeletionEvent
import band.gosrock.domain.common.events.event.EventStatusChangeEvent
import band.gosrock.domain.common.events.user.UserDeactivatedEvent
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.gift.domain.TicketGiftCancelReason
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.service.TicketGiftGuard
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.user.domain.AccountState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate

/**
 * 선물(#719) 후속 (#734): 탈퇴 경로 커넥션 수, v1 주문 목록 선물 판정 묶음 조회, 호스트 공연 비공개·삭제 연쇄.
 * 반환 ↔ 정지·수락 ↔ 공연 상태 변경의 잠금 순서는 MySQL E2E test_48 (test_21·22) 의 결정적 경합으로 검증한다 (H2 는 잠금 대기를 관찰할 수 없다)
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@Import(V2GiftFollowupTest.ConnectionProbe::class)
@DisplayName("v2 선물 후속 (#734)")
class V2GiftFollowupTest : V2GiftTestSupport() {

    @MockBean private lateinit var kakaoOauthHelper: KakaoOauthHelper

    @Autowired private lateinit var probe: ConnectionProbe

    @SpyBean private lateinit var ticketGiftGuard: TicketGiftGuard

    @Autowired private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired private lateinit var transactionManager: PlatformTransactionManager

    /** 탈퇴 커밋 직전(BEFORE_COMMIT, 선물 연쇄 뒤)에 이 스레드가 쥔 커넥션 수를 기록한다 */
    @TestConfiguration
    class ConnectionProbe {
        @Volatile var openAtCommit: Int = -1

        @TransactionalEventListener(classes = [UserDeactivatedEvent::class], phase = TransactionPhase.BEFORE_COMMIT)
        fun record(event: UserDeactivatedEvent) {
            openAtCommit = ThreadConnections.open
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 커넥션")
    inner class WithdrawConnections {

        @Test
        fun `회원 탈퇴(DELETE v1 auth me) - 바깥 트랜잭션 없이 탈퇴 트랜잭션 1개만 커넥션을 쥔다 (연쇄 후보 읽기를 더해도 최대 2개), 대기 선물 취소·카카오 연결 해제`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val g = giftOk(sender, uuids[0]).at("/giftId").asLong()
            val oid = userRepository.findById(sender.id!!).get().oauthInfo!!.oid!!
            ThreadConnections.reset()
            mockMvc.delete("/api/v1/auth/me") { with(auth(sender)) }.andExpect { status { isOk() } }
            assertEquals(1, probe.openAtCommit, "탈퇴 커밋 시점에 이 요청이 쥔 커넥션 수 (예전: 유스케이스·메서드 트랜잭션 + 탈퇴 락 트랜잭션 = 2)")
            assertEquals(2, ThreadConnections.peak, "최댓값 = 탈퇴 트랜잭션 + 연쇄 후보 읽기(V2TicketGiftCandidateReader, REQUIRES_NEW) (예전 3)")
            assertEquals(0, ThreadConnections.open, "요청이 끝나면 모두 반납")
            assertEquals(AccountState.DELETED, userRepository.findById(sender.id!!).get().accountState)
            assertEquals(TicketGiftCancelReason.SENDER_WITHDRAWN, giftOf(g).cancelReason)
            verify(kakaoOauthHelper).unlink(oid) // 탈퇴로 지워지기 전 oid
        }

        @Test
        fun `oid 가 없는 사용자 탈퇴 - 탈퇴는 되고 카카오 연결 해제는 건너뛴다`() {
            val user = newBuyer()
            userRepository.findById(user.id!!).get().also { ReflectionTestUtils.setField(it, "oauthInfo", null) }.let { userRepository.save(it) }
            mockMvc.delete("/api/v1/auth/me") { with(auth(user)) }.andExpect { status { isOk() } }
            assertEquals(AccountState.DELETED, userRepository.findById(user.id!!).get().accountState)
            verifyNoInteractions(kakaoOauthHelper)
        }
    }

    @Nested
    @DisplayName("v1 주문 목록 선물 판정")
    inner class OrderListGiftBlock {

        private fun myOrders(user: band.gosrock.domain.domains.user.domain.User) =
            mockMvc.get("/api/v1/orders") {
                with(auth(user))
                param("showing", "true")
                param("size", "10")
            }.andExpect { status { isOk() } }.data().at("/content")

        @Test
        fun `주문 3건 목록 - 선물 판정 조회는 주문 수와 무관하게 1번, 선물 대기·완료 주문만 환불 불가`() {
            val shop = Shop()
            val buyer = newBuyer()
            // 같은 요청 10초 안 재전송은 앞 주문을 돌려주므로(중복 방지) 입금자명을 달리한다
            fun approved(depositor: String): Pair<String, List<String>> {
                val orderUuid = v2OrderOk(buyer, shopBody(shop, depositorName = depositor)).at("/orderUuid").asText()
                v1Approve(shop.team.master, shop.eventId, orderUuid).andExpect { status { isOk() } }
                return orderUuid to shop.ticketUuids(orderUuid)
            }
            val (plain, _) = approved("입금자일")
            val (pending, pendingUuids) = approved("입금자이")
            giftOk(buyer, pendingUuids[0])
            val (sent, sentUuids) = approved("입금자삼")
            giveAndAccept(buyer, newBuyer(), sentUuids[0])

            clearInvocations(ticketGiftGuard)
            val rows = myOrders(buyer)
            // 목록 전체에 묶음 판정 1번(발급 티켓 IN 조회 1 + 선물 대기 IN 조회 1), 주문별 판정은 부르지 않는다 (예전: 주문마다 조회)
            verify(ticketGiftGuard, times(1)).userCancelBlockedOrderUuids(anyCollection())
            verify(ticketGiftGuard, never()).hasUserCancelBlockingGift(any(Order::class.java) ?: Order())
            fun refundable(orderUuid: String) = rows.first { it.at("/orderUuid").asText() == orderUuid }.at("/refundInfo/availAble").asBoolean()
            assertTrue(refundable(plain))
            assertFalse(refundable(pending))
            assertFalse(refundable(sent))
        }
    }

    @Nested
    @DisplayName("호스트 공연 비공개·삭제")
    inner class HostEventRemoval {

        @Test
        fun `호스트 경로로는 대기 선물이 있는 공연을 준비중·삭제로 바꿀 수 없다 (OPEN → 준비중 불가, OPEN·발급 티켓 공연 삭제 불가)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val g = giftOk(sender, uuids[0]).at("/giftId").asLong()
            mockMvc.patch("/api/v1/events/${shop.eventId}/status") {
                with(auth(shop.team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("status" to "PREPARING"))
            }.andExpect { status { is4xxClientError() } }
            mockMvc.patch("/api/v1/events/${shop.eventId}/delete") { with(auth(shop.team.master)) }.andExpect { status { is4xxClientError() } }
            mockMvc.delete("/api/v2/events/${shop.eventId}") { with(auth(shop.team.master)) }.andExpect { status { is4xxClientError() } }
            assertEquals(TicketGiftStatus.PENDING, giftOf(g).status)
        }

        @Test
        fun `호스트 준비중 전환·삭제 이벤트에도 운영 경로와 같이 대기 선물 CANCELED(EVENT_REMOVED), 진행·정산 전환은 그대로`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val g1 = giftOk(sender, uuids[0]).at("/giftId").asLong()
            val tx = TransactionTemplate(transactionManager)

            // 전이 규칙상 호스트 경로로는 만들 수 없는 상태라, 엔티티가 내는 이벤트를 같은 트랜잭션에서 직접 발행해 연쇄 연결을 본다
            tx.executeWithoutResult { eventPublisher.publishEvent(EventStatusChangeEvent(shop.team.hostId, shop.eventId, EventStatus.CALCULATING)) }
            assertEquals(TicketGiftStatus.PENDING, giftOf(g1).status)
            tx.executeWithoutResult { eventPublisher.publishEvent(EventStatusChangeEvent(shop.team.hostId, shop.eventId, EventStatus.PREPARING)) }
            assertEquals(TicketGiftCancelReason.EVENT_REMOVED, giftOf(g1).cancelReason)

            val g2 = giftOk(sender, uuids[1]).at("/giftId").asLong()
            tx.executeWithoutResult { eventPublisher.publishEvent(EventDeletionEvent(shop.team.hostId, "공연", shop.eventId)) }
            assertEquals(TicketGiftCancelReason.EVENT_REMOVED, giftOf(g2).cancelReason)
        }
    }
}
