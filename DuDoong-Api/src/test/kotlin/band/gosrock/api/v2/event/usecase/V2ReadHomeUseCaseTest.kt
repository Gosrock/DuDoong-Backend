package band.gosrock.api.v2.event.usecase

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseQuery
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.test.util.ReflectionTestUtils

/**
 * P-1 홈 상한 (#721): 홈은 공연을 최대 10개만 요청한다. 공유 H2 데이터와 무관하게 쿼리를 목으로 바꿔 넘기는 limit 만 본다.
 * 쿼리가 limit 을 지키는지는 Domain `V2EventBrowseQueryLimitTest`.
 */
@DisplayName("v2 홈 - 공연 상한")
class V2ReadHomeUseCaseTest {

    private val requestedLimits = mutableListOf<Int>()

    private val events: List<Event> = (1L..10L).map { id ->
        Event(hostId = 1L, name = "공연$id", startAt = LocalDateTime.now().plusDays(id), runTime = 60).also {
            ReflectionTestUtils.setField(it, "id", id)
            ReflectionTestUtils.setField(it, "status", EventStatus.OPEN)
        }
    }

    private val query: V2EventBrowseQuery = mock(V2EventBrowseQuery::class.java) { inv ->
        when (inv.method.name) {
            "findActive" -> events.also { requestedLimits += inv.arguments[1] as Int }
            "findHostNames" -> mapOf(1L to "고스락")
            else -> null
        }
    }

    private val useCase = V2ReadHomeUseCase(query)

    @Test
    fun `홈은 종료 전 등록 공연을 최대 10개 요청한다`() {
        useCase.execute()

        assertEquals(listOf(10), requestedLimits)
    }

    @Test
    fun `쿼리가 돌려준 공연을 순서 그대로 응답한다`() {
        val response = useCase.execute()

        assertEquals((1L..10L).toList(), response.events.map { it.eventId })
    }
}
