package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.common.vo.Money
import javax.persistence.AttributeOverride
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded

@Embeddable
class PaymentInfo() {

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "payment_amount"))
    var paymentAmount: Money? = null
        protected set

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "supply_amount"))
    var supplyAmount: Money? = null
        protected set

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "discount_amount"))
    var discountAmount: Money? = null
        protected set

    companion object {
        @JvmStatic
        fun of(paymentAmount: Money, supplyAmount: Money, discountAmount: Money): PaymentInfo =
            PaymentInfo().apply {
                this.paymentAmount = paymentAmount
                this.supplyAmount = supplyAmount
                this.discountAmount = discountAmount
            }
    }
}
