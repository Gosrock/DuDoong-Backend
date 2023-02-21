package band.gosrock.api.common.aop.hostRole;


import band.gosrock.api.common.UserUtils;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import org.springframework.stereotype.Component;

@Component
class HostCallTransactionFactory {

    private final HostRoleEventTransaction hostRoleEventTransaction;
    private final HostRoleEventTransaction hostRoleEventWithoutTransaction;

    public HostCallTransactionFactory(
            UserUtils userUtils,
            HostAdaptor hostAdaptor,
            EventAdaptor eventAdaptor,
            HostRoleEventTransaction hostRoleEventTransaction,
            HostRoleHostTransaction hostRoleHostTransaction) {
        this.hostRoleEventTransaction = hostRoleEventTransaction;
        this.hostRoleEventWithoutTransaction =
                new HostRoleEventTransaction(userUtils, eventAdaptor, hostAdaptor);
        this.hostRoleHostTransaction = hostRoleHostTransaction;
        this.hostRoleHostWithoutTransaction = new HostRoleHostTransaction(userUtils, hostAdaptor);
    }

    private final HostRoleHostTransaction hostRoleHostTransaction;
    private final HostRoleHostTransaction hostRoleHostWithoutTransaction;

    public HostRoleCallTransaction getCallTransaction(
            FindHostFrom findHostFrom, Boolean applyTransaction) {
        if (findHostFrom == FindHostFrom.HOST_ID) {
            return applyTransaction ? hostRoleHostTransaction : hostRoleHostWithoutTransaction;
        }
        return applyTransaction ? hostRoleEventTransaction : hostRoleEventWithoutTransaction;
    }
}
