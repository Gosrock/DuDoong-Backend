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
        hostProfile = HostProfile.builder().build();
    }

    @Test
    void 호스트_프로필_업데이트_테스트() {
        // given
        final HostProfile newHostProfile =
                HostProfile.builder()
                        .name("테스트")
                        .contactEmail("22@cc.com")
                        .contactNumber("010-0000-0000")
                        .introduce("123")
                        .profileImageKey("key")
                        .build();
        // when
        hostProfile.updateProfile(newHostProfile);
        // then
        assertEquals(hostProfile.getProfileImage(), newHostProfile.getProfileImage());
        assertEquals(hostProfile.getContactEmail(), newHostProfile.getContactEmail());
        assertEquals(hostProfile.getContactNumber(), newHostProfile.getContactNumber());
        assertEquals(hostProfile.getIntroduce(), newHostProfile.getIntroduce());
    }
}
