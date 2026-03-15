package band.gosrock.api.common.aop.hostPartner

import band.gosrock.api.common.aop.hostRole.FindHostFrom
import org.springframework.stereotype.Component

@Component
internal class HostPartnerCallTransactionFactory(
    private val hostRoleEventTransaction: HostPartnerEventTransaction,
    private val hostRoleHostTransaction: HostPartnerHostTransaction
) {
    fun getCallTransaction(findHostFrom: FindHostFrom): HostPartnerCallTransaction {
        return if (findHostFrom == FindHostFrom.HOST_ID) {
            hostRoleHostTransaction
        } else {
            hostRoleEventTransaction
        }
    }
}
