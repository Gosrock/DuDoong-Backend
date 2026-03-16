package band.gosrock.domain.domains.settlement.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

/** 이벤트 별 정산용 ( 클라이언트 용 ) */
@Entity(name = "tbl_event_settlement")
open class EventSettlement protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_settlement_id")
    var id: Long? = null
        protected set

    var eventId: Long? = null
        protected set

    // 총 매출 금액
    @AttributeOverride(name = "amount", column = Column(name = "total_sales_amount"))
    @Embedded
    var totalSalesAmount: Money? = null
        protected set

    // 두둥티켓 송금 관련
    @AttributeOverride(name = "amount", column = Column(name = "dudoong_amount"))
    @Embedded
    var dudoongAmount: Money? = null
        protected set

    // 카드 결제 금액
    @AttributeOverride(name = "amount", column = Column(name = "payment_amount"))
    @Embedded
    var paymentAmount: Money? = null
        protected set

    // 쿠폰 금액
    @AttributeOverride(name = "amount", column = Column(name = "coupon_amount"))
    @Embedded
    var couponAmount: Money? = null
        protected set

    // 중개 수수료 ( 카드 결제 금액의 % )
    @AttributeOverride(name = "amount", column = Column(name = "dudoong_fee"))
    @Embedded
    var dudoongFee: Money? = null
        protected set

    // 결제 대행 수수료
    @AttributeOverride(name = "amount", column = Column(name = "pg_fee"))
    @Embedded
    var pgFee: Money? = null
        protected set

    // 결제 대행 수수료 vat
    @AttributeOverride(name = "amount", column = Column(name = "pg_fee_vat"))
    @Embedded
    var pgFeeVat: Money? = null
        protected set

    // 최종 정산 금액
    @AttributeOverride(name = "amount", column = Column(name = "total_amount"))
    @Embedded
    var totalAmount: Money? = null
        protected set

    // S3 업로드된 키.
    var eventOrderExcelKey: String? = null
        protected set

    // 정산 진행 과정
    @Enumerated(EnumType.STRING)
    var eventSettlementStatus: EventSettlementStatus = EventSettlementStatus.READY
        protected set

    constructor(
        eventId: Long?,
        totalSalesAmount: Money?,
        dudoongAmount: Money?,
        paymentAmount: Money?,
        couponAmount: Money?,
        dudoongFee: Money?,
        pgFee: Money?,
        pgFeeVat: Money?,
        totalAmount: Money?,
        eventOrderExcelKey: String?,
        eventSettlementStatus: EventSettlementStatus?,
    ) : this() {
        this.eventId = eventId
        this.totalSalesAmount = totalSalesAmount
        this.dudoongAmount = dudoongAmount
        this.paymentAmount = paymentAmount
        this.couponAmount = couponAmount
        this.dudoongFee = dudoongFee
        this.pgFee = pgFee
        this.pgFeeVat = pgFeeVat
        this.totalAmount = totalAmount
        this.eventOrderExcelKey = eventOrderExcelKey
        if (eventSettlementStatus != null) this.eventSettlementStatus = eventSettlementStatus
    }

    fun updateEventOrderListExcelKey(key: String) {
        this.eventOrderExcelKey = key
    }

    companion object {
        @JvmStatic
        fun createWithEventId(eventId: Long): EventSettlement =
            EventSettlement(
                eventId = eventId,
                totalSalesAmount = null,
                dudoongAmount = null,
                paymentAmount = null,
                couponAmount = null,
                dudoongFee = null,
                pgFee = null,
                pgFeeVat = null,
                totalAmount = null,
                eventOrderExcelKey = null,
                eventSettlementStatus = null,
            )

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var eventId: Long? = null
        private var totalSalesAmount: Money? = null
        private var dudoongAmount: Money? = null
        private var paymentAmount: Money? = null
        private var couponAmount: Money? = null
        private var dudoongFee: Money? = null
        private var pgFee: Money? = null
        private var pgFeeVat: Money? = null
        private var totalAmount: Money? = null
        private var eventOrderExcelKey: String? = null
        private var eventSettlementStatus: EventSettlementStatus? = null

        fun eventId(eventId: Long?) = apply { this.eventId = eventId }
        fun totalSalesAmount(v: Money?) = apply { this.totalSalesAmount = v }
        fun dudoongAmount(v: Money?) = apply { this.dudoongAmount = v }
        fun paymentAmount(v: Money?) = apply { this.paymentAmount = v }
        fun couponAmount(v: Money?) = apply { this.couponAmount = v }
        fun dudoongFee(v: Money?) = apply { this.dudoongFee = v }
        fun pgFee(v: Money?) = apply { this.pgFee = v }
        fun pgFeeVat(v: Money?) = apply { this.pgFeeVat = v }
        fun totalAmount(v: Money?) = apply { this.totalAmount = v }
        fun eventOrderExcelKey(v: String?) = apply { this.eventOrderExcelKey = v }
        fun eventSettlementStatus(v: EventSettlementStatus?) = apply { this.eventSettlementStatus = v }

        fun build(): EventSettlement =
            EventSettlement(
                eventId = eventId,
                totalSalesAmount = totalSalesAmount,
                dudoongAmount = dudoongAmount,
                paymentAmount = paymentAmount,
                couponAmount = couponAmount,
                dudoongFee = dudoongFee,
                pgFee = pgFee,
                pgFeeVat = pgFeeVat,
                totalAmount = totalAmount,
                eventOrderExcelKey = eventOrderExcelKey,
                eventSettlementStatus = eventSettlementStatus,
            )
    }
}
