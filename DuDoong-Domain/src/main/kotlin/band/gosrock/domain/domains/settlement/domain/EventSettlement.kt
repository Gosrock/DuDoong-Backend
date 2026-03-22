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
open class EventSettlement(
    var eventId: Long? = null,

    // 총 매출 금액
    @AttributeOverride(name = "amount", column = Column(name = "total_sales_amount"))
    @Embedded
    var totalSalesAmount: Money? = null,

    // 두둥티켓 송금 관련
    @AttributeOverride(name = "amount", column = Column(name = "dudoong_amount"))
    @Embedded
    var dudoongAmount: Money? = null,

    // 카드 결제 금액
    @AttributeOverride(name = "amount", column = Column(name = "payment_amount"))
    @Embedded
    var paymentAmount: Money? = null,

    // 쿠폰 금액
    @AttributeOverride(name = "amount", column = Column(name = "coupon_amount"))
    @Embedded
    var couponAmount: Money? = null,

    // 중개 수수료 ( 카드 결제 금액의 % )
    @AttributeOverride(name = "amount", column = Column(name = "dudoong_fee"))
    @Embedded
    var dudoongFee: Money? = null,

    // 결제 대행 수수료
    @AttributeOverride(name = "amount", column = Column(name = "pg_fee"))
    @Embedded
    var pgFee: Money? = null,

    // 결제 대행 수수료 vat
    @AttributeOverride(name = "amount", column = Column(name = "pg_fee_vat"))
    @Embedded
    var pgFeeVat: Money? = null,

    // 최종 정산 금액
    @AttributeOverride(name = "amount", column = Column(name = "total_amount"))
    @Embedded
    var totalAmount: Money? = null,

    // S3 업로드된 키.
    var eventOrderExcelKey: String? = null,

    // 정산 진행 과정
    @Enumerated(EnumType.STRING)
    var eventSettlementStatus: EventSettlementStatus = EventSettlementStatus.READY,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_settlement_id")
    var id: Long? = null
        protected set

    fun updateEventOrderListExcelKey(key: String) {
        this.eventOrderExcelKey = key
    }

    companion object {
        @JvmStatic
        fun createWithEventId(eventId: Long): EventSettlement =
            EventSettlement(eventId = eventId)
    }
}
