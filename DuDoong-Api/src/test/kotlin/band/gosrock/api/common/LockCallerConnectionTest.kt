package band.gosrock.api.common

import band.gosrock.api.coupon.service.CreateUserCouponUseCase
import band.gosrock.api.supports.ApiIntegrateSpringBootTest
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

/**
 * 호출 측 트랜잭션이 v1 락을 기다리며 커넥션을 쥐지 않는지 (#746, #743 후속). 테스트 스레드에서 직접 부르므로 open-in-view 영향이 없다.
 * 측정값은 이 스레드의 최대 동시 커넥션 수 (`ThreadConnections`)
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
    fun `v2 승인·취소 - v1 주문 락을 트랜잭션 밖에서 기다린다 (최대 = 락 트랜잭션 + 그 안의 발급·철회 락 트랜잭션 = 2, 예전 3)`() {
        val shop = Shop()
        val orderUuid = shop.order(newBuyer())
        assertEquals(2, peakOf { v2OrderDomainService.approve(shop.eventId, orderUuid) })
        assertEquals(OrderStatus.APPROVED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
        assertEquals(2, peakOf { v2OrderDomainService.cancel(shop.eventId, orderUuid, "취소") })
        assertEquals(OrderStatus.CANCELED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
    }

    @Test
    fun `v1 쿠폰 발급 - 발급 락을 트랜잭션 밖에서 기다린다 (최대 1, 예전 2)`() {
        val code = "L${UUID.randomUUID().toString().take(8)}"
        couponCampaignRepository.save(
            CouponCampaign(
                userId = 1L, discountType = DiscountType.AMOUNT, couponStockInfo = CouponStockInfo(issuedAmount = 5, remainingAmount = 5),
                discountAmount = 1000, couponCode = code, minimumCost = 10000, validTerm = 30,
                dateTimePeriod = DateTimePeriod(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1)),
            ),
        )
        val user = newBuyer()
        assertEquals(1, peakOf { createUserCouponUseCase.execute(user.id!!, code) })
        assertEquals(4L, couponCampaignRepository.findByCouponCode(code).get().couponStockInfo!!.remainingAmount)
    }
}
