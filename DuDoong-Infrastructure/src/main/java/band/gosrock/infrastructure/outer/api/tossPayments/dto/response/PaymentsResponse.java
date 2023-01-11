package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import java.time.ZonedDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentsResponse {
    // api 버젼 정보
    private String version;
    // 결제 건에 대한 고유 한 키값
    private String paymentKey;
    // 결제 타입 정 NORMAL(일반 결제), BILLING(자동 결제), BRANDPAY(브랜드페이)
    // NORMAL 만 지원
    private String type;
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
    private Long totalAmount;
    // 취소할 수 있는 금액(잔고)
    private Long balanceAmount;

    private PaymentStatus status;

    // 결제 일어난 시간정보
    private ZonedDateTime requestedAt;
    // 결제 승인이 일어난 시간정보
    private ZonedDateTime approvedAt;
    // 에스크로 사용여부
    private Boolean useEscrow;
    // 마지막 거래건에대한 키값 승인 후 취소 두번 이면 마지막 취소 키
    private String lastTransactionKey;
    // 공금가액
    private Long suppliedAmount;
    // 부가세
    private Long vat;

    private Boolean cultureExpense;
    private Long taxFreeAmount;
    private Long taxExemptionAmount;
    // 취소 이력
    private List<PaymentsCancels> cancels;
    // 부분 취소 가능 여부
    private Boolean isPartialCancelable;

    /* 가상계좌 , 휴대폰 결제, 상품권 결제 , 계좌 이체 지원안함. */

    // 거래 영수증 정보 ( 영수증 주소 )
    private PaymentReceipt receipt;
    // 결제창 정보 ( 결제창 열리는 주소 )
    private PaymentCheckout checkout;

    // 간편 결제
    private PaymentEasyPay easyPay;
    // 카드결제
    private PaymentsCard card;

    private String country;
    // 결제 실패시
    private PaymentsFailure failure;

    // 현금 영수증 발급 관련
    private PaymentsCashReceipt cashReceipt;
    // 카드 프로모션 적용시. 적용안할듯
    private PaymentsCardPromotion discount;

    public String getProviderName(){
        if(TossPaymentMethod.CARD.equals(this.method)){
            this.card.getIssuerCode().get
        }else if(TossPaymentMethod.EASYPAY.equals(this.method)){

        }

    }
}
