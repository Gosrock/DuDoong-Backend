package band.gosrock.api.v2.notification.controller

import band.gosrock.api.v2.common.V2Paging
import band.gosrock.api.v2.common.swagger.V2AlsoIn
import band.gosrock.api.v2.common.swagger.V2ApiArea
import band.gosrock.api.v2.common.swagger.V2ApiTags
import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.notification.dto.V2NotificationResponse
import band.gosrock.api.v2.notification.dto.V2ReadNotificationsRequest
import band.gosrock.api.v2.notification.dto.V2ReadNotificationsResponse
import band.gosrock.api.v2.notification.dto.V2UnreadCountResponse
import band.gosrock.api.v2.notification.usecase.V2NotificationUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@V2AlsoIn(V2ApiArea.USER)
@SecurityRequirement(name = "access-token")
@Tag(name = V2ApiTags.NOTIFICATION, description = V2ApiTags.NOTIFICATION_DESCRIPTION)
@RestController
@RequestMapping("/api/v2/me/notifications")
@Validated
class V2NotificationController(
    private val notificationUseCase: V2NotificationUseCase,
) {
    @Operation(summary = "[N-1] 내 알림 목록 (최신 순, Slice: totalElements/totalPages 는 null)")
    @GetMapping
    fun getNotifications(
        @CurrentUserId userId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = V2Paging.DEFAULT_SIZE) @Min(1) @Max(V2Paging.TABLE_MAX_SIZE) size: Int,
    ): V2PageResponse<V2NotificationResponse> = notificationUseCase.list(userId, page, size)

    @Operation(summary = "[N-2] 안읽은 알림 수 (벨 빨간 점)")
    @GetMapping("/unread-count")
    fun getUnreadCount(@CurrentUserId userId: Long): V2UnreadCountResponse = notificationUseCase.unreadCount(userId)

    @Operation(summary = "[N-3] 읽음 처리 (멱등). all=true 면 전체, 아니면 notificationIds(최대 100). 남의 알림·없는 id 는 무시")
    @PostMapping("/read")
    fun read(
        @CurrentUserId userId: Long,
        @RequestBody @Valid request: V2ReadNotificationsRequest,
    ): V2ReadNotificationsResponse = notificationUseCase.read(userId, request)
}
