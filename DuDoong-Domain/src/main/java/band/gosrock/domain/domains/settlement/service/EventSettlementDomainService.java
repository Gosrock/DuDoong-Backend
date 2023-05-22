package band.gosrock.domain.domains.settlement.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.settlement.adaptor.EventSettlementAdaptor;
import band.gosrock.domain.domains.settlement.adaptor.TransactionSettlementAdaptor;
import band.gosrock.domain.domains.settlement.domain.EventSettlement;
import band.gosrock.domain.domains.settlement.domain.EventSettlementStatus;
import band.gosrock.domain.domains.settlement.domain.TransactionSettlement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@RequiredArgsConstructor
public class EventSettlementDomainService {

    private final EventSettlementAdaptor eventSettlementAdaptor;
    private final TransactionSettlementAdaptor transactionSettlementAdaptor;

    @Transactional
    public void generateEventSettlement(Long eventId, List<Order> orders) {
        List<TransactionSettlement> transactionSettlements =
                transactionSettlementAdaptor.findByEventId(eventId);
        Money totalPaymentAmount = getTotalPaymentAmount(transactionSettlements);
        Money pgFee = getPgFee(transactionSettlements, totalPaymentAmount);
        Money pgFeeVat = getPgFeeVat(pgFee);

        EventSettlement eventSettlement =
                EventSettlement.builder()
                        .eventId(eventId)
                        .totalSalesAmount(getTotalSalesAmount(orders))
                        .dudoongAmount(getDudoongTicketSalesAmount(orders))
                        .paymentAmount(totalPaymentAmount)
                        .couponAmount(getPaymentOrderDiscountAmount(orders))
                        .dudoongFee(Money.ZERO)
                        .pgFee(pgFee)
                        .pgFeeVat(pgFeeVat)
                        .totalAmount(getTotalSettlementAmount(totalPaymentAmount, pgFee, pgFeeVat))
                        .eventSettlementStatus(EventSettlementStatus.CALCULATED)
                        .build();

        // 최종 정산 금액 계산.
        eventSettlementAdaptor.save(eventSettlement);
    }
    /** 토스페이먼츠 수수료 */
    private Money getPgFee(
            List<TransactionSettlement> transactionSettlements, Money paymentAmount) {
        return paymentAmount.minus(getSettlementAmount(transactionSettlements));
    }

    /** 최종 정산금액 ( 판매대금 - 토스페이먼츠 수수료 - 토스페이먼츠 수수료 vat ) */
    private static Money getTotalSettlementAmount(
            Money paymentAmount, Money pgFee, Money pgFeeVat) {
        return paymentAmount.minus(pgFee).minus(pgFeeVat);
    }
    /** 토스페이먼츠 수수료 vat */
    private Money getPgFeeVat(Money pgFee) {
        return Money.wons(pgFee.times(0.1).longValue());
    }

    private Money getPaymentOrderDiscountAmount(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.CONFIRM)
                .map(Order::getTotalDiscountPrice)
                .reduce(Money.ZERO, Money::plus);
    }

    private Money getPaymentOrderTotalSales(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.CONFIRM)
                .map(Order::getTotalPaymentPrice)
                .reduce(Money.ZERO, Money::plus);
    }
    /** 주문목록 두둥티켓 판매 대금 */
    private Money getDudoongTicketSalesAmount(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.APPROVED)
                .map(Order::getTotalPaymentPrice)
                .reduce(Money.ZERO, Money::plus);
    }
    /** 주문 액 기준 총 판매 대금 ( 두둥티켓 , 결제티켓 ) */
    private Money getTotalSalesAmount(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getOrderStatus().isCanWithDraw())
                .map(Order::getTotalPaymentPrice)
                .reduce(Money.ZERO, Money::plus);
    }

    /** 토스페이먼츠에서 정산해주는 금액 */
    private Money getSettlementAmount(List<TransactionSettlement> transactionSettlements) {
        return transactionSettlements.stream()
                .map(TransactionSettlement::getSettlementAmount)
                .reduce(Money.ZERO, Money::plus);
    }
    /** 토스페이먼츠의 총 매출액 */
    private Money getTotalPaymentAmount(List<TransactionSettlement> transactionSettlements) {
        return transactionSettlements.stream()
                .map(TransactionSettlement::getPaymentAmount)
                .reduce(Money.ZERO, Money::plus);
    }
}
