package band.gosrock.api.common.aop.hostRole

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@DisplayName("HostRoleAop")
class HostRoleAopTest {

    private lateinit var hostRoleAop: HostRoleAop

    @Mock
    private lateinit var hostCallTransactionFactory: HostCallTransactionFactory

    @BeforeEach
    fun setUp() {
        hostRoleAop = HostRoleAop(hostCallTransactionFactory)
    }

    @Nested
    @DisplayName("getId")
    inner class GetIdTest {

        @Test
        @DisplayName("파라미터 이름으로 userId를 찾아 반환한다")
        fun getUserIdSuccess() {
            val parameterNames = arrayOf("userId", "eventId")
            val args = arrayOf<Any?>(1L, 2L)

            val result = hostRoleAop.getId(parameterNames, args, "userId")

            assertEquals(1L, result)
        }

        @Test
        @DisplayName("파라미터 이름으로 eventId를 찾아 반환한다")
        fun getEventIdSuccess() {
            val parameterNames = arrayOf("userId", "eventId", "request")
            val args = arrayOf<Any?>(1L, 2L, "dummy")

            val result = hostRoleAop.getId(parameterNames, args, "eventId")

            assertEquals(2L, result)
        }

        @Test
        @DisplayName("userId 파라미터가 없으면 IllegalArgumentException 발생")
        fun noUserIdThrowsException() {
            val parameterNames = arrayOf("eventId", "request")
            val args = arrayOf<Any?>(2L, "dummy")

            assertThrows(IllegalArgumentException::class.java) {
                hostRoleAop.getId(parameterNames, args, "userId")
            }
        }

        @Test
        @DisplayName("파라미터 이름이 없는 경우 IllegalArgumentException 발생")
        fun emptyParamsThrowsException() {
            val parameterNames = emptyArray<String>()
            val args = emptyArray<Any?>()

            assertThrows(IllegalArgumentException::class.java) {
                hostRoleAop.getId(parameterNames, args, "userId")
            }
        }
    }
}
