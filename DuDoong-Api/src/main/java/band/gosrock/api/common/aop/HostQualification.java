package band.gosrock.api.common.aop;


import band.gosrock.domain.domains.host.domain.Host;
import java.util.function.BiConsumer;

/** 각 권한에 맞춰서 host 도메인의 검증메소드를 실행시킵니다. 검증 메소드이므로 biConsumer 를 통해 실행 시킬 함수를 미리 생성해 둡니다. -이찬진 */
public enum HostQualification {
    MASTER((userId, host) -> host.validateMasterHostUser(userId)),
    MANAGER((userId, host) -> host.validateManagerHostUser(userId)),
    GUEST((userId, host) -> host.validateActiveHostUser(userId));
    private final BiConsumer<Long, Host> consumer;

    HostQualification(BiConsumer<Long, Host> consumer) {
        this.consumer = consumer;
    }

    /**
     * 호스트의 검증을 수행하는 메서드
     *
     * @param userId
     * @param host
     */
    public void validQualification(Long userId, Host host) {
        consumer.accept(userId, host);
    }
}
