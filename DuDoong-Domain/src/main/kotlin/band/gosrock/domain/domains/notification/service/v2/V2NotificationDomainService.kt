package band.gosrock.domain.domains.notification.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
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
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import band.gosrock.domain.domains.order.service.v2.V2UserOrderDomainService
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
    private val v2UserOrderDomainService: V2UserOrderDomainService,
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
        val notifications = managerIds(host).map { userId ->
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
                    body = withRefundAccountGuide(
                        order,
                        "'$eventName' ${order.orderName.orEmpty()} 주문이 거절되었어요." + (reason?.let { " 사유: ${ellipsis(it, BODY_REASON_MAX_LENGTH)}" } ?: ""),
                    ),
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

    /**
     * 사용자 취소·환불 요청(REFUND) → 호스트 활성 마스터·매니저 (#718). 환불 요청이 걸렸으면 ORDER_REFUND_REQUESTED, 아니면(무료) ORDER_CANCELED_BY_USER.
     * v1 사용자 환불도 대상이다. 카드(PG) 결제 주문은 결제 취소가 자동이라 저장 안 함 (핸들러 condition 에서도 거름)
     */
    @Transactional
    fun notifyOrderWithdrawnByUser(orderUuid: String): Int {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (order.orderStatus != OrderStatus.REFUND) return 0
        if (order.orderMethod == OrderMethod.PAYMENT && order.isNeedPaid()) return 0
        // 돌려줄 돈이 있을 때만 환불 요청 알림 (무료·0원 주문은 v1 경로에서 환불 요청 상태가 걸려도 취소 알림)
        val refund = order.refundStatus != RefundStatus.NONE && order.getTotalPaymentPrice().isGreaterThan(Money.ZERO)
        val event = eventAdaptor.findById(order.eventId!!)
        val host = hostAdaptor.findById(event.hostId!!)
        val eventName = event.eventBasic?.name.orEmpty()
        val subject = "'$eventName' ${order.orderName.orEmpty()} 주문(${order.orderNo.orEmpty()})"
        val notifications = managerIds(host).map { userId ->
            orderDraft(
                userId = userId,
                order = order,
                type = if (refund) NotificationType.ORDER_REFUND_REQUESTED else NotificationType.ORDER_CANCELED_BY_USER,
                title = if (refund) "주문자가 환불을 요청했어요" else "주문자가 주문을 취소했어요",
                body = if (refund) "${subject}에 환불 요청이 들어왔습니다. 환불 계좌로 송금한 뒤 환불 완료로 처리해 주세요." else "${subject}이 주문자에 의해 취소되었습니다.",
                extra = mapOf("eventName" to eventName, "orderNo" to order.orderNo),
            )
        }
        return notificationBulkRepository.insertSkippingDuplicates(notifications)
    }

    /**
     * 승인 완료 주문의 호스트 취소 → 주문자 (#726, v1 취소·v2 R-5·운영 어드민 공통, [V2OrderStatus.CANCELED] 중 CANCELED 상태). 거절·사용자 취소(REFUND)는 저장 안 함.
     * 결제 방식 무관 — 승인형·무료 선착순·카드 결제 모두 (사용자 결정 2026-10-05). 사유는 호스트가 입력한 cancel_reason (없을 수 있음)
     */
    @Transactional
    fun notifyOrderCanceledByHost(orderUuid: String): Int {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (order.orderStatus != OrderStatus.CANCELED || V2OrderStatus.of(order) != V2OrderStatus.CANCELED) return 0
        val eventName = eventName(order)
        val reason = order.cancelReason?.takeIf { it.isNotBlank() }
        return notificationBulkRepository.insertSkippingDuplicates(
            listOf(
                orderDraft(
                    userId = order.userId!!,
                    order = order,
                    type = NotificationType.ORDER_CANCELED_BY_HOST,
                    title = "티켓 주문이 취소되었습니다",
                    body = withRefundAccountGuide(
                        order,
                        "'$eventName' ${order.orderName.orEmpty()} 주문이 호스트에 의해 취소되었어요." + (reason?.let { " 사유: ${ellipsis(it, BODY_REASON_MAX_LENGTH)}" } ?: ""),
                    ),
                    extra = mapOf("eventName" to eventName, "orderNo" to order.orderNo, "cancelReason" to reason),
                ),
            ),
        )
    }

    /**
     * 환불 완료 → 주문자 (#726, v1 환불 완료·v2 F-2·운영 어드민 공통). 거절·취소·사용자 철회(CANCELED·REFUND)된 주문 + 환불 완료 상태 + 돌려준 돈이 있는 주문만.
     * v1·운영 어드민은 상태 검사 없이 환불 완료로 바꿀 수 있어, 승인 완료 주문 등에 잘못 처리된 경우는 저장하지 않는다.
     * 카드(PG) 결제 주문은 결제 취소로 자동 환불되므로 저장 안 함 (핸들러 condition 에서도 거름)
     */
    @Transactional
    fun notifyOrderRefundCompleted(orderUuid: String): Int {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (order.orderStatus !in REFUNDABLE_STATUSES || order.refundStatus != RefundStatus.REFUND_COMPLETED) return 0
        if (!order.getTotalPaymentPrice().isGreaterThan(Money.ZERO)) return 0
        if (order.orderMethod == OrderMethod.PAYMENT) return 0
        val eventName = eventName(order)
        return notificationBulkRepository.insertSkippingDuplicates(
            listOf(
                orderDraft(
                    userId = order.userId!!,
                    order = order,
                    type = NotificationType.ORDER_REFUND_COMPLETED,
                    title = "환불이 완료되었습니다",
                    body = "'$eventName' ${order.orderName.orEmpty()} 주문(${order.orderNo.orEmpty()})의 환불이 완료되었어요.",
                    extra = mapOf("eventName" to eventName, "orderNo" to order.orderNo),
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

    // ===== 티켓탭 공지 바 (T-3, #719) =====

    /** 안 읽은 승인 알림의 주문 uuid (최신 알림 순, 중복 제거). 알림을 읽으면(N-3) 빠진다 */
    fun unreadApprovedOrderUuids(userId: Long): List<String> =
        notificationRepository.findAllByUserIdAndTypeAndIsReadFalseOrderByIdDesc(userId, NotificationType.ORDER_APPROVED).map { it.targetId }.distinct()

    /** 이 주문의 안 읽은 승인 알림이 있는지 (T-2 가 읽음 처리 이벤트를 낼지 정하는 사전 확인 — 없으면 UPDATE 를 하지 않는다) */
    fun hasUnreadOrderApproved(userId: Long, orderUuid: String): Boolean =
        notificationRepository.existsByUserIdAndTypeAndTargetIdAndIsReadFalse(userId, NotificationType.ORDER_APPROVED, orderUuid)

    /** 승인된 주문의 티켓을 열면(T-2·G-7a) 그 주문의 승인 알림을 읽음으로 — 공지 바 해제 (8-4 A8). 멱등. 조회가 끝난 뒤 알림 전용 풀에서 부른다 */
    @Transactional
    fun markOrderApprovedRead(userId: Long, orderUuid: String): Int =
        notificationRepository.markReadByTarget(userId, NotificationType.ORDER_APPROVED, orderUuid, LocalDateTime.now())

    // ===== 내부 =====

    /** 호스트의 활성 마스터·매니저 (일반 멤버·초대 대기 제외) */
    private fun managerIds(host: Host): List<Long> = host.hostUsers
        .filter { it.active && (it.role == HostRole.MASTER || it.role == HostRole.MANAGER) }
        .mapNotNull { it.userId }
        .distinct()

    /**
     * 거절·호스트 취소 알림의 환불 계좌 입력 안내 (#728 결정): 환불 계좌를 받을 수 있는 주문(환불 요청 중인 유료 계좌이체)이고 아직 계좌가 없을 때만.
     * 무료·0원·카드 결제·이미 계좌가 있는 주문은 붙이지 않는다. 알림을 누르면 주문상세(target ORDER)에서 입력한다
     */
    private fun withRefundAccountGuide(order: Order, body: String): String {
        if (!v2UserOrderDomainService.canEditRefundAccount(order) || v2UserOrderDomainService.refundAccountOf(order.id!!) != null) return body
        // 사유 문구 뒤에 바로 붙으면 이어 읽히므로 문장을 끊는다
        return body + (if (body.endsWith(".")) " " else ". ") + REFUND_ACCOUNT_GUIDE
    }

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

        /** 거절·호스트 취소 알림에 붙이는 환불 계좌 입력 안내 (#728) */
        const val REFUND_ACCOUNT_GUIDE = "주문상세에서 환불 계좌를 입력해 주세요."

        /** 환불 완료 알림 대상 주문 상태: 거절·호스트 취소(CANCELED), 사용자 취소·환불 요청(REFUND) */
        private val REFUNDABLE_STATUSES = setOf(OrderStatus.CANCELED, OrderStatus.REFUND)

        /** 본문에 넣는 거절 사유 최대 글자 수 (넘으면 … 붙임). extra.refuseReason 에는 최대 300자 */
        const val BODY_REASON_MAX_LENGTH = 100

        /**
         * extra 값 하나의 최대 글자 수 (넘으면 … 붙임). 긴 값은 거절 사유 하나뿐이고(공연명 30·호스트명 15자 등 나머지는 짧다),
         * 사유가 전부 이스케이프 문자여도(301자 → 최대 약 600자) 직렬화 결과가 컬럼 1,000자 안이다
         */
        const val EXTRA_VALUE_MAX_LENGTH = 300
    }
}
