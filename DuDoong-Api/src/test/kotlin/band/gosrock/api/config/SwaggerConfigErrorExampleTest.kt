package band.gosrock.api.config

import band.gosrock.api.v2.support.V2TestHandlers
import band.gosrock.common.annotation.ApiErrorCodeExample
import band.gosrock.domain.domains.host.exception.HostErrorCode
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.responses.ApiResponses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationContext
import org.springframework.web.method.HandlerMethod

@DisplayName("SwaggerConfig - 에러 예시 상태 코드가 v2 403 정책과 일치")
class SwaggerConfigErrorExampleTest {

    /** v2 패키지 밖(v1) 문서 핸들러 */
    inner class V1DocsHandler {
        @ApiErrorCodeExample(HostErrorCode::class)
        fun hostErrors() {}
    }

    private val customizer = SwaggerConfig(mock(ApplicationContext::class.java)).customize()

    private fun exampleNamesByStatus(bean: Any): Map<String, Set<String>> {
        val operation = Operation().responses(ApiResponses())
        customizer.customize(operation, HandlerMethod(bean, "hostErrors"))
        return operation.responses.mapValues { (_, response) ->
            response.content["application/json"]!!.examples.keys
        }
    }

    @Test
    @DisplayName("v2 핸들러: 권한 코드 예시는 403, 나머지는 원래 상태")
    fun v2Handler() {
        val examples = exampleNamesByStatus(V2TestHandlers().DocsHandler())

        assertEquals(setOf("HOST_400_1", "HOST_400_2", "HOST_400_4", "HOST_400_6", "HOST_400_7"), examples["403"])
        assertEquals(setOf("HOST_400_3", "HOST_400_5", "HOST_400_8", "HOST_400_9"), examples["400"])
        assertEquals(setOf("Host_404_1", "HOST_404_2"), examples["404"])
    }

    @Test
    @DisplayName("v1 핸들러: 기존대로 권한 코드도 400")
    fun v1Handler() {
        val examples = exampleNamesByStatus(V1DocsHandler())

        assertNull(examples["403"])
        assertEquals(
            setOf("HOST_400_1", "HOST_400_2", "HOST_400_3", "HOST_400_4", "HOST_400_5", "HOST_400_6", "HOST_400_7", "HOST_400_8", "HOST_400_9"),
            examples["400"],
        )
    }
}
