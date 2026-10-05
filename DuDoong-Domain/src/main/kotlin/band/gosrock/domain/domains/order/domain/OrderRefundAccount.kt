package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 사용자 취소·환불 요청 때 입력한 환불 받을 계좌 (v2 전용, #718, V007). 주문 1:1 (order_id unique).
 * 계좌번호는 마스킹 없이 저장하고, 노출 범위는 응답에서 줄인다 (호스트 M+ 전체, 주문자 본인은 뒤 4자리, DEC-022).
 * 저장·조회는 `service.v2` 에서만 한다
 */
@Table(
    name = "tbl_order_refund_account",
    indexes = [Index(name = "uk_order_refund_account_order_id", columnList = "order_id", unique = true)],
)
@Entity
class OrderRefundAccount(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    bankName: String,
    accountHolder: String,
    accountNumber: String,
) : BaseTimeEntity() {

    @Column(name = "bank_name", nullable = false, length = BANK_NAME_MAX_LENGTH)
    var bankName: String = bankName
        protected set

    @Column(name = "account_holder", nullable = false, length = ACCOUNT_HOLDER_MAX_LENGTH)
    var accountHolder: String = accountHolder
        protected set

    @Column(name = "account_number", nullable = false, length = ACCOUNT_NUMBER_MAX_LENGTH)
    var accountNumber: String = accountNumber
        protected set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_refund_account_id")
    var id: Long? = null
        protected set

    /** 환불 계좌 수정 (#728). 환불 완료 전·본인 주문 확인은 `service.v2.V2UserOrderDomainService` 가 주문 잠금 안에서 한다 */
    internal fun change(bankName: String, accountHolder: String, accountNumber: String) {
        this.bankName = bankName
        this.accountHolder = accountHolder
        this.accountNumber = accountNumber
    }

    /** 뒤 4자리만 남기고 `*` (숫자·하이픈 외 문자도 그대로 센다). 4자리 이하면 전부 가린다 */
    fun maskedAccountNumber(): String =
        if (accountNumber.length <= VISIBLE_SUFFIX) "*".repeat(accountNumber.length)
        else "*".repeat(accountNumber.length - VISIBLE_SUFFIX) + accountNumber.takeLast(VISIBLE_SUFFIX)

    companion object {
        const val BANK_NAME_MAX_LENGTH = 20
        const val ACCOUNT_HOLDER_MAX_LENGTH = 20
        const val ACCOUNT_NUMBER_MAX_LENGTH = 30
        private const val VISIBLE_SUFFIX = 4
    }
}
