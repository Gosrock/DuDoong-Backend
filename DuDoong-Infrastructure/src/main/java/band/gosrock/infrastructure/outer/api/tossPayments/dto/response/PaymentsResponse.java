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
    private String method;
    // 총 결제 금액
    private Long totalAmount;
    // 취소할 수 있는 금액(잔고)
    private Long balanceAmount;
    // 결제 처리 상태
    // - READY: 결제를 생성하면 가지게 되는 초기 상태 입니다. 인증 전까지는 READY 상태를 유지합니다.
    //
    // - IN_PROGRESS: 결제 수단 정보와 해당 결제 수단의 소유자가 맞는지 인증을 마친 상태입니다. 결제 승인 API를 호출하면 결제가 완료됩니다.
    //
    // - WAITING_FOR_DEPOSIT: 가상계좌 결제 흐름에만 있는 상태로, 결제 고객이 발급된 가상계좌에 입금하는 것을 기다리고 있는 상태입니다.
    //
    // - DONE: 인증된 결제 수단 정보, 고객 정보로 요청한 결제가 승인된 상태입니다.
    //
    // - CANCELED: 승인된 결제가 취소된 상태입니다.
    //
    // - PARTIAL_CANCELED: 승인된 결제가 부분 취소된 상태입니다.
    //
    // - ABORTED: 결제 승인이 실패했거나, 결제 고객이 창을 닫아서 결제를 취소한 상태입니다.
    //
    // - EXPIRED: 결제 유효 시간 30분이 지나 거래가 취소된 상태입니다. IN_PROGRESS 상태에서 결제 승인 API를 호출하지 않으면 EXPIRED가 됩니다
    private String status;

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
}
