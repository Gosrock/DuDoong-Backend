package band.gosrock.domain.domains.host.domain;

import static band.gosrock.domain.domains.host.domain.HostRole.MANAGER;
import static band.gosrock.domain.domains.host.domain.HostRole.MASTER;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HostUserTest {

    @Mock Host host;
    HostUser hostUser;

    @BeforeEach
    void setup() {
        hostUser = HostUser.builder().host(host).role(MASTER).build();
    }

    @Test
    void 호스트유저_권한변경_테스트() {
        // given
        final HostRole role = MANAGER;
        // when
        hostUser.setHostRole(role);
        // then
        assertEquals(hostUser.getRole(), role);
    }

    @Test
    void 호스트유저_초대수락으로_활성화_테스트() {
        // given
        // when
        hostUser.activate();
        // then
        assertEquals(hostUser.getActive(), TRUE);
    }

    @Test
    void 호스트유저_초대_중복수락은_불가능하다() {
        // given
        // when
        hostUser.activate();
        // then
        assertThrows(AlreadyJoinedHostException.class, () -> hostUser.activate());
    }
}
