package band.gosrock.domain.domains.issuedTicket.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.PhoneNumberVo;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssuedTicketUserInfoVoTest {

    @Mock User user;

    IssuedTicketUserInfoVo userInfoVo;

    Profile profile;

    private final PhoneNumberVo phoneNumberVo = PhoneNumberVo.valueOf("010-1234-5678");

    @BeforeEach
    void setUp() {
        userInfoVo =
                IssuedTicketUserInfoVo.builder()
                        .userId(1L)
                        .userName("test")
                        .phoneNumber(phoneNumberVo)
                        .email("test@test.com")
                        .build();
        profile =
                Profile.builder()
                        .profileImage("test")
                        .phoneNumber("010-1234-5678")
                        .name("test")
                        .email("test@test.com")
                        .build();
    }

    @Test
    public void 유저_정보를_발급티켓_유저_인포로_변환_테스트() {
        // given
        given(user.getProfile()).willReturn(profile);

        // when
        IssuedTicketUserInfoVo userInfoVoForTest = IssuedTicketUserInfoVo.from(user);

        // then
        assertAll(
                () -> assertEquals(userInfoVo.getUserName(), userInfoVoForTest.getUserName()),
                () ->
                        assertEquals(
                                userInfoVo.getPhoneNumber().getPhoneNumber(),
                                userInfoVoForTest.getPhoneNumber().getPhoneNumber()),
                () -> assertEquals(userInfoVo.getEmail(), userInfoVoForTest.getEmail()));
    }
}
