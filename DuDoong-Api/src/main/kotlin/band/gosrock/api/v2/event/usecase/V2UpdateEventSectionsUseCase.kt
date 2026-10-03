package band.gosrock.api.v2.event.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.event.dto.request.V2EventSectionRequest
import band.gosrock.api.v2.common.V2HtmlSanitizer
import band.gosrock.api.v2.event.dto.response.V2EventSectionResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.EventSection
import band.gosrock.domain.domains.event.domain.EventSectionContentFormat
import band.gosrock.domain.domains.event.exception.InvalidEventSectionException
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2UpdateEventSectionsUseCase(
    private val eventAdaptor: EventAdaptor,
    private val eventRepository: EventRepository,
    private val v2EventDomainService: V2EventDomainService,
) {
    /** 전체 교체. sortOrder 오름차순(같으면 요청 순)으로 정렬해 0부터 다시 매긴다. 첫 섹션('공연 소개') 본문은 v1 content 에도 기록. 본문은 HTML sanitize */
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long, request: List<V2EventSectionRequest?>): List<V2EventSectionResponse> {
        val event = eventAdaptor.findById(eventId)
        // 배열 안의 null 원소는 형식 오류 (500 방지)
        val sections = request.map { it ?: throw InvalidEventSectionException.EXCEPTION }
            .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
            .map { EventSection(title = it.title ?: "", content = it.content?.let(V2HtmlSanitizer::sanitize), contentFormat = EventSectionContentFormat.HTML) }
        v2EventDomainService.replaceSections(event, sections)
        return v2EventDomainService.displaySections(eventRepository.save(event)).map { V2EventSectionResponse.from(it) }
    }
}
