package band.gosrock.admin.service

import band.gosrock.admin.exception.AdminCannotChangeOwnStatusException
import band.gosrock.admin.exception.AdminSuperAdminRequiredException
import band.gosrock.admin.model.dto.request.AdminUpdateUserStatusRequest
import band.gosrock.common.properties.OauthProperties
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.HostMasterCannotWithdrawException
import band.gosrock.domain.domains.user.service.UserDomainService
import band.gosrock.infrastructure.outer.api.oauth.client.KakaoInfoClient
import band.gosrock.infrastructure.outer.api.oauth.dto.UnlinkKaKaoTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

/** 운영 사용자 상태 변경 (#762 M-2·M-5) */
class AdminUpdateUserStatusUseCaseTest {

    private val userAdaptor = mock(UserAdaptor::class.java)
    private val userDomainService = mock(UserDomainService::class.java)
    private val refreshTokenAdaptor = mock(RefreshTokenAdaptor::class.java)
    private val adminAuthValidator = mock(AdminAuthValidator::class.java)
    private val kakaoInfoClient = mock(KakaoInfoClient::class.java)
    private val useCase = AdminUpdateUserStatusUseCase(
        userAdaptor,
        userDomainService,
        refreshTokenAdaptor,
        adminAuthValidator,
        kakaoInfoClient,
        OauthProperties(OauthProperties.OAuthSecret(adminKey = "admin-key")),
    )

    private val operatorId = 1L
    private val targetId = 2L

    private fun user(id: Long, role: AccountRole, oid: String = "12345") =
        User(oauthInfo = OauthInfo(OauthProvider.KAKAO, oid)).also {
            ReflectionTestUtils.setField(it, "id", id)
            ReflectionTestUtils.setField(it, "accountRole", role)
        }

    private fun stub(operatorRole: AccountRole, target: User) {
        `when`(adminAuthValidator.validateAdminOrAbove(operatorId)).thenReturn(user(operatorId, operatorRole))
        `when`(userAdaptor.queryUser(targetId)).thenReturn(target)
    }

    private fun stubChange(state: AccountState, result: User) {
        `when`(userDomainService.changeAccountStateByAdmin(targetId, state)).thenAnswer {
            ReflectionTestUtils.setField(result, "accountState", state)
            result
        }
    }

    private fun anyUnlinkTarget(): UnlinkKaKaoTarget = any(UnlinkKaKaoTarget::class.java) ?: UnlinkKaKaoTarget("")

    @Test
    fun `ADMIN 이 SUPER_ADMIN 의 상태를 바꾸면 403 이고 아무것도 바꾸지 않는다`() {
        stub(AccountRole.ADMIN, user(targetId, AccountRole.SUPER_ADMIN))

        val e = assertThrows<AdminSuperAdminRequiredException> {
            useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.SUSPENDED))
        }
        assertEquals(403, e.getErrorReason().status)
        verifyNoInteractions(userDomainService, refreshTokenAdaptor, kakaoInfoClient)
    }

    @Test
    fun `SUPER_ADMIN 은 SUPER_ADMIN 의 상태를 바꿀 수 있다`() {
        val target = user(targetId, AccountRole.SUPER_ADMIN)
        stub(AccountRole.SUPER_ADMIN, target)
        stubChange(AccountState.SUSPENDED, target)

        val response = useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.SUSPENDED))

        assertEquals(AccountState.SUSPENDED, response.accountState)
    }

    @Test
    fun `정지하면 refresh 를 지우고 카카오 연결은 그대로 둔다`() {
        val target = user(targetId, AccountRole.USER)
        stub(AccountRole.ADMIN, target)
        stubChange(AccountState.SUSPENDED, target)

        val response = useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.SUSPENDED))

        assertEquals(AccountState.SUSPENDED, response.accountState)
        verify(refreshTokenAdaptor).deleteByUserId(targetId)
        verifyNoInteractions(kakaoInfoClient)
    }

    @Test
    fun `정상으로 되돌리면 refresh 를 지우지 않는다`() {
        val target = user(targetId, AccountRole.USER)
        stub(AccountRole.ADMIN, target)
        stubChange(AccountState.NORMAL, target)

        useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.NORMAL))

        verify(refreshTokenAdaptor, never()).deleteByUserId(anyLong())
    }

    @Test
    fun `탈퇴는 도메인 탈퇴 절차를 거치고 refresh 삭제·카카오 연결 해제를 한다`() {
        val target = user(targetId, AccountRole.USER)
        stub(AccountRole.ADMIN, target)
        stubChange(AccountState.DELETED, target)

        useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.DELETED))

        verify(userDomainService).changeAccountStateByAdmin(targetId, AccountState.DELETED)
        verify(refreshTokenAdaptor).deleteByUserId(targetId)
        verify(kakaoInfoClient).unlinkUser(anyString(), anyUnlinkTarget())
    }

    @Test
    fun `카카오 연결 해제가 실패해도 탈퇴 응답은 성공한다`() {
        val target = user(targetId, AccountRole.USER)
        stub(AccountRole.ADMIN, target)
        stubChange(AccountState.DELETED, target)
        `when`(kakaoInfoClient.unlinkUser(anyString(), anyUnlinkTarget())).thenThrow(RuntimeException("kakao down"))

        val response = useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.DELETED))

        assertEquals(AccountState.DELETED, response.accountState)
    }

    @Test
    fun `개발용 계정 탈퇴는 카카오 연결 해제를 건너뛴다`() {
        val target = user(targetId, AccountRole.USER, oid = "local:dev@dudoong.com")
        stub(AccountRole.ADMIN, target)
        stubChange(AccountState.DELETED, target)

        useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.DELETED))

        verifyNoInteractions(kakaoInfoClient)
    }

    @Test
    fun `호스트 마스터 탈퇴가 거절되면 refresh 를 지우지 않는다`() {
        stub(AccountRole.ADMIN, user(targetId, AccountRole.USER))
        `when`(userDomainService.changeAccountStateByAdmin(targetId, AccountState.DELETED))
            .thenThrow(HostMasterCannotWithdrawException.EXCEPTION)

        assertThrows<HostMasterCannotWithdrawException> {
            useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.DELETED))
        }
        verifyNoInteractions(refreshTokenAdaptor, kakaoInfoClient)
    }

    @Test
    fun `자기 자신의 상태는 바꿀 수 없다`() {
        `when`(adminAuthValidator.validateAdminOrAbove(operatorId)).thenReturn(user(operatorId, AccountRole.ADMIN))

        val e = assertThrows<AdminCannotChangeOwnStatusException> {
            useCase.execute(operatorId, operatorId, AdminUpdateUserStatusRequest(AccountState.SUSPENDED))
        }
        assertEquals(400, e.getErrorReason().status)
        verifyNoInteractions(userDomainService, refreshTokenAdaptor, kakaoInfoClient)
    }

    @Test
    fun `ADMIN 은 다른 ADMIN 의 상태를 바꿀 수 있다`() {
        val target = user(targetId, AccountRole.ADMIN)
        stub(AccountRole.ADMIN, target)
        stubChange(AccountState.SUSPENDED, target)

        assertEquals(AccountState.SUSPENDED, useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.SUSPENDED)).accountState)
    }

    @Test
    fun `refresh 삭제가 실패해도 탈퇴 응답은 성공하고 카카오 연결 해제는 계속한다`() {
        val target = user(targetId, AccountRole.USER)
        stub(AccountRole.ADMIN, target)
        stubChange(AccountState.DELETED, target)
        `when`(refreshTokenAdaptor.deleteByUserId(targetId)).thenThrow(RuntimeException("redis down"))

        assertEquals(AccountState.DELETED, useCase.execute(operatorId, targetId, AdminUpdateUserStatusRequest(AccountState.DELETED)).accountState)
        verify(kakaoInfoClient).unlinkUser(anyString(), anyUnlinkTarget())
    }
}
