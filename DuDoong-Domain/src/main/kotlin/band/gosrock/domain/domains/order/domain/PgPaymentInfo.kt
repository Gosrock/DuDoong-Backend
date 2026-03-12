package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.common.vo.Money
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse
import javax.persistence.AttributeOverride
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
class PgPaymentInfo() {

    @Enumerated(EnumType.STRING)
    var paymentMethod: PaymentMethod = PaymentMethod.DEFAULT
        protected set

    var paymentProvider: String = ""
        protected set

    var receiptUrl: String = ""
        protected set

    var paymentKey: String = ""
        protected set

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "vat_amount"))
    var vat: Money = Money.ZERO
        protected set

    companion object {
        @JvmStatic
        fun from(paymentsResponse: PaymentsResponse): PgPaymentInfo = PgPaymentInfo().apply {
            paymentKey = paymentsResponse.paymentKey!!
            paymentMethod = PaymentMethod.from(paymentsResponse.method!!)
            paymentProvider = paymentsResponse.getProviderName()
            receiptUrl = paymentsResponse.receipt!!.url!!
            vat = Money.wons(paymentsResponse.vat!!)
        }

        @JvmStatic
        fun empty(): PgPaymentInfo = PgPaymentInfo()
    }
}
