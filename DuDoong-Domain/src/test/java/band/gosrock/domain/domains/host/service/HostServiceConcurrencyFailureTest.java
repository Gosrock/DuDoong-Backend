package band.gosrock.domain.domains.host.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.CunCurrencyExecutorService;
import band.gosrock.domain.DisableDomainEvent;
import band.gosrock.domain.DisableRedissonLock;
import band.gosrock.domain.DomainIntegrateSpringBootTest;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.repository.HostRepository;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

@DomainIntegrateSpringBootTest
@DisableDomainEvent
@DisableRedissonLock
@Slf4j
public class HostServiceConcurrencyFailureTest {
    @Autowired HostService hostService;

    @Autowired RedissonClient redissonClient;

    Host host;

    @Mock HostUser hostUser;
    @MockBean HostRepository hostRepository;

    @BeforeEach
    void setup() {
        host = Host.builder().build();
        ReflectionTestUtils.setField(host, "id", 1L);
        given(hostUser.getUserId()).willReturn(9L);
        given(hostRepository.save(any(Host.class))).willReturn(host);
    }

    @Test
    @DisplayName("락 적용을 안하면 여러개의 초대가 승인될 수도 있다.")
    public void 호스트유저_초대요청_동시성_실패() throws InterruptedException {
        // given
        // when
        AtomicLong successCount = new AtomicLong();
        CunCurrencyExecutorService.execute(
                () -> hostService.inviteHostUser(host, hostUser), successCount);
        // then
        log.info(String.valueOf(successCount.get()));
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1L);
    }
}
