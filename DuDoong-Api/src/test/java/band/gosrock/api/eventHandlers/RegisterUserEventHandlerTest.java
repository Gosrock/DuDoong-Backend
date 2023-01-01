package band.gosrock.api.eventHandlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.Profile;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class RegisterUserEventHandlerTest {

    @Autowired UserAdaptor userDomainService;
    @MockBean RegisterUserEventHandler registerUserEventHandler;

    @Test
    void 유저등록시도메인이벤트가발생해야한다() {
        // given
        Profile profile = Profile.builder().build();
        OauthInfo oauthInfo = OauthInfo.builder().build();
        //        given(registerUserEventHandler.handleRegisterUserEvent(any())).will(new Answer() {
        //            @Override
        //            public UserRegisterEvent answer(InvocationOnMock invocation) throws Throwable
        // {
        //                Object[] args = invocation.getArguments();
        //                UserRegisterEvent userRegisterEvent = (UserRegisterEvent) args[0];
        //                System.out.println(userRegisterEvent.getUserId());
        //                return UserRegisterEvent.builder().build();
        //            }
        //        });
        // when
        userDomainService.registerUser(profile, oauthInfo);

        // then
        BDDMockito.then(registerUserEventHandler).should(times(1)).handleRegisterUserEvent(any());
    }
}