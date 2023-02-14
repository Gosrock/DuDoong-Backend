package band.gosrock.domain.domains.host.domain;

import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException;
import band.gosrock.domain.domains.host.exception.CannotModifyMasterHostRoleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class HostTest {
    @Mock HostUser masterHostUser;
    @Mock HostUser managerHostUser;
    @Mock HostUser guestHostUser;
    @Mock HostProfile hostProfile;

    Host host;
    final Long masterUserId = 1L;
    final Long managerUserId = 2L;
    final Long guestUserId = 3L;

    @BeforeEach
    void setup() {
        host = Host.builder().masterUserId(1L).build();
    }

    @Test
    public void 호스트에_유저_1명_추가기능_검증() {
        // given
        given(managerHostUser.getUserId()).willReturn(managerUserId);
        // when
        host.addHostUsers(Set.of(managerHostUser));
        // then
        final HostUser findUser =
                host.getHostUsers().stream()
                        .filter(hostUser -> hostUser.getUserId().equals(managerUserId))
                        .findFirst()
                        .orElse(null);
        assertEquals(findUser, managerHostUser);
    }

    @Test
    public void 호스트유저_추가후_유저ID로_가져오기_검증() {
        // given
        given(masterHostUser.getUserId()).willReturn(masterUserId);
        // when
        host.addHostUsers(Set.of(masterHostUser));
        // then
        final HostUser findUser =
                host.getHostUsers().stream()
                        .filter(hostUser -> hostUser.getUserId().equals(masterUserId))
                        .findFirst()
                        .orElse(null);
        assertEquals(host.getHostUserByUserId(masterUserId), findUser);
    }

    @Test
    public void 호스트유저ID로_포함여부_검증() {
        // given
        given(masterHostUser.getUserId()).willReturn(masterUserId);
        // when
        host.addHostUsers(Set.of(masterHostUser));
        // then
        assertTrue(host.hasHostUserId(masterUserId));
    }

    @Test
    public void 호스트유저로_포함여부_검증() {
        // given
        given(masterHostUser.getUserId()).willReturn(masterUserId);
        // when
        host.addHostUsers(Set.of(masterHostUser));
        // then
        assertTrue(host.hasHostUser(masterHostUser));
    }

    @Test
    public void 호스트에_유저_여러명_추가기능_검증() {
        // given
        given(masterHostUser.getUserId()).willReturn(masterUserId);
        given(managerHostUser.getUserId()).willReturn(managerUserId);
        // when
        host.addHostUsers(Set.of(masterHostUser, managerHostUser));
        // then
        assertEquals(host.getHostUserByUserId(masterUserId), masterHostUser);
        assertEquals(host.getHostUserByUserId(managerUserId), managerHostUser);
    }

    @Test
    public void 호스트에_유저_초대기능_검증() {
        // given
        given(managerHostUser.getUserId()).willReturn(managerUserId);
        // when
        host.inviteHostUsers(Set.of(managerHostUser));
        // then
        assertEquals(host.getHostUserByUserId(managerUserId), managerHostUser);
    }

    @Test
    public void 이미있는_호스트유저는_초대불가능() {
        // given
        given(managerHostUser.getUserId()).willReturn(managerUserId);
        // when
        host.addHostUsers(Set.of(managerHostUser));
        // then
        assertThrows(
                AlreadyJoinedHostException.class,
                () -> {
                    host.inviteHostUsers(Set.of(managerHostUser));
                });
    }

    @Test
    public void 호스트_프로필_변경_호출되었는지_검증() {
        // given
        given(hostProfile.getIntroduce()).willReturn("소개");
        given(hostProfile.getContactNumber()).willReturn("num");
        given(hostProfile.getProfileImage()).willReturn(null);
        given(hostProfile.getContactEmail()).willReturn("mail");
        // when
        host.updateProfile(hostProfile);
        // then
        assertEquals(host.getProfile().getIntroduce(), hostProfile.getIntroduce());
        assertEquals(host.getProfile().getContactEmail(), hostProfile.getContactEmail());
        assertEquals(host.getProfile().getContactNumber(), hostProfile.getContactNumber());
        assertEquals(host.getProfile().getProfileImage(), hostProfile.getProfileImage());
    }

    @Test
    public void 마스터호스트_역할변경_불가_검증() {
        // given
        // then
        // @BeforeEach 에서 마스터 ID 는 현재 1임
        assertThrows(
                CannotModifyMasterHostRoleException.class,
                () -> {
                    host.setHostUserRole(masterUserId, HostRole.MANAGER);
                });
    }

    @Test
    public void 호스트_역할변경_검증() {
        // given
        given(masterHostUser.getUserId()).willReturn(masterUserId);
        given(managerHostUser.getUserId()).willReturn(managerUserId);
        // when
        host.addHostUsers(Set.of(masterHostUser, managerHostUser));
        // then
        host.setHostUserRole(managerUserId, HostRole.MANAGER);
        verify(managerHostUser).setHostRole(HostRole.MANAGER);
        // HostUser 에서 역할 변경 호출시 성공
    }

    @Test
    public void 호스트유저_매니저인지_검증() {
        // given
        given(masterHostUser.getUserId()).willReturn(masterUserId);
        given(masterHostUser.getRole()).willReturn(HostRole.MASTER);
        given(managerHostUser.getUserId()).willReturn(managerUserId);
        given(managerHostUser.getRole()).willReturn(HostRole.MANAGER);
        given(guestHostUser.getUserId()).willReturn(guestUserId);
        given(guestHostUser.getRole()).willReturn(HostRole.GUEST);
        // when
        host.addHostUsers(Set.of(masterHostUser, managerHostUser, guestHostUser));
        // then
        // 게스트, 마스터는 매니저 x
        assertFalse(host.isManagerHostUserId(masterUserId));
        assertTrue(host.isManagerHostUserId(managerUserId));
        assertFalse(host.isManagerHostUserId(guestUserId));
    }

    // todo :: activate 유저 테스트부터 선행
    //    @Test
    //    public void 호스트유저_매니저_권한_검증() {
    //        //given
    //        given(masterHostUser.getUserId()).willReturn(masterUserId);
    //        given(masterHostUser.getRole()).willReturn(HostRole.MASTER);
    //        given(managerHostUser.getUserId()).willReturn(managerUserId);
    //        given(managerHostUser.getRole()).willReturn(HostRole.MANAGER);
    //        given(guestHostUser.getUserId()).willReturn(guestUserId);
    //        given(guestHostUser.getRole()).willReturn(HostRole.GUEST);
    //
    //        //when
    //        host.addHostUsers(Set.of(masterHostUser, managerHostUser, guestHostUser));
    //
    //        //then
    //        //게스트는 권한이 없음
    //        assertThrows(NotManagerHostException.class, () -> {
    //            host.validateManagerHostUser(guestUserId);
    //        });
    //        //마스터와 매니저는 권한 부여
    //        assertDoesNotThrow(() -> {
    //            host.validateManagerHostUser(managerUserId);
    //            host.validateManagerHostUser(masterUserId);
    //        });
    //        //마스터와 매니저 모두 매니저 호스트여야함
    //    }
}
