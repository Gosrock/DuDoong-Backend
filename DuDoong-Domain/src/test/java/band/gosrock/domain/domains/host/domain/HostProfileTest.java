package band.gosrock.domain.domains.host.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HostProfileTest {

    @Mock Host host;

    HostProfile hostProfile;

    @BeforeEach
    void setup() {
        hostProfile = new HostProfile();
    }

    @Test
    void 호스트_프로필_업데이트_테스트() {
        // given
        final HostProfile newHostProfile =
                new HostProfile("테스트", "123", "key", "22@cc.com", "010-0000-0000");
        // when
        hostProfile.updateProfile(newHostProfile);
        // then
        assertEquals(hostProfile.getProfileImage(), newHostProfile.getProfileImage());
        assertEquals(hostProfile.getContactEmail(), newHostProfile.getContactEmail());
        assertEquals(hostProfile.getContactNumber(), newHostProfile.getContactNumber());
        assertEquals(hostProfile.getIntroduce(), newHostProfile.getIntroduce());
    }
}
