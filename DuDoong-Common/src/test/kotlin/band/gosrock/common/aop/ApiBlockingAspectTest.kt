package band.gosrock.common.aop

import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.common.helper.SpringEnvironmentHelper
import org.aspectj.lang.ProceedingJoinPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.env.MockEnvironment

/** 개발용 API(@DevelopOnlyApi)는 local·dev 프로필에서만 열린다 (#762 S-1·M-7) */
class ApiBlockingAspectTest {

    private fun aspect(vararg profiles: String): ApiBlockingAspect {
        val env = MockEnvironment().apply { setActiveProfiles(*profiles) }
        return ApiBlockingAspect(SpringEnvironmentHelper(env))
    }

    @ParameterizedTest
    @ValueSource(strings = ["local", "dev"])
    fun `local·dev 프로필이면 통과한다`(profile: String) {
        val joinPoint = mock(ProceedingJoinPoint::class.java)
        `when`(joinPoint.proceed()).thenReturn("ok")

        assertEquals("ok", aspect(profile).checkApiAcceptingCondition(joinPoint))
    }

    @ParameterizedTest
    @ValueSource(strings = ["prod", "staging", "test", ""])
    fun `그 밖의 프로필이면 막는다`(profile: String) {
        val joinPoint = mock(ProceedingJoinPoint::class.java)
        val profiles = if (profile.isEmpty()) emptyArray() else arrayOf(profile)

        assertThrows<DuDoongDynamicException> { aspect(*profiles).checkApiAcceptingCondition(joinPoint) }
        verify(joinPoint, never()).proceed()
    }

    @ParameterizedTest
    @ValueSource(strings = ["prod", "staging"])
    fun `dev 와 운영·스테이징 프로필이 함께 켜져 있으면 막는다`(profile: String) {
        val joinPoint = mock(ProceedingJoinPoint::class.java)

        assertThrows<DuDoongDynamicException> { aspect("dev", profile).checkApiAcceptingCondition(joinPoint) }
        verify(joinPoint, never()).proceed()
    }
}
