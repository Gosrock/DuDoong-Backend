package band.gosrock.api.auth.service

import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor
import band.gosrock.domain.domains.user.exception.HostMasterCannotWithdrawException
import band.gosrock.domain.domains.user.service.UserDomainService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.anyString
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/** 회원 탈퇴: 탈퇴가 성공한 뒤에만 refresh 를 지운다, 개발용 계정은 카카오 연결 해제를 건너뛴다 (#762) */
class WithDrawUseCaseTest {

    private val refreshTokenAdaptor = mock(RefreshTokenAdaptor::class.java)
    private val userDomainService = mock(UserDomainService::class.java)
    private val kakaoOauthHelper = mock(KakaoOauthHelper::class.java)
    private val useCase = WithDrawUseCase(refreshTokenAdaptor, userDomainService, kakaoOauthHelper)

    @Test
    fun `탈퇴 후 refresh 를 지우고 카카오 연결을 해제한다`() {
        `when`(userDomainService.withDrawUser(1L)).thenReturn("12345")

        useCase.execute(1L)

        val order = inOrder(userDomainService, refreshTokenAdaptor, kakaoOauthHelper)
        order.verify(userDomainService).withDrawUser(1L)
        order.verify(refreshTokenAdaptor).deleteByUserId(1L)
        order.verify(kakaoOauthHelper).unlink("12345")
    }

    @Test
    fun `탈퇴가 거절되면 refresh 를 지우지 않는다`() {
        `when`(userDomainService.withDrawUser(1L)).thenThrow(HostMasterCannotWithdrawException.EXCEPTION)

        assertThrows<HostMasterCannotWithdrawException> { useCase.execute(1L) }
        verify(refreshTokenAdaptor, never()).deleteByUserId(1L)
    }

    @Test
    fun `개발용 계정은 카카오 연결 해제를 건너뛴다`() {
        `when`(userDomainService.withDrawUser(1L)).thenReturn("local:dev@dudoong.com")

        useCase.execute(1L)

        verify(refreshTokenAdaptor).deleteByUserId(1L)
        verify(kakaoOauthHelper, never()).unlink(anyString())
    }
}
