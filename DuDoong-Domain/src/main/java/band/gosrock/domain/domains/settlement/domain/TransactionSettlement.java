package band.gosrock.domain.domains.settlement.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_transaction_settlement")
public class TransactionSettlement extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    private Long eventId;
    // 주문 아이디
    //

    // 총 매출 금액
    // 두둥티켓 송금 관련
    // 카드 결제 금액
    // 쿠폰 금액

    // 수수료 관련

    // 중개 수수료 ( 카드 결제 금액의 % )
    // 결제 대행 수수료

    // 최종 정산 금액
    private Money totalAmount;

    // S3 업로드된 키.
    private String eventOrderExcelKey;
}
