package band.gosrock.api.common.aop.hostPartner;


import band.gosrock.api.common.aop.hostRole.FindHostFrom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class HostPartnerCallTransactionFactory {

    private final HostPartnerEventTransaction hostRoleEventTransaction;
    private final HostPartnerHostTransaction hostRoleHostTransaction;

    public HostPartnerCallTransaction getCallTransaction(FindHostFrom findHostFrom) {
        if (findHostFrom == FindHostFrom.HOST_ID) {
            return hostRoleHostTransaction;
        }
        return hostRoleEventTransaction;
    }
}
