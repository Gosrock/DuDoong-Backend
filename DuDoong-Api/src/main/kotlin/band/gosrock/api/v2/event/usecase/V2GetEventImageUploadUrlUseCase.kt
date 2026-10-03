package band.gosrock.api.v2.event.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.event.dto.request.V2EventImageUploadRequest
import band.gosrock.api.v2.event.dto.response.V2EventImageUploadResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2GetEventImageUploadUrlUseCase(
    private val eventAdaptor: EventAdaptor,
    private val presignedUrlService: S3UploadPresignedUrlService,
) {
    /** 포스터·본문 모두 기존 공연 이미지 경로(event/{eventId}/...)를 쓴다. 정산중·지난공연은 발급하지 않는다 */
    @Transactional(readOnly = true)
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long, request: V2EventImageUploadRequest): V2EventImageUploadResponse {
        eventAdaptor.findById(eventId).validateEditableV2()
        return V2EventImageUploadResponse.of(request.purpose!!, presignedUrlService.forEvent(eventId, request.extension!!))
    }
}
