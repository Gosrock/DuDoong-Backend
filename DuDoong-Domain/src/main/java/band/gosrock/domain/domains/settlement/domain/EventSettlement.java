package band.gosrock.domain.domains.settlement.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import javax.persistence.Column;
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

/** 이벤트 별 정산용 ( 클라이언트 용 ) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_event_settlement")
public class EventSettlement extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_settlement_id")
    private Long id;

    private Long eventId;

    // 총 매출 금액
    private Money totalSalesAmount;
    // 두둥티켓 송금 관련
    private Money dudoongAmount;
    // 카드 결제 금액
    private Money paymentAmount;

    // 쿠폰 금액
    private Money couponAmount;

    // 수수료 관련
    // 중개 수수료 ( 카드 결제 금액의 % )
    private Money dudoongFee;

    // 결제 대행 수수료
    private Money pgFee;

    // 최종 정산 금액
    private Money totalAmount;

    // S3 업로드된 키.
    private String eventOrderExcelKey;

    @Enumerated(EnumType.STRING)
    private EventSettlementStatus eventSettlementStatus;

    @Builder
    public EventSettlement(
            Long eventId,
            Money totalSalesAmount,
            Money dudoongAmount,
            Money paymentAmount,
            Money couponAmount,
            Money dudoongFee,
            Money pgFee,
            Money totalAmount,
            String eventOrderExcelKey,
            EventSettlementStatus eventSettlementStatus) {
        this.eventId = eventId;
        this.totalSalesAmount = totalSalesAmount;
        this.dudoongAmount = dudoongAmount;
        this.paymentAmount = paymentAmount;
        this.couponAmount = couponAmount;
        this.dudoongFee = dudoongFee;
        this.pgFee = pgFee;
        this.totalAmount = totalAmount;
        this.eventOrderExcelKey = eventOrderExcelKey;
        this.eventSettlementStatus = eventSettlementStatus;
    }

    public static EventSettlement createWithExcelKey(Long eventId, String key) {
        return EventSettlement.builder().eventId(eventId).eventOrderExcelKey(key).build();
    }
}
