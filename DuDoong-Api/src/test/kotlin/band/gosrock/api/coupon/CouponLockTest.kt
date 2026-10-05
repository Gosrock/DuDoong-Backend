package band.gosrock.api.coupon

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.coupon.repository.CouponCampaignRepository
import band.gosrock.domain.domains.coupon.repository.IssuedCouponRepository
import band.gosrock.domain.domains.coupon.service.RecoveryCouponService
import band.gosrock.domain.domains.coupon.service.UseCouponService
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.user.domain.User
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.post

/**
 * v1 쿠폰 (#746): 쿠폰 사용·회복 락 키(파라미터 이름 불일치로 BadLockIdentifier — 쿠폰 주문이 늘 실패), 발급 재고 갱신 유실(락 밖에서 읽은 캠페인을 호출 측 커밋 때 덮어씀).
 * 동시 발급의 결정적 경합은 MySQL E2E test_25
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v1 쿠폰 - 락 키·발급 재고 (#746)")
class CouponLockTest : V2OperationTestSupport() {

    @Autowired private lateinit var couponCampaignRepository: CouponCampaignRepository

    @Autowired private lateinit var issuedCouponRepository: IssuedCouponRepository

    @Autowired private lateinit var hostRepository: HostRepository

    @Autowired private lateinit var useCouponService: UseCouponService

    @Autowired private lateinit var recoveryCouponService: RecoveryCouponService

    private val fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    private fun campaign(stock: Long): String {
        val admin = superAdmin()
        val code = "C${UUID.randomUUID().toString().take(8)}"
        mockMvc.post("/api/v1/coupons/campaigns") {
            with(user(admin.id.toString()).roles("SUPER_ADMIN"))
            contentType = MediaType.APPLICATION_JSON
            content = json(
                mapOf(
                    "discountType" to "AMOUNT", "applyTarget" to "ALL", "validTerm" to 30,
                    "startAt" to LocalDateTime.now().minusMinutes(1).format(fmt), "endAt" to LocalDateTime.now().plusDays(30).format(fmt),
                    "issuedAmount" to stock, "discountAmount" to 1000, "couponCode" to code, "minimumCost" to 10000,
                ),
            )
        }.andExpect { status { isOk() } }
        return code
    }

    private fun issue(user: User, code: String) = mockMvc.post("/api/v1/coupons/campaigns/$code") { with(auth(user)) }

    private fun remaining(code: String) = couponCampaignRepository.findByCouponCode(code).get().couponStockInfo!!.remainingAmount!!

    /** 쿠폰 주문은 v1 유료(PG) 선착순 티켓만 된다 — 제휴 호스트에서 v1 API 로 만든다 */
    private fun priceTicket(shop: Shop): Long {
        hostRepository.findById(shop.team.hostId).get().also { it.changePartner(true) }.let { hostRepository.save(it) }
        return mockMvc.post("/api/v1/events/${shop.eventId}/ticketItems") {
            with(auth(shop.team.master))
            contentType = MediaType.APPLICATION_JSON
            content = json(
                mapOf(
                    "payType" to "유료티켓", "name" to "카드", "description" to "카드 결제", "price" to 20000, "supplyCount" to 10,
                    "approveType" to "선착순", "isQuantityPublic" to true, "purchaseLimit" to 4,
                ),
            )
        }.andExpect { status { isOk() } }.data().at("/ticketItemId").asLong()
    }

    @Test
    fun `쿠폰 적용 v1 주문 생성 - 쿠폰 사용 락(키 = issuedCouponId)이 걸리고 쿠폰이 사용 처리된다`() {
        val shop = Shop()
        val ticketId = priceTicket(shop)
        val code = campaign(stock = 10)
        val buyer = newBuyer()
        val issuedCouponId = issue(buyer, code).andExpect { status { isOk() } }.data().at("/issuedCouponId").asLong()

        val cartId = v1Cart(buyer, ticketId).andExpect { status { isOk() } }.data().at("/cartId").asLong()
        val orderUuid = mockMvc.post("/api/v1/orders/") {
            with(auth(buyer))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("cartId" to cartId, "couponId" to issuedCouponId))
        }.andExpect { status { isOk() } }.data().at("/orderId").asText()
        assertTrue(issuedCouponRepository.findById(issuedCouponId).get().usageStatus, "주문 생성(BEFORE_COMMIT 핸들러) 때 쿠폰 사용")
        assertEquals(issuedCouponId, orderRepository.findByOrderUuid(orderUuid).get().orderCouponVo.couponId)
    }

    @Test
    fun `쿠폰 회복 - 주문 철회·결제 확정 실패 핸들러가 부르는 회복이 락(키 = issuedCouponId) 안에서 되고, 남의 쿠폰은 거부`() {
        val code = campaign(stock = 10)
        val buyer = newBuyer()
        val issuedCouponId = issue(buyer, code).andExpect { status { isOk() } }.data().at("/issuedCouponId").asLong()
        useCouponService.execute(buyer.id!!, issuedCouponId)
        assertTrue(issuedCouponRepository.findById(issuedCouponId).get().usageStatus)
        recoveryCouponService.execute(buyer.id!!, issuedCouponId)
        assertFalse(issuedCouponRepository.findById(issuedCouponId).get().usageStatus)
        val other = newBuyer()
        assertThrows<DuDoongCodeException> { useCouponService.execute(other.id!!, issuedCouponId) }
        assertFalse(issuedCouponRepository.findById(issuedCouponId).get().usageStatus)
    }

    @Test
    fun `동시 발급 - 발급 수만큼 재고가 줄어든다 (락 안에서 캠페인을 다시 읽어 줄이고 저장)`() {
        val code = campaign(stock = 100)
        val users = (1..8).map { newBuyer("발급$it") }
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(users.size)
        val statuses = Collections.synchronizedList(mutableListOf<Int>())
        users.forEach { u -> pool.submit { start.await(); statuses += issue(u, code).andReturn().response.status } }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))
        assertEquals(List(users.size) { 200 }, statuses)
        assertEquals(100L - users.size, remaining(code), "발급 ${users.size}건 → 재고 ${100 - users.size}")
    }

    @Test
    fun `순차 발급 - 재고가 하나씩 줄고, 같은 사람 재발급은 거부, 재고가 없으면 거부`() {
        val code = campaign(stock = 2)
        val a = newBuyer()
        issue(a, code).andExpect { status { isOk() } }
        assertEquals(1L, remaining(code))
        issue(a, code).andExpect { status { isBadRequest() } }
        assertEquals(1L, remaining(code))
        issue(newBuyer(), code).andExpect { status { isOk() } }
        assertEquals(0L, remaining(code))
        issue(newBuyer(), code).andExpect { status { is4xxClientError() } }
        assertEquals(0L, remaining(code))
    }
}
