package band.gosrock.api.common.aop.hostRole;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class HostCallTransactionFactory {

    private final HostRoleEventTransaction hostRoleEventTransaction;
    private final HostRoleHostTransaction hostRoleHostTransaction;

    public HostRoleCallTransaction getCallTransaction(FindHostFrom findHostFrom) {
        if (findHostFrom == FindHostFrom.HOST_ID) {
            return hostRoleHostTransaction;
        }
        return hostRoleEventTransaction;
    }
}
