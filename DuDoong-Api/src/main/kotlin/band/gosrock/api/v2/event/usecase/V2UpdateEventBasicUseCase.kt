package band.gosrock.api.v2.event.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.event.dto.request.V2UpdateEventBasicRequest
import band.gosrock.api.v2.event.dto.response.V2EventManageResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.exception.InvalidEventContactException
import band.gosrock.domain.domains.event.exception.InvalidEventImageKeyException
import band.gosrock.domain.domains.event.exception.InvalidEventTagException
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2UpdateEventBasicUseCase(
    private val eventAdaptor: EventAdaptor,
    private val eventRepository: EventRepository,
    private val v2EventDomainService: V2EventDomainService,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val presignedUrlService: S3UploadPresignedUrlService,
    private val readEventManageUseCase: V2ReadEventManageUseCase,
) {
    /** 등록(OPEN) 후에도 수정 가능 (DEC-007, 단 시작 시각은 현재 이후로만). hasTicket 은 준비중일 때만(유효 티켓이 있으면 false 불가), 정산중·지난공연은 수정 불가 */
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long, request: V2UpdateEventBasicRequest): V2EventManageResponse {
        validateImageKey(eventId, request.posterImageKey)
        val event = eventAdaptor.findById(eventId)
        v2EventDomainService.updateBasic(
            event = event,
            name = request.name?.trim(),
            startAt = request.startAt,
            endAt = request.endAt,
            hasTicket = request.hasTicket,
            posterImageKey = request.posterImageKey,
            place = request.place?.toEventPlace(),
            // '티켓 없음' 으로 바꿀 때만 유효 티켓을 조회한다
            hasValidTicket = request.hasTicket == false && event.hasTicket && ticketItemAdaptor.existsValidByEventId(eventId),
        )
        // 배열 안의 null 원소는 형식 오류 (500 방지)
        request.contacts?.let { contacts ->
            v2EventDomainService.replaceContacts(event, contacts.map { it?.toEntity() ?: throw InvalidEventContactException.EXCEPTION })
        }
        request.tagIds?.let { tagIds -> v2EventDomainService.replaceTags(event, tagIds.map { it ?: throw InvalidEventTagException.EXCEPTION }) }
        return readEventManageUseCase.toResponse(userId, eventRepository.save(event))
    }

    /** 빈 문자열(포스터 제거)이거나, E-10 이 이 공연에 발급한 key 여야 한다. 외부 URL / 다른 공연 key 거부 */
    private fun validateImageKey(eventId: Long, key: String?) {
        if (key == null || key.isEmpty()) return
        if (!key.startsWith(presignedUrlService.eventImageKeyPrefix(eventId)) || key.contains("..")) {
            throw InvalidEventImageKeyException.EXCEPTION
        }
    }
}
