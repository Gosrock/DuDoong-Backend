package band.gosrock.api.common.aop;


import band.gosrock.domain.domains.host.domain.Host;
import java.util.function.BiConsumer;

public enum HostQualification {
    MASTER((userId, host) -> host.validateMasterHostUser(userId)),
    MANAGER((userId, host) -> host.validateSuperHostUser(userId)),
    GUEST((userId, host) -> host.validateActiveHostUser(userId));
    private final BiConsumer<Long, Host> consumer;

    HostQualification(BiConsumer<Long, Host> consumer) {
        this.consumer = consumer;
    }

    public void validQualification(Long userId, Host host) {
        consumer.accept(userId, host);
    }
}
