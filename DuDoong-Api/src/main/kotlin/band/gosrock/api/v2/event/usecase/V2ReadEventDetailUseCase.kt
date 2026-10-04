package band.gosrock.api.v2.event.usecase

import band.gosrock.api.v2.event.dto.response.V2EventDetailResponse
import band.gosrock.api.v2.event.dto.response.V2EventHostSummaryResponse
import band.gosrock.api.v2.event.dto.response.V2EventPlaceResponse
import band.gosrock.api.v2.tag.dto.V2TagResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseDomainService
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadEventDetailUseCase(
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val tagAdaptor: TagAdaptor,
    private val v2EventBrowseDomainService: V2EventBrowseDomainService,
) {
    /** 공개 API. 준비중·삭제 공연은 멤버여도 404 (호스트는 E-3 관리 화면을 쓴다) */
    @Transactional(readOnly = true)
    fun execute(eventId: Long): V2EventDetailResponse {
        val event = eventAdaptor.findById(eventId)
        v2EventBrowseDomainService.validatePublic(event)
        val host = hostAdaptor.findById(event.hostId!!)
        val tags = tagAdaptor.findAllByIdIn(event.getTagIds())
            .sortedWith(compareBy({ it.category.ordinal }, { it.sortOrder }, { it.id }))
        return V2EventDetailResponse(
            eventId = event.id!!,
            name = event.getEventName(),
            posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
            startAt = event.getStartAt(),
            endAt = event.getEndAt(),
            runTime = event.eventBasic?.runTime,
            place = event.eventPlace?.takeIf { it.placeName != null || it.placeAddress != null }?.let {
                V2EventPlaceResponse(name = it.placeName, address = it.placeAddress, latitude = it.latitude, longitude = it.longitude)
            },
            hasTicket = event.hasTicket,
            tags = tags.map { V2TagResponse.from(it) },
            host = V2EventHostSummaryResponse(
                hostId = host.id!!,
                name = host.profile?.name,
                profileImageUrl = host.profile?.profileImage?.generateImageUrl(),
            ),
            contacts = v2EventBrowseDomainService.displayContacts(event, host),
            displayStatus = V2EventDisplayRule.of(event, LocalDateTime.now()),
        )
    }
}
