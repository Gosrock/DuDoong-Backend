package band.gosrock.domain.domains.settlement.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.settlement.adaptor.EventSettlementAdaptor
import band.gosrock.domain.domains.settlement.adaptor.TransactionSettlementAdaptor
import band.gosrock.domain.domains.settlement.domain.EventSettlement
import band.gosrock.domain.domains.settlement.domain.EventSettlementStatus
import band.gosrock.domain.domains.settlement.domain.TransactionSettlement
import org.springframework.transaction.annotation.Transactional

@DomainService
class EventSettlementDomainService(
    private val eventSettlementAdaptor: EventSettlementAdaptor,
    private val transactionSettlementAdaptor: TransactionSettlementAdaptor,
) {
    @Transactional
    fun generateEventSettlement(eventId: Long, orders: List<Order>) {
        val transactionSettlements = transactionSettlementAdaptor.findByEventId(eventId)
        val totalPaymentAmount = getTotalPaymentAmount(transactionSettlements)
        val pgFee = getPgFee(transactionSettlements, totalPaymentAmount)
        val pgFeeVat = getPgFeeVat(pgFee)

        val eventSettlement = EventSettlement(
            eventId = eventId,
            totalSalesAmount = getTotalSalesAmount(orders),
            dudoongAmount = getDudoongTicketSalesAmount(orders),
            paymentAmount = totalPaymentAmount,
            couponAmount = getPaymentOrderDiscountAmount(orders),
            dudoongFee = Money.ZERO,
            pgFee = pgFee,
            pgFeeVat = pgFeeVat,
            totalAmount = getTotalSettlementAmount(totalPaymentAmount, pgFee, pgFeeVat),
            eventSettlementStatus = EventSettlementStatus.CALCULATED,
        )

        // 최종 정산 금액 계산.
        eventSettlementAdaptor.save(eventSettlement)
    }

    /** 토스페이먼츠 수수료 */
    private fun getPgFee(
        transactionSettlements: List<TransactionSettlement>,
        paymentAmount: Money,
    ): Money = paymentAmount.minus(getSettlementAmount(transactionSettlements))

    /** 최종 정산금액 ( 판매대금 - 토스페이먼츠 수수료 - 토스페이먼츠 수수료 vat ) */
    private fun getTotalSettlementAmount(
        paymentAmount: Money,
        pgFee: Money,
        pgFeeVat: Money,
    ): Money = paymentAmount.minus(pgFee).minus(pgFeeVat)

    /** 토스페이먼츠 수수료 vat */
    private fun getPgFeeVat(pgFee: Money): Money =
        Money.wons(pgFee.times(0.1).longValue())

    private fun getPaymentOrderDiscountAmount(orders: List<Order>): Money =
        orders
            .filter { it.orderStatus == OrderStatus.CONFIRM }
            .map { it.getTotalDiscountPrice() }
            .fold(Money.ZERO, Money::plus)

    private fun getPaymentOrderTotalSales(orders: List<Order>): Money =
        orders
            .filter { it.orderStatus == OrderStatus.CONFIRM }
            .map { it.getTotalPaymentPrice() }
            .fold(Money.ZERO, Money::plus)

    /** 주문목록 두둥티켓 판매 대금 */
    private fun getDudoongTicketSalesAmount(orders: List<Order>): Money =
        orders
            .filter { it.orderStatus == OrderStatus.APPROVED }
            .map { it.getTotalPaymentPrice() }
            .fold(Money.ZERO, Money::plus)

    /** 주문 액 기준 총 판매 대금 ( 두둥티켓 , 결제티켓 ) */
    private fun getTotalSalesAmount(orders: List<Order>): Money =
        orders
            .filter { it.orderStatus.isCanWithDraw() }
            .map { it.getTotalPaymentPrice() }
            .fold(Money.ZERO, Money::plus)

    /** 토스페이먼츠에서 정산해주는 금액 */
    private fun getSettlementAmount(transactionSettlements: List<TransactionSettlement>): Money =
        transactionSettlements
            .mapNotNull { it.settlementAmount }
            .fold(Money.ZERO, Money::plus)

    /** 토스페이먼츠의 총 매출액 */
    private fun getTotalPaymentAmount(transactionSettlements: List<TransactionSettlement>): Money =
        transactionSettlements
            .mapNotNull { it.paymentAmount }
            .fold(Money.ZERO, Money::plus)
}
