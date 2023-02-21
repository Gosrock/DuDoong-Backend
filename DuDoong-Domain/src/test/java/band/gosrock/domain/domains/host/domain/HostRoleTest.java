package band.gosrock.domain.domains.host.domain;

import static band.gosrock.domain.domains.host.domain.HostRole.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HostRoleTest {

    @Test
    void ENUM_값에_해당하지_않으면_NULL_반환해야한다() {
        // given
        // when
        // then
        assertEquals(HostRole.fromHostRole("MASTER"), MASTER);
        assertEquals(HostRole.fromHostRole("MANAGER"), MANAGER);
        assertEquals(HostRole.fromHostRole("GUEST"), GUEST);
        assertNull(HostRole.fromHostRole("NOT_PROVIDED"));
    }
}
