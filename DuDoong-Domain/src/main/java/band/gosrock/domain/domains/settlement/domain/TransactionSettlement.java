package band.gosrock.domain.domains.settlement.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.TossPaymentMethod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.AttributeOverride;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 거래 건 별 정산용 ( 로그 성 ) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_transaction_settlement")
public class TransactionSettlement extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_settlement_id")
    private Long id;

    private Long eventId;
    // 주문 아이디
    private String orderUuid;
    // 결제 키값 ( 주문 요청시 토스페이먼츠 자체 키값 )
    private String paymentKey;
    // 거래 키값 ( 트랜잭션 키 )
    private String transactionKey;
    // 결제 방식
    @Enumerated(EnumType.STRING)
    private TossPaymentMethod paymentMethod;
    // 결제한 금액

    @AttributeOverride(name = "amount", column = @Column(name = "payment_amount"))
    @Embedded
    private Money paymentAmount;
    // 수수료 상세정보
    @ElementCollection
    @CollectionTable(name = "tbl_transaction_settlement_fee_detail")
    private List<SettlementFeeVo> fees = new ArrayList<>();
    // 수수료 공급가액 ( 수수료 총액 )

    @AttributeOverride(name = "amount", column = @Column(name = "fee_supply_amount"))
    @Embedded
    private Money feeSupplyAmount;
    // 수수료 부가세
    @AttributeOverride(name = "amount", column = @Column(name = "fee_vat"))
    @Embedded
    private Money feeVat;

    // 할부 수수료 금액
    @AttributeOverride(name = "amount", column = @Column(name = "interest_fee"))
    @Embedded
    private Money interestFee;
    // 지급 금액 ( 정산 받는 금액 )

    @AttributeOverride(name = "amount", column = @Column(name = "settlement_amount"))
    @Embedded
    private Money settlementAmount;

    // 정산 지급일 ( 예정일도 가능 )
    private LocalDate soldDate;
    // 정산 매출일
    private LocalDate paidOutDate;

    // 거래 승인 시점
    private LocalDateTime approvedAt;

    @Builder
    public TransactionSettlement(
            Long eventId,
            String orderUuid,
            String paymentKey,
            String transactionKey,
            TossPaymentMethod paymentMethod,
            Money paymentAmount,
            List<SettlementFeeVo> fees,
            Money feeSupplyAmount,
            Money feeVat,
            Money interestFee,
            Money settlementAmount,
            LocalDate soldDate,
            LocalDate paidOutDate,
            LocalDateTime approvedAt) {
        this.eventId = eventId;
        this.orderUuid = orderUuid;
        this.paymentKey = paymentKey;
        this.transactionKey = transactionKey;
        this.paymentMethod = paymentMethod;
        this.paymentAmount = paymentAmount;
        this.fees = fees;
        this.feeSupplyAmount = feeSupplyAmount;
        this.feeVat = feeVat;
        this.interestFee = interestFee;
        this.settlementAmount = settlementAmount;
        this.soldDate = soldDate;
        this.paidOutDate = paidOutDate;
        this.approvedAt = approvedAt;
    }

    public static TransactionSettlement of(Long eventId, SettlementResponse settlementResponse) {
        List<SettlementFeeVo> settlementFeeVos =
                settlementResponse.getFees().stream().map(SettlementFeeVo::from).toList();
        return TransactionSettlement.builder()
                .eventId(eventId)
                .orderUuid(settlementResponse.getOrderId())
                .paymentKey(settlementResponse.getPaymentKey())
                .transactionKey(settlementResponse.getTransactionKey())
                .paymentMethod(settlementResponse.getMethod())
                .paymentAmount(Money.wons(settlementResponse.getAmount()))
                .fees(settlementFeeVos)
                .feeSupplyAmount(Money.wons(settlementResponse.getSupplyAmount()))
                .feeVat(Money.wons(settlementResponse.getVat()))
                .interestFee(Money.wons((settlementResponse.getInterestFee())))
                .settlementAmount(Money.wons(settlementResponse.getPayOutAmount()))
                .soldDate(settlementResponse.getSoldDate())
                .paidOutDate(settlementResponse.getPaidOutDate())
                .approvedAt(settlementResponse.getApprovedAt().toLocalDateTime())
                .build();
    }
}
