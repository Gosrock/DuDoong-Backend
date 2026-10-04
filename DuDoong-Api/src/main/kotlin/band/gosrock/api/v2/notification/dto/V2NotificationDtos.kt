package band.gosrock.api.v2.notification.dto

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.notification.domain.Notification
import band.gosrock.domain.domains.notification.domain.NotificationTargetType
import band.gosrock.domain.domains.notification.domain.NotificationType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class V2NotificationResponse(
    val id: Long,
    @field:Schema(description = "HOST_MEMBER_ADDED / ORDER_PENDING_APPROVE / ORDER_APPROVED / ORDER_REFUSED / ORDER_REFUND_REQUESTED(사용자 환불 요청, 호스트 마스터·매니저) / ORDER_CANCELED_BY_USER(돌려줄 돈 없는 사용자 취소, 호스트 마스터·매니저)")
    val type: NotificationType,
    val title: String,
    val body: String,
    val target: V2NotificationTarget,
    @field:Schema(description = "부가 정보 (hostName, role, eventName, orderNo, refuseReasonType, refuseReason 등 종류별)")
    val extra: Map<String, String>,
    val isRead: Boolean,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val createdAt: LocalDateTime?,
) {
    companion object {
        private val OBJECT_MAPPER = ObjectMapper()
        private val log = LoggerFactory.getLogger(V2NotificationResponse::class.java)

        /** extra JSON 파싱. 깨진 값이면 빈 맵으로 내리고 warn 로그 (목록 전체가 실패하지 않게) */
        private fun parseExtra(notification: Notification): Map<String, String> {
            val raw = notification.extra ?: return emptyMap()
            return try {
                OBJECT_MAPPER.readValue<Map<String, String>>(raw)
            } catch (e: Exception) {
                log.warn("[알림센터] extra 파싱 실패 (notificationId={}): {}", notification.id, e.message)
                emptyMap()
            }
        }

        fun of(notification: Notification) = V2NotificationResponse(
            id = notification.id!!,
            type = notification.type,
            title = notification.title,
            body = notification.body,
            target = V2NotificationTarget(type = notification.targetType, id = notification.targetId, eventId = notification.eventId),
            extra = parseExtra(notification),
            isRead = notification.isRead,
            createdAt = notification.createdAt,
        )
    }
}

data class V2NotificationTarget(
    @field:Schema(description = "HOST: id = hostId, ORDER: id = orderUuid (+ eventId)")
    val type: NotificationTargetType,
    val id: String,
    val eventId: Long?,
)

data class V2UnreadCountResponse(val count: Long)

data class V2ReadNotificationsRequest(
    @field:Schema(description = "읽음 처리할 알림 id. 본인 것이 아니거나 없는 id 는 무시")
    @field:Size(max = 100)
    val notificationIds: List<Long>? = null,
    @field:Schema(description = "true 면 전체 읽음 (notificationIds 무시)")
    val all: Boolean? = null,
)

data class V2ReadNotificationsResponse(
    @field:Schema(description = "이번 요청으로 읽음 처리된 건수 (이미 읽은 것·남의 것 제외)")
    val updatedCount: Int,
    @field:Schema(description = "처리 후 안읽음 수")
    val unreadCount: Long,
)
