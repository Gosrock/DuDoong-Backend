package band.gosrock.api.v2.notification.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.notification.dto.V2NotificationResponse
import band.gosrock.api.v2.notification.dto.V2ReadNotificationsRequest
import band.gosrock.api.v2.notification.dto.V2ReadNotificationsResponse
import band.gosrock.api.v2.notification.dto.V2UnreadCountResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import org.springframework.data.domain.PageRequest

@UseCase
class V2NotificationUseCase(
    private val notificationDomainService: V2NotificationDomainService,
) {
    fun list(userId: Long, page: Int, size: Int): V2PageResponse<V2NotificationResponse> =
        V2PageResponse.of(notificationDomainService.querySlice(userId, PageRequest.of(page, size)).map { V2NotificationResponse.of(it) })

    fun unreadCount(userId: Long): V2UnreadCountResponse = V2UnreadCountResponse(notificationDomainService.countUnread(userId))

    fun read(userId: Long, request: V2ReadNotificationsRequest): V2ReadNotificationsResponse {
        val updated = notificationDomainService.markRead(userId, request.notificationIds, request.readAll == true)
        return V2ReadNotificationsResponse(updatedCount = updated.toLong(), unreadCount = notificationDomainService.countUnread(userId))
    }
}
