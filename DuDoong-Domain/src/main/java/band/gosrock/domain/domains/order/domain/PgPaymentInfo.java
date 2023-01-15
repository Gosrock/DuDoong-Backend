package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@Getter
public class PgPaymentInfo {
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.DEFAULT;

    // 결제 공급자 정보 ex 카카오페이 ( 토스 승인 이후 저장 )
    private String paymentProvider;

    // 영수증 주소 ( 토스 승인 이후 저장 )
    private String receiptUrl;
    // 승인된 거래키 ( 취소 때 사용 )
    private String paymentKey;
    // 세금 ( 토스 승인 이후 저장 )
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "vat_amount"))
    private Money vat;

    @Builder
    public PgPaymentInfo(
            PaymentMethod paymentMethod,
            String paymentProvider,
            String receiptUrl,
            String paymentKey,
            Money vat) {
        this.paymentMethod = paymentMethod;
        this.paymentProvider = paymentProvider;
        this.receiptUrl = receiptUrl;
        this.paymentKey = paymentKey;
        this.vat = vat;
    }

    // PaymentsResponse infra layer
    public static PgPaymentInfo from(PaymentsResponse paymentsResponse) {
        return PgPaymentInfo.builder()
                .paymentKey(paymentsResponse.getPaymentKey())
                .paymentMethod(PaymentMethod.from(paymentsResponse.getMethod()))
                .paymentProvider(paymentsResponse.getProviderName())
                .receiptUrl(paymentsResponse.getReceipt().getUrl())
                .vat(Money.wons(paymentsResponse.getVat()))
                .build();
    }
}
