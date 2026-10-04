package band.gosrock.api.v2.event.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.event.dto.response.V2EventListItemResponse
import band.gosrock.api.v2.tag.dto.V2TagResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseDomainService
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseQuery
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseSearch
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2SearchEventsUseCase(
    private val v2EventBrowseQuery: V2EventBrowseQuery,
    private val v2EventBrowseDomainService: V2EventBrowseDomainService,
    private val tagAdaptor: TagAdaptor,
) {
    /**
     * 공개 API. 정렬 UPCOMING: 다가오는 공연(시작 임박순) → 지난 공연(최근 시작 순). 준비중·삭제 공연은 제외.
     * 태그·호스트명은 페이지 단위로 한 번씩 일괄 조회한다 (N+1 없음)
     */
    @Transactional(readOnly = true)
    fun execute(keyword: String?, tagIds: List<Long>, includePast: Boolean, page: Int, size: Int): V2PageResponse<V2EventListItemResponse> {
        val now = LocalDateTime.now()
        val search = V2EventBrowseSearch(
            keyword = keyword,
            tagIdGroups = v2EventBrowseDomainService.tagFilterGroups(tagIds),
            includePast = includePast,
        )
        val events = v2EventBrowseQuery.search(search, now, PageRequest.of(page, size))
        val eventIds = events.content.map { it.id!! }
        val tagIdsByEvent = v2EventBrowseQuery.findTagIdsByEventIds(eventIds)
        val tagsById = tagAdaptor.findAllByIdIn(tagIdsByEvent.values.flatten().toSet()).associateBy { it.id!! }
        val hostNames = v2EventBrowseQuery.findHostNames(events.content.mapNotNull { it.hostId }.toSet())
        return V2PageResponse.of(
            events.map { event ->
                V2EventListItemResponse(
                    eventId = event.id!!,
                    name = event.getEventName(),
                    posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
                    startAt = event.getStartAt(),
                    endAt = event.getEndAt(),
                    placeName = event.eventPlace?.placeName,
                    hostName = hostNames[event.hostId],
                    tags = tagIdsByEvent[event.id].orEmpty().mapNotNull { tagsById[it] }
                        .sortedWith(compareBy({ it.category.ordinal }, { it.sortOrder }, { it.id }))
                        .map { V2TagResponse.from(it) },
                    displayStatus = v2EventBrowseDomainService.displayStatusOf(event, now),
                )
            },
        )
    }
}
