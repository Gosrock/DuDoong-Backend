package band.gosrock.api.event.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.api.event.model.mapper.EventMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.exception.InvalidEventImageKeyException
import band.gosrock.domain.domains.event.service.EventService
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdateEventDetailUseCase(
    private val eventService: EventService,
    private val eventAdaptor: EventAdaptor,
    private val eventMapper: EventMapper,
    private val presignedUrlService: S3UploadPresignedUrlService,
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long, updateEventDetailRequest: UpdateEventDetailRequest): EventResponse {
        val event = eventAdaptor.findById(eventId)
        validatePosterImageKey(event, updateEventDetailRequest.posterImageKey)
        return EventResponse.of(
            eventService.updateEventDetail(event, eventMapper.toEventDetail(updateEventDetailRequest))
        )
    }

    /** 지금 저장된 포스터 key 를 그대로 다시 보내는 경우 외에는, 이 공연에 발급한 형식 그대로의 key 여야 한다 */
    private fun validatePosterImageKey(event: Event, key: String?) {
        if (key == null || key == event.eventDetail?.posterImage?.imageKey) return
        if (!presignedUrlService.isEventImageKey(event.id!!, key)) {
            throw InvalidEventImageKeyException.EXCEPTION
        }
    }
}
