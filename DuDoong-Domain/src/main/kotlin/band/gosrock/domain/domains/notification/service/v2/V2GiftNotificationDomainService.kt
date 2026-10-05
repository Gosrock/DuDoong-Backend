package band.gosrock.domain.domains.notification.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.gift.domain.TicketGift
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.repository.TicketGiftRepository
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.notification.domain.Notification
import band.gosrock.domain.domains.notification.domain.NotificationTargetType
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationBulkRepository
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.transaction.annotation.Transactional

/**
 * 선물 알림 저장 (#719, 11 문서 8-2 알림 표, Figma 알림함 '티켓선물 안내'). 저장 방식은 [V2NotificationDomainService] 와 같다:
 * 커밋 후 비동기 핸들러가 부르고, 커밋된 데이터를 다시 읽어 수신자·문구를 정한다. 중복은 uk(type, dedup_key = gift:{giftId}, user_id) 로 건너뛴다.
 *
 * 보낸 사람 — 생성·수락·거절·반환, 받은 사람 — 수락·선물받은 티켓 취소. 보낸 사람 회수(G-2)는 알림 없음.
 * 대기 중 선물이 주문 취소로 무효가 될 때 보낸 사람은 주문 취소 알림으로 안다 (Backend #726)
 */
@DomainService
@Transactional(readOnly = true)
class V2GiftNotificationDomainService(
    private val ticketGiftRepository: TicketGiftRepository,
    private val issuedTicketRepository: IssuedTicketRepository,
    private val notificationBulkRepository: NotificationBulkRepository,
    private val eventAdaptor: EventAdaptor,
    private val userAdaptor: UserAdaptor,
) {

    /** G-1 → 보낸 사람 "○○ 티켓을 선물 중입니다" */
    @Transactional
    fun notifyGiftSent(giftId: Long): Int {
        val gift = ticketGiftRepository.findById(giftId).orElse(null) ?: return 0
        val s = subject(gift)
        return save(draft(gift, gift.senderUserId, NotificationType.GIFT_SENT, "티켓을 선물하고 있어요", "${s.ticket} 티켓을 선물 중입니다.", s))
    }

    /** G-4 → 보낸 사람 "△△ 님이 ○○ 티켓(T…)을 수령했습니다", 받은 사람 "□□ 님에게 받은 ○○ 티켓을 수령하였습니다" */
    @Transactional
    fun notifyGiftAccepted(giftId: Long): Int {
        val gift = ticketGiftRepository.findById(giftId).orElse(null)?.takeIf { it.acceptedAt != null } ?: return 0
        val receiverId = gift.receiverUserId ?: return 0
        val s = subject(gift)
        val receiverName = nameOf(receiverId)
        val senderName = nameOf(gift.senderUserId)
        return save(
            draft(gift, gift.senderUserId, NotificationType.GIFT_ACCEPTED, "선물한 티켓을 받았어요", "$receiverName 님이 ${s.ticket} 티켓(${s.ticketNo})을 수령했습니다.", s),
            draft(gift, receiverId, NotificationType.GIFT_RECEIVED, "선물받은 티켓이 도착했어요", "$senderName 님에게 받은 ${s.ticket} 티켓을 수령하였습니다.", s),
        )
    }

    /** G-5 → 보낸 사람 "○○ 티켓(T…) 선물이 반송되었습니다" */
    @Transactional
    fun notifyGiftRejected(giftId: Long): Int {
        val gift = ticketGiftRepository.findById(giftId).orElse(null)?.takeIf { it.rejectedAt != null } ?: return 0
        val s = subject(gift)
        return save(draft(gift, gift.senderUserId, NotificationType.GIFT_REJECTED, "선물이 반송되었어요", "${s.ticket} 티켓(${s.ticketNo}) 선물이 반송되었습니다.", s))
    }

    /** G-6 → 보낸 사람 "△△ 님이 ○○ 티켓(T…)을 돌려보냈습니다" */
    @Transactional
    fun notifyGiftReturned(giftId: Long): Int {
        val gift = ticketGiftRepository.findById(giftId).orElse(null)?.takeIf { it.returnedAt != null } ?: return 0
        val receiverName = gift.receiverUserId?.let { nameOf(it) }.orEmpty()
        val s = subject(gift)
        return save(draft(gift, gift.senderUserId, NotificationType.GIFT_RETURNED, "선물한 티켓이 돌아왔어요", "$receiverName 님이 ${s.ticket} 티켓(${s.ticketNo})을 돌려보냈습니다.", s))
    }

    /**
     * 원 주문 취소로 선물받은 티켓이 취소됨 → 받은 사람 "선물받은 ○○ 티켓이 취소되었습니다" (DEC-026 #8).
     * 대상: 그 주문의 티켓 중 가장 최근 선물이 ACCEPTED 이고, 지금 받은 사람이 소유한 채 취소된 티켓. 선물이 없는 주문은 0
     */
    @Transactional
    fun notifyGiftTicketsCanceled(orderUuid: String): Int {
        val tickets = issuedTicketRepository.findAllByOrderUuid(orderUuid).filter { it.issuedTicketStatus.isCanceled() }
        if (tickets.isEmpty()) return 0
        val latest = ticketGiftRepository.findAllByIssuedTicketIdIn(tickets.mapNotNull { it.id })
            .groupBy { it.issuedTicketId }.mapValues { (_, gifts) -> gifts.maxBy { it.id!! } }
        val drafts = tickets.mapNotNull { ticket ->
            val gift = latest[ticket.id]?.takeIf { it.status == TicketGiftStatus.ACCEPTED && it.receiverUserId == ticket.getUserId() } ?: return@mapNotNull null
            val s = subject(gift)
            draft(gift, gift.receiverUserId!!, NotificationType.GIFT_TICKET_CANCELED, "선물받은 티켓이 취소되었어요", "선물받은 ${s.ticket} 티켓이 취소되었습니다.", s)
        }
        return save(*drafts.toTypedArray())
    }

    // ===== 내부 =====

    private data class Subject(val eventName: String, val ticketName: String, val ticketNo: String) {
        /** 본문의 '○○' = '공연명' 티켓명 */
        val ticket: String get() = "'$eventName' $ticketName"
    }

    private fun subject(gift: TicketGift): Subject {
        val ticket = issuedTicketRepository.findById(gift.issuedTicketId).orElse(null)
        val eventName = runCatching { eventAdaptor.findById(gift.eventId).eventBasic?.name }.getOrNull().orEmpty()
        return Subject(eventName, ticket?.itemInfo?.ticketName.orEmpty(), ticket?.issuedTicketNo.orEmpty())
    }

    private fun nameOf(userId: Long): String = runCatching { userAdaptor.queryUser(userId).profile?.name }.getOrNull().orEmpty()

    private fun draft(gift: TicketGift, userId: Long, type: NotificationType, title: String, body: String, s: Subject): Notification =
        Notification(
            userId = userId,
            type = type,
            title = title.take(Notification.TITLE_MAX_LENGTH),
            body = body.take(Notification.BODY_MAX_LENGTH),
            targetType = NotificationTargetType.GIFT,
            targetId = gift.id!!.toString(),
            eventId = gift.eventId,
            // 값이 모두 짧다 (공연명 30·티켓명·티켓 번호) — 직렬화 결과가 extra 컬럼 1,000자 안
            extra = OBJECT_MAPPER.writeValueAsString(mapOf("eventName" to s.eventName, "ticketName" to s.ticketName, "issuedTicketNo" to s.ticketNo)),
            dedupKey = "gift:${gift.id}",
        )

    private fun save(vararg notifications: Notification): Int =
        if (notifications.isEmpty()) 0 else notificationBulkRepository.insertSkippingDuplicates(notifications.toList())

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()
    }
}
