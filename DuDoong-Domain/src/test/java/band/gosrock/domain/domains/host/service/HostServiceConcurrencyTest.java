package band.gosrock.domain.domains.host.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.CunCurrencyExecutorService;
import band.gosrock.domain.DisableDomainEvent;
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
@Slf4j
public class HostServiceConcurrencyTest {
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
    @DisplayName("동시에 초대 요청을 보내도 하나의 요청만 성공해야한다.")
    public void 호스트유저_초대요청_동시성테스트() throws InterruptedException {
        // given
        // when
        AtomicLong successCount = new AtomicLong();
        CunCurrencyExecutorService.execute(
                () -> hostService.inviteHostUser(host, hostUser), successCount);
        // then
        assertThat(successCount.get()).isEqualTo(1L);
    }
}
