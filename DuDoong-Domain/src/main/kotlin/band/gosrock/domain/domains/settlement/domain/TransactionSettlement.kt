package band.gosrock.domain.domains.settlement.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.TossPaymentMethod
import java.time.LocalDate
import java.time.LocalDateTime
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

/** 거래 건 별 정산용 ( 로그 성 ) */
@Entity(name = "tbl_transaction_settlement")
open class TransactionSettlement protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_settlement_id")
    var id: Long? = null
        protected set

    var eventId: Long? = null
        protected set

    // 주문 아이디
    var orderUuid: String? = null
        protected set

    // 결제 키값 ( 주문 요청시 토스페이먼츠 자체 키값 )
    var paymentKey: String? = null
        protected set

    // 거래 키값 ( 트랜잭션 키 )
    var transactionKey: String? = null
        protected set

    // 결제 방식
    @Enumerated(EnumType.STRING)
    var paymentMethod: TossPaymentMethod? = null
        protected set

    // 결제한 금액
    @AttributeOverride(name = "amount", column = Column(name = "payment_amount"))
    @Embedded
    var paymentAmount: Money? = null
        protected set

    // 수수료 상세정보
    @ElementCollection
    @CollectionTable(name = "tbl_transaction_settlement_fee_detail")
    var fees: MutableList<SettlementFeeVo> = mutableListOf()
        protected set

    // 수수료 공급가액 ( 수수료 총액 )
    @AttributeOverride(name = "amount", column = Column(name = "fee_supply_amount"))
    @Embedded
    var feeSupplyAmount: Money? = null
        protected set

    // 수수료 부가세
    @AttributeOverride(name = "amount", column = Column(name = "fee_vat"))
    @Embedded
    var feeVat: Money? = null
        protected set

    // 할부 수수료 금액
    @AttributeOverride(name = "amount", column = Column(name = "interest_fee"))
    @Embedded
    var interestFee: Money? = null
        protected set

    // 지급 금액 ( 정산 받는 금액 )
    @AttributeOverride(name = "amount", column = Column(name = "settlement_amount"))
    @Embedded
    var settlementAmount: Money? = null
        protected set

    // 정산 지급일 ( 예정일도 가능 )
    var soldDate: LocalDate? = null
        protected set

    // 정산 매출일
    var paidOutDate: LocalDate? = null
        protected set

    // 거래 승인 시점
    var approvedAt: LocalDateTime? = null
        protected set

    constructor(
        eventId: Long?,
        orderUuid: String?,
        paymentKey: String?,
        transactionKey: String?,
        paymentMethod: TossPaymentMethod?,
        paymentAmount: Money?,
        fees: List<SettlementFeeVo>,
        feeSupplyAmount: Money?,
        feeVat: Money?,
        interestFee: Money?,
        settlementAmount: Money?,
        soldDate: LocalDate?,
        paidOutDate: LocalDate?,
        approvedAt: LocalDateTime?,
    ) : this() {
        this.eventId = eventId
        this.orderUuid = orderUuid
        this.paymentKey = paymentKey
        this.transactionKey = transactionKey
        this.paymentMethod = paymentMethod
        this.paymentAmount = paymentAmount
        this.fees = fees.toMutableList()
        this.feeSupplyAmount = feeSupplyAmount
        this.feeVat = feeVat
        this.interestFee = interestFee
        this.settlementAmount = settlementAmount
        this.soldDate = soldDate
        this.paidOutDate = paidOutDate
        this.approvedAt = approvedAt
    }

    companion object {
        @JvmStatic
        fun of(eventId: Long, settlementResponse: SettlementResponse): TransactionSettlement {
            val settlementFeeVos = (settlementResponse.fees
                ?: throw IllegalArgumentException("Missing fees in settlement response"))
                .map { SettlementFeeVo.from(it) }
            return TransactionSettlement(
                eventId = eventId,
                orderUuid = settlementResponse.orderId,
                paymentKey = settlementResponse.paymentKey,
                transactionKey = settlementResponse.transactionKey,
                paymentMethod = settlementResponse.method,
                paymentAmount = Money.wons(settlementResponse.amount
                    ?: throw IllegalArgumentException("Missing amount in settlement response")),
                fees = settlementFeeVos,
                feeSupplyAmount = Money.wons(settlementResponse.supplyAmount
                    ?: throw IllegalArgumentException("Missing supplyAmount in settlement response")),
                feeVat = Money.wons(settlementResponse.vat
                    ?: throw IllegalArgumentException("Missing vat in settlement response")),
                interestFee = Money.wons(settlementResponse.interestFee
                    ?: throw IllegalArgumentException("Missing interestFee in settlement response")),
                settlementAmount = Money.wons(settlementResponse.payOutAmount
                    ?: throw IllegalArgumentException("Missing payOutAmount in settlement response")),
                soldDate = settlementResponse.soldDate,
                paidOutDate = settlementResponse.paidOutDate,
                approvedAt = (settlementResponse.approvedAt
                    ?: throw IllegalArgumentException("Missing approvedAt in settlement response"))
                    .toLocalDateTime(),
            )
        }
    }
}
