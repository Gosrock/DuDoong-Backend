package band.gosrock.api.v2.mypage.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.mypage.dto.response.V2ArchiveEventResponse
import band.gosrock.api.v2.mypage.dto.response.V2ArchiveHostResponse
import band.gosrock.api.v2.mypage.dto.response.V2ArchiveResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseQuery
import band.gosrock.domain.domains.issuedTicket.service.v2.V2ArchiveQuery
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadArchiveUseCase(
    private val v2ArchiveQuery: V2ArchiveQuery,
    private val v2EventBrowseQuery: V2EventBrowseQuery,
) {
    /** M-5 (#729, DEC-022 #5). 연도 1 + 목록 1 + 건수 1 + 호스트명 1 + 주문 uuid 1 — 페이지 크기와 관계없이 쿼리 수 고정 */
    @Transactional(readOnly = true)
    fun execute(userId: Long, year: Int?, page: Int, size: Int): V2ArchiveResponse {
        val now = LocalDateTime.now()
        val years = v2ArchiveQuery.findArchivedYears(userId, now)
        val events = v2ArchiveQuery.findArchivedEvents(userId, year, now, PageRequest.of(page, size))
        val hostNames = v2EventBrowseQuery.findHostNames(events.content.map { it.hostId }.toSet())
        val orderUuids = v2ArchiveQuery.findMyOrderUuids(userId, events.content.map { it.eventId })
        return V2ArchiveResponse(
            years = years,
            events = V2PageResponse.of(
                events.map {
                    V2ArchiveEventResponse(
                        eventId = it.eventId,
                        name = it.name,
                        posterImageUrl = ImageVo.valueOf(it.posterImageKey).generateImageUrl(),
                        startAt = it.startAt,
                        host = V2ArchiveHostResponse(hostId = it.hostId, name = hostNames[it.hostId]),
                        orderUuid = orderUuids[it.eventId],
                    )
                },
            ),
        )
    }
}
