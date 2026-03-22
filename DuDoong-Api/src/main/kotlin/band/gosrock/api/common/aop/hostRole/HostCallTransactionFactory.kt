package band.gosrock.api.common.aop.hostRole

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.stereotype.Component

@Component
internal class HostCallTransactionFactory(
    userAdaptor: UserAdaptor,
    hostAdaptor: HostAdaptor,
    eventAdaptor: EventAdaptor,
    hostRoleEventTransaction: HostRoleEventTransaction,
    hostRoleHostTransaction: HostRoleHostTransaction
) {
    private val hostRoleEventTransaction: HostRoleEventTransaction = hostRoleEventTransaction
    private val hostRoleEventWithoutTransaction: HostRoleEventTransaction =
        HostRoleEventTransaction(userAdaptor, eventAdaptor, hostAdaptor)
    private val hostRoleHostTransaction: HostRoleHostTransaction = hostRoleHostTransaction
    private val hostRoleHostWithoutTransaction: HostRoleHostTransaction =
        HostRoleHostTransaction(userAdaptor, hostAdaptor)

    fun getCallTransaction(findHostFrom: FindHostFrom, applyTransaction: Boolean): HostRoleCallTransaction {
        return if (findHostFrom == FindHostFrom.HOST_ID) {
            if (applyTransaction) hostRoleHostTransaction else hostRoleHostWithoutTransaction
        } else {
            if (applyTransaction) hostRoleEventTransaction else hostRoleEventWithoutTransaction
        }
    }
}
