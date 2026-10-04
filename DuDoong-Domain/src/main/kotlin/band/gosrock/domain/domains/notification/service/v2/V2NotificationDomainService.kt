package band.gosrock.domain.domains.notification.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.notification.domain.Notification
import band.gosrock.domain.domains.notification.domain.NotificationTargetType
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationBulkRepository
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.transaction.annotation.Transactional

/**
 * v2 알림센터 (#714). 알림 테이블은 v2 에만 있다 (v1 코드는 이 서비스를 호출하지 않는다).
 *
 * 저장(`notify*`)은 도메인 이벤트 핸들러가 원 트랜잭션 커밋 후 비동기로 부른다. 커밋된 데이터를 다시 읽어 수신자·문구를 정하고,
 * 조건이 맞지 않으면(결제형 주문, 이미 상태가 바뀐 주문 등) 저장하지 않는다. 같은 알림의 재처리는 uk 로 건너뛴다(멱등).
 */
@DomainService
@Transactional(readOnly = true)
class V2NotificationDomainService(
    private val notificationRepository: NotificationRepository,
    private val notificationBulkRepository: NotificationBulkRepository,
    private val hostAdaptor: HostAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val orderAdaptor: OrderAdaptor,
) {

    // ===== 저장 =====

    /** 호스트 멤버 추가 → 추가된 사용자. 핸들러 실행 전에 삭제된 멤버는 건너뛴다. @return 저장 건수 */
    @Transactional
    fun notifyHostMembersAdded(hostId: Long, userIds: List<Long>): Int {
        val host = hostAdaptor.findById(hostId)
        val hostName = host.profile?.name.orEmpty()
        val notifications = userIds.distinct().mapNotNull { userId ->
            val hostUser = host.hostUsers.firstOrNull { it.userId == userId && it.active } ?: return@mapNotNull null
            draft(
                userId = userId,
                type = NotificationType.HOST_MEMBER_ADDED,
                title = "호스트 멤버로 추가되었어요",
                body = "'$hostName' 호스트에 ${hostUser.role.value}(으)로 추가되었습니다.",
                targetType = NotificationTargetType.HOST,
                targetId = hostId.toString(),
                extra = mapOf("hostName" to hostName, "role" to hostUser.role.name),
                dedupKey = "host_user:${hostUser.id}",
            )
        }
        return notificationBulkRepository.insertSkippingDuplicates(notifications)
    }

    /** 승인형 주문 접수 → 호스트의 활성 마스터·매니저 전원 (일반 멤버·초대 대기 제외). 결제형 주문은 저장 안 함 */
    @Transactional
    fun notifyOrderPendingApprove(orderUuid: String): Int {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (order.orderMethod != OrderMethod.APPROVAL || order.orderStatus != OrderStatus.PENDING_APPROVE) return 0
        val event = eventAdaptor.findById(order.eventId!!)
        val host = hostAdaptor.findById(event.hostId!!)
        val eventName = event.eventBasic?.name.orEmpty()
        val recipients = host.hostUsers
            .filter { it.active && (it.role == HostRole.MASTER || it.role == HostRole.MANAGER) }
            .mapNotNull { it.userId }
            .distinct()
        val notifications = recipients.map { userId ->
            orderDraft(
                userId = userId,
                order = order,
                type = NotificationType.ORDER_PENDING_APPROVE,
                title = "새 주문이 승인을 기다리고 있어요",
                body = "'$eventName' ${order.orderName.orEmpty()} 주문(${order.orderNo.orEmpty()})이 접수되었습니다.",
                extra = mapOf("eventName" to eventName, "orderNo" to order.orderNo),
            )
        }
        return notificationBulkRepository.insertSkippingDuplicates(notifications)
    }

    /** 승인형 주문 승인 → 주문자. 결제형(결제 확정·무료 확정)은 저장 안 함 */
    @Transactional
    fun notifyOrderApproved(orderUuid: String): Int {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (order.orderMethod != OrderMethod.APPROVAL || V2OrderStatus.of(order) != V2OrderStatus.APPROVED) return 0
        val eventName = eventName(order)
        return notificationBulkRepository.insertSkippingDuplicates(
            listOf(
                orderDraft(
                    userId = order.userId!!,
                    order = order,
                    type = NotificationType.ORDER_APPROVED,
                    title = "티켓 주문이 승인되었습니다!",
                    body = "'$eventName' ${order.orderName.orEmpty()} 주문이 승인되어 티켓이 발급되었어요.",
                    extra = mapOf("eventName" to eventName, "orderNo" to order.orderNo),
                ),
            ),
        )
    }

    /**
     * 승인 대기 주문 거절 → 주문자 (v1 거절·v2 거절 공통, [V2OrderStatus.REFUSED] 기준). 승인 후 취소·환불은 저장 안 함.
     * 사유: v2 는 사유 종류 + 문구, v1 은 문구(cancel_reason, 없을 수 있음)
     */
    @Transactional
    fun notifyOrderRefused(orderUuid: String): Int {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (V2OrderStatus.of(order) != V2OrderStatus.REFUSED) return 0
        val eventName = eventName(order)
        val reason = order.cancelReason?.takeIf { it.isNotBlank() }
        return notificationBulkRepository.insertSkippingDuplicates(
            listOf(
                orderDraft(
                    userId = order.userId!!,
                    order = order,
                    type = NotificationType.ORDER_REFUSED,
                    title = "티켓 주문이 거절되었습니다",
                    body = "'$eventName' ${order.orderName.orEmpty()} 주문이 거절되었어요." + (reason?.let { " 사유: ${ellipsis(it, BODY_REASON_MAX_LENGTH)}" } ?: ""),
                    extra = mapOf(
                        "eventName" to eventName,
                        "orderNo" to order.orderNo,
                        "refuseReasonType" to order.refuseReasonType?.name,
                        "refuseReason" to reason,
                    ),
                ),
            ),
        )
    }

    // ===== 조회 / 읽음 =====

    fun querySlice(userId: Long, pageable: Pageable): Slice<Notification> =
        notificationRepository.findSliceByUserIdOrderByIdDesc(userId, pageable)

    fun countUnread(userId: Long): Long = notificationRepository.countByUserIdAndIsReadFalse(userId)

    /**
     * 읽음 처리 (멱등). 본인 알림만 바뀌고 남의 id·없는 id·이미 읽은 id 는 조용히 무시한다 (존재 여부를 드러내지 않음).
     * @return 이번에 읽음으로 바뀐 건수
     */
    @Transactional
    fun markRead(userId: Long, notificationIds: Collection<Long>?, all: Boolean): Int {
        val now = LocalDateTime.now()
        if (all) return notificationRepository.markAllRead(userId, now)
        if (notificationIds.isNullOrEmpty()) return 0
        return notificationRepository.markReadByIds(userId, notificationIds.toSet(), now)
    }

    // ===== 내부 =====

    private fun eventName(order: Order): String = eventAdaptor.findById(order.eventId!!).eventBasic?.name.orEmpty()

    private fun orderDraft(
        userId: Long,
        order: Order,
        type: NotificationType,
        title: String,
        body: String,
        extra: Map<String, String?>,
    ): Notification = draft(
        userId = userId,
        type = type,
        title = title,
        body = body,
        targetType = NotificationTargetType.ORDER,
        targetId = order.uuid!!,
        eventId = order.eventId,
        extra = extra,
        dedupKey = order.uuid!!,
    )

    private fun draft(
        userId: Long,
        type: NotificationType,
        title: String,
        body: String,
        targetType: NotificationTargetType,
        targetId: String,
        eventId: Long? = null,
        extra: Map<String, String?>,
        dedupKey: String,
    ): Notification = Notification(
        userId = userId,
        type = type,
        title = title.take(Notification.TITLE_MAX_LENGTH),
        body = body.take(Notification.BODY_MAX_LENGTH),
        targetType = targetType,
        targetId = targetId,
        eventId = eventId,
        // 값 단위로 자른 뒤 직렬화 (직렬화된 문자열을 자르면 JSON 이 깨진다). 값 상한 합이 컬럼 길이 안이다
        extra = OBJECT_MAPPER.writeValueAsString(
            extra.filterValues { it != null }.mapValues { ellipsis(it.value!!, EXTRA_VALUE_MAX_LENGTH) },
        ),
        dedupKey = dedupKey,
    )

    private fun ellipsis(text: String, max: Int): String = if (text.length <= max) text else text.take(max) + "…"

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()

        /** 본문에 넣는 거절 사유 최대 글자 수 (넘으면 … 붙임). extra.refuseReason 에는 최대 300자 */
        const val BODY_REASON_MAX_LENGTH = 100

        /**
         * extra 값 하나의 최대 글자 수 (넘으면 … 붙임). 긴 값은 거절 사유 하나뿐이고(공연명 30·호스트명 15자 등 나머지는 짧다),
         * 사유가 전부 이스케이프 문자여도(301자 → 최대 약 600자) 직렬화 결과가 컬럼 1,000자 안이다
         */
        const val EXTRA_VALUE_MAX_LENGTH = 300
    }
}
