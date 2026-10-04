package band.gosrock.domain.domains.event.service.v2

import band.gosrock.domain.DomainIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.annotation.Transactional

/**
 * P-1 홈 쿼리 상한 경계 (#721). 기준 시각을 먼 미래(2099)로 잡아 다른 테스트가 만든 공연이 '종료 전'에 걸리지 않게 하고,
 * 이 테스트가 만든 공연만 후보가 되게 한다 (공유 H2 데이터 비의존). 트랜잭션 롤백으로 남기지 않는다.
 */
@DomainIntegrateSpringBootTest
@Transactional
@DisplayName("v2 공연 탐색 쿼리 - 홈 상한")
class V2EventBrowseQueryLimitTest {

    @Autowired private lateinit var query: V2EventBrowseQuery

    @Autowired private lateinit var eventRepository: EventRepository

    private val now: LocalDateTime = LocalDateTime.of(2099, 1, 1, 12, 0)

    private fun saveOpenEvents(count: Int): List<Long> = (1..count).map { i ->
        eventRepository.save(
            Event(hostId = 1L, name = "상한$i", startAt = now.plusHours(i.toLong()), runTime = 60).also {
                ReflectionTestUtils.setField(it, "status", EventStatus.OPEN)
            },
        ).id!!
    }

    @Test
    fun `후보가 11개면 시작 임박순 앞 10개만 돌려준다`() {
        val ids = saveOpenEvents(11)

        assertEquals(ids.take(10), query.findActive(now, 10).map { it.id })
    }

    @Test
    fun `후보가 정확히 10개면 10개 모두 돌려준다`() {
        val ids = saveOpenEvents(10)

        assertEquals(ids, query.findActive(now, 10).map { it.id })
    }
}
