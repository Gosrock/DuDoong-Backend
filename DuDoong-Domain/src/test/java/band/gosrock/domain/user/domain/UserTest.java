package band.gosrock.domain.user.domain;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.domain.domain.user.domain.Profile;
import band.gosrock.domain.domain.user.domain.User;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    public void 유저프로필변경테스트() {

        // given
        Profile profile = Profile.builder().email("t@naver.com").name("곽팔두").build();
        User user = User.builder().profile(profile).build();
        Profile newProfile = Profile.builder().email("a@naver.com").name("홍길동").build();
        // when
        user.changeProfile(newProfile);
        // then

        assertEquals(newProfile.getEmail(), user.getProfile().getEmail());
        assertEquals(newProfile.getName(), user.getProfile().getName());
    }
}
