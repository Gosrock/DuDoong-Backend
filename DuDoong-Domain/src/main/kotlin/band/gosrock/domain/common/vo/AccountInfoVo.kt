package band.gosrock.domain.common.vo

import jakarta.persistence.Embeddable

@Embeddable
class AccountInfoVo(
    var bankName: String? = null,
    var accountNumber: String? = null,
    var accountHolder: String? = null,
) {
    companion object {
        @JvmStatic
        fun valueOf(bankName: String?, accountNumber: String?, accountHolder: String?): AccountInfoVo =
            AccountInfoVo(bankName, accountNumber, accountHolder)
    }
}
