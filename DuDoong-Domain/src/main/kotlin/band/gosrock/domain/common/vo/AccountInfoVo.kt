package band.gosrock.domain.common.vo

import jakarta.persistence.Embeddable

@Embeddable
class AccountInfoVo() {
    var bankName: String? = null
        protected set
    var accountNumber: String? = null
        protected set
    var accountHolder: String? = null
        protected set

    constructor(bankName: String?, accountNumber: String?, accountHolder: String?) : this() {
        this.bankName = bankName
        this.accountNumber = accountNumber
        this.accountHolder = accountHolder
    }

    companion object {
        @JvmStatic
        fun valueOf(bankName: String?, accountNumber: String?, accountHolder: String?): AccountInfoVo =
            AccountInfoVo(bankName, accountNumber, accountHolder)
    }
}
