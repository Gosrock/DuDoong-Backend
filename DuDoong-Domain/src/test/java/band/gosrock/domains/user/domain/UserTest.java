package band.gosrock.domains.user.domain;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    public void 유저프로필변경테스트() {

        // given
        Profile profile = new Profile("곽팔두", "t@naver.com", null, null);
        User user = new User(profile, null, false);
        Profile newProfile = new Profile("홍길동", "a@naver.com", null, null);
        // when
        user.changeProfile(newProfile);
        // then

        assertEquals(newProfile.getEmail(), user.getProfile().getEmail());
        assertEquals(newProfile.getName(), user.getProfile().getName());
    }
}
