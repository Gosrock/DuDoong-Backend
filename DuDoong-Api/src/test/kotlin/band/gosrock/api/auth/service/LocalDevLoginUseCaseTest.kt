package band.gosrock.api.auth.service

import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.ForbiddenUserException
import band.gosrock.domain.domains.user.service.UserDomainService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/** 개발용 로그인: oid 는 local: 접두사, 계정 상태 검사를 거친다 (#762 S-1·M-7) */
class LocalDevLoginUseCaseTest {

    private val userDomainService = mock(UserDomainService::class.java)
    private val tokenGenerateHelper = mock(TokenGenerateHelper::class.java)
    private val useCase = LocalDevLoginUseCase(userDomainService, tokenGenerateHelper)
    private val request = RegisterRequest(email = "dev@dudoong.com", name = "개발자")
    private val user = User()
    private val oids = mutableListOf<String?>()

    private fun anyOauthInfo(): OauthInfo = any(OauthInfo::class.java) ?: OauthInfo()

    private fun stubUpsert() {
        `when`(userDomainService.upsertUser(any(Profile::class.java) ?: Profile(), anyOauthInfo())).thenAnswer {
            oids += it.getArgument<OauthInfo>(1).oid
            user
        }
    }

    @Test
    fun `oid 를 local 접두사로 저장하고 로그인 검사를 거친다`() {
        stubUpsert()
        `when`(userDomainService.loginUser(anyOauthInfo())).thenAnswer {
            oids += it.getArgument<OauthInfo>(0).oid
            user
        }
        val response = mock(TokenAndUserResponse::class.java)
        `when`(tokenGenerateHelper.execute(user)).thenReturn(response)

        assertEquals(response, useCase.execute(request))
        assertEquals(listOf("local:dev@dudoong.com", "local:dev@dudoong.com"), oids)
    }

    @Test
    fun `정지 계정이면 토큰을 만들지 않는다`() {
        stubUpsert()
        `when`(userDomainService.loginUser(anyOauthInfo())).thenThrow(ForbiddenUserException.EXCEPTION)

        assertThrows<ForbiddenUserException> { useCase.execute(request) }
        verify(tokenGenerateHelper, never()).execute(user)
    }
}
