package band.gosrock.api.v2.event.usecase

import band.gosrock.api.v2.event.dto.response.V2HomeEventResponse
import band.gosrock.api.v2.event.dto.response.V2HomeResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseQuery
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadHomeUseCase(
    private val v2EventBrowseQuery: V2EventBrowseQuery,
) {
    /** 공개 API. 홈 캐러셀 = 종료 전 등록 공연(진행 중 + 시작 전), 시작 임박순 — 진행 중이 앞 (DEC-022, #716 M-1) */
    @Transactional(readOnly = true)
    fun execute(): V2HomeResponse {
        val now = LocalDateTime.now()
        val events = v2EventBrowseQuery.findActive(now, HOME_EVENT_LIMIT)
        val hostNames = v2EventBrowseQuery.findHostNames(events.mapNotNull { it.hostId }.toSet())
        return V2HomeResponse(
            events = events.map {
                V2HomeEventResponse(
                    eventId = it.id!!,
                    name = it.getEventName(),
                    posterImageUrl = it.eventDetail?.posterImage?.generateImageUrl(),
                    startAt = it.getStartAt(),
                    placeName = it.eventPlace?.placeName,
                    hostName = hostNames[it.hostId],
                    displayStatus = V2EventDisplayRule.of(it, now),
                )
            },
        )
    }

    companion object {
        const val HOME_EVENT_LIMIT = 10
    }
}
