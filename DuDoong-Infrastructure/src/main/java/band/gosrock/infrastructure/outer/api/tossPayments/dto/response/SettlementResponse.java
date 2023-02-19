package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import java.time.LocalDate;
import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SettlementResponse {
    // 결제 건에 대한 고유 한 키값
    private String paymentKey;
    // 거래의 키 값입니다. 한 결제 건의 승인 거래와 취소 거래를 구분하는데 사용됩니다. 최대 길이는 64자입니다.
    private String transactionKey;
    // 결제 타입 정 NORMAL(일반 결제), BILLING(자동 결제), BRANDPAY(브랜드페이)
    // 주문 아이디 ( 상점에서 발급 해야함 ) 6-64자 사이의 길이
    private String orderId;
    // 주문 이름 ex 생수 외 1건  최대 길이 100자
    private String orderName;
    // 상점 구분 아이디
    private String mId;

    // 통화 단위 원화 'KRW' 기본값
    private String currency;
    // 결제할 때 사용한 결제수단 카드, 가상계좌, 간편결제, 휴대폰, 계좌이체, 상품권(문화상품권, 도서문화상품권, 게임문화상품권)
    private TossPaymentMethod method;

    // 총 결제 금액
    private Long amount;
    // 결제 수수료의 공급가액
    private Long supplyAmount;
    // 결제 수수료의 부가세
    private Long vat;
    // 지급 금액 결제금액 amount - fee 를 제외한 금액임
    private Long payOutAmount;
    // 할부 수수료 금액
    private Long interestFee;

    // 결제 승인이 일어난 시간정보
    private ZonedDateTime approvedAt;

    private LocalDate soldDate;
    private LocalDate paidOutDate;
}
