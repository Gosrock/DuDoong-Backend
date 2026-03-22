package band.gosrock.api.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import band.gosrock.api.email.handler.RegisterUserEventEmailHandler;
import band.gosrock.api.supports.ApiIntegrateSpringBootTest;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.OauthProvider;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.service.UserDomainService;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@ApiIntegrateSpringBootTest
class RegisterUserEventHandlerTest {

    @Autowired UserDomainService userDomainService;
    @MockBean RegisterUserEventEmailHandler registerUserEventHandler;

    @Test
    void 유저등록시도메인이벤트가발생해야한다() {
        // given
        Profile profile = new Profile("test", "test@test.com", null, null);
        OauthInfo oauthInfo = new OauthInfo(OauthProvider.KAKAO, "test-oid");
        // when
        userDomainService.registerUser(profile, oauthInfo, Boolean.TRUE);

        // then
        BDDMockito.then(registerUserEventHandler).should(times(1)).handleRegisterUserEvent(any());
    }
}
