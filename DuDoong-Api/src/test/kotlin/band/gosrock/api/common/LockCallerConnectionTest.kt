package band.gosrock.api.common

import band.gosrock.api.coupon.service.CreateUserCouponUseCase
import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.supports.LockConnectionProbe
import band.gosrock.api.supports.ThreadConnections
import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.domain.common.vo.DateTimePeriod
import band.gosrock.domain.domains.coupon.domain.CouponCampaign
import band.gosrock.domain.domains.coupon.domain.CouponStockInfo
import band.gosrock.domain.domains.coupon.domain.DiscountType
import band.gosrock.domain.domains.coupon.repository.CouponCampaignRepository
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.service.v2.V2OrderDomainService
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

/**
 * 호출 측 트랜잭션이 v1 락을 기다리며 커넥션을 쥐지 않는지 (#746, #743 후속). 테스트 스레드에서 직접 부르므로 open-in-view 영향이 없다.
 * 측정값은 이 스레드의 최대 동시 커넥션 수 (`ThreadConnections`). open-in-view 가 켜진 요청(MockMvc·prod)에서는 락 전 조회의 커넥션이 남아 1 씩 더 많다
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("락 호출부 커넥션 (#746)")
class LockCallerConnectionTest : V2OperationTestSupport() {

    @Autowired private lateinit var v2OrderDomainService: V2OrderDomainService

    @Autowired private lateinit var createUserCouponUseCase: CreateUserCouponUseCase

    @Autowired private lateinit var couponCampaignRepository: CouponCampaignRepository

    private fun peakOf(block: () -> Unit): Int {
        ThreadConnections.reset()
        block()
        assertEquals(0, ThreadConnections.open, "모두 반납")
        return ThreadConnections.peak
    }

    @Test
    fun `v2 승인·취소 - v1 주문 락을 트랜잭션 밖에서 기다린다 (최대 2 이하 = 락 트랜잭션 + 그 안의 발급·철회 락 트랜잭션, 예전 3)`() {
        val shop = Shop()
        val orderUuid = shop.order(newBuyer())
        val approvePeak = peakOf { v2OrderDomainService.approve(shop.eventId, orderUuid) }
        assertTrue(approvePeak <= 2, "최대 $approvePeak — 락 트랜잭션 1 + 그 안의 발급 락 트랜잭션 1 까지만 (호출 측 트랜잭션이 락을 기다리며 쥐면 3)")
        assertNull(LockConnectionProbe.lastCallerTxOf("OrderApproveService.execute"), "v1 주문 락을 부를 때 진행 중인 호출 측 트랜잭션이 없다")
        assertEquals(OrderStatus.APPROVED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
        val cancelPeak = peakOf { v2OrderDomainService.cancel(shop.eventId, orderUuid, "취소") }
        assertTrue(cancelPeak <= 2, "최대 $cancelPeak — 락 트랜잭션 1 + 그 안의 철회 락 트랜잭션 1 까지만 (호출 측 트랜잭션이 락을 기다리며 쥐면 3)")
        assertNull(LockConnectionProbe.lastCallerTxOf("WithdrawOrderService.cancelOrder"))
        assertEquals(OrderStatus.CANCELED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
    }

    @Test
    fun `v1 쿠폰 발급 - 발급 락을 트랜잭션 밖에서 기다린다 (최대 1 이하, 예전 2)`() {
        val code = "L${UUID.randomUUID().toString().take(8)}"
        couponCampaignRepository.save(
            CouponCampaign(
                userId = 1L, discountType = DiscountType.AMOUNT, couponStockInfo = CouponStockInfo(issuedAmount = 5, remainingAmount = 5),
                discountAmount = 1000, couponCode = code, minimumCost = 10000, validTerm = 30,
                dateTimePeriod = DateTimePeriod(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1)),
            ),
        )
        val user = newBuyer()
        val issuePeak = peakOf { createUserCouponUseCase.execute(user.id!!, code) }
        assertTrue(issuePeak <= 1, "최대 $issuePeak — 발급 락 트랜잭션 1 개만 (유스케이스 트랜잭션이 락을 기다리며 쥐면 2)")
        assertNull(LockConnectionProbe.lastCallerTxOf("CreateIssuedCouponDomainService.createIssuedCoupon"))
        assertEquals(4L, couponCampaignRepository.findByCouponCode(code).get().couponStockInfo!!.remainingAmount)
    }
}
