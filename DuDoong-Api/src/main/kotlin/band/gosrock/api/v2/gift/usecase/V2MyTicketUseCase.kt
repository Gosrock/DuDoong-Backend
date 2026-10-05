package band.gosrock.api.v2.gift.usecase

import band.gosrock.api.v2.gift.dto.V2MyTicketState
import band.gosrock.api.v2.gift.dto.response.V2MyTicketDetailResponse
import band.gosrock.api.v2.gift.dto.response.V2MyTicketElement
import band.gosrock.api.v2.gift.dto.response.V2MyTicketGroupResponse
import band.gosrock.api.v2.gift.dto.response.V2MyTicketsResponse
import band.gosrock.api.v2.gift.dto.response.V2NewApprovedResponse
import band.gosrock.api.v2.gift.dto.response.V2TicketGiftInfoResponse
import band.gosrock.api.v2.operation.usecase.V2OperationMapper
import band.gosrock.api.v2.order.dto.response.V2MyOrderEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.gift.domain.TicketGift
import band.gosrock.domain.domains.gift.exception.GiftNotFoundException
import band.gosrock.domain.domains.gift.service.v2.V2GiftState
import band.gosrock.domain.domains.gift.service.v2.V2MyTicketQuery
import band.gosrock.domain.domains.gift.service.v2.V2TicketGiftDomainService
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.service.v2.V2MyOrderStatus
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

/**
 * T-1 내 티켓 / T-2 티켓 상세 + 입장 QR (#719).
 * 범위: 지금 내가 소유한 티켓 + 내가 선물해 수락된 티켓('선물 완료', uuid·QR 없음 — DEC-026 #3) + 티켓 없는 승인 대기·거절 주문.
 * 지난 공연·취소된 티켓도 보인다 (8-4 A7: 페이지 없음, 정렬은 공연 임박순 하나)
 */
@UseCase
class V2MyTicketUseCase(
    private val myTicketQuery: V2MyTicketQuery,
    private val giftDomainService: V2TicketGiftDomainService,
    private val issuedTicketRepository: IssuedTicketRepository,
    private val eventAdaptor: EventAdaptor,
    private val userAdaptor: UserAdaptor,
    private val mapper: V2OperationMapper,
    private val notificationDomainService: V2NotificationDomainService,
) {

    @Transactional(readOnly = true)
    fun tickets(userId: Long): V2MyTicketsResponse {
        val now = LocalDateTime.now()
        val owned = myTicketQuery.findOwnedTickets(userId)
        val sentGiftTicketIds = giftDomainService.acceptedSentGifts(userId).map { it.issuedTicketId }.toSet() - owned.mapNotNull { it.id }.toSet()
        val candidates = owned + myTicketQuery.findTicketsByIds(sentGiftTicketIds)
        val latest = giftDomainService.latestGiftsOf(candidates.mapNotNull { it.id })
        val orders = myTicketQuery.findOrdersByUuids(candidates.mapNotNull { it.orderUuid }.toSet()).associateBy { it.uuid!! }
        val rows = candidates.mapNotNull { ticket ->
            val order = orders[ticket.orderUuid] ?: return@mapNotNull null
            val giftState = giftDomainService.giftStateOf(ticket, order.userId, latest[ticket.id], userId)
            // 남의 티켓은 내가 보내 수락된 것('선물 완료')만
            if (ticket.getUserId() != userId && giftState != V2GiftState.SENT) return@mapNotNull null
            Triple(ticket, order, giftState)
        }
        val waiting = myTicketQuery.findWaitingOrRefusedOrders(userId).filter { it.uuid !in orders }
        val events = eventsOf((rows.map { it.second } + waiting).mapNotNull { it.eventId })

        val ticketGroups = rows.groupBy { it.second.uuid!! }.map { (orderUuid, items) ->
            val order = items.first().second
            val event = events[order.eventId]
            val elements = items.sortedBy { it.first.id }.map { (ticket, _, giftState) ->
                val expired = giftState == V2GiftState.PENDING && event != null && giftDomainService.isEventEnded(event, now)
                V2MyTicketElement(
                    ticketUuid = ticket.uuid.takeIf { giftState != V2GiftState.SENT },
                    issuedTicketNo = ticket.issuedTicketNo,
                    ticketName = ticket.itemInfo?.ticketName,
                    optionPrice = ticket.sumOptionPrice().longValue(),
                    state = stateOf(ticket, order, giftState, expired),
                    entrance = V2EntranceState.of(ticket.issuedTicketStatus),
                    enteredAt = ticket.enteredAt,
                    giftState = giftState,
                    isGiftExpired = expired,
                    isReceived = giftState == V2GiftState.RECEIVED,
                    giftId = latest[ticket.id]?.id?.takeIf { giftState != V2GiftState.NONE },
                )
            }
            Group(order, event, groupResponse(order, event, userId, elements.groupingBy { it.state }.eachCount(), elements, now))
        }
        val waitingGroups = waiting.map { order ->
            val event = events[order.eventId]
            val state = if (V2OrderStatus.of(order) == V2OrderStatus.REFUSED) V2MyTicketState.REFUSED else V2MyTicketState.PENDING_APPROVE
            Group(order, event, groupResponse(order, event, userId, mapOf(state to order.getTotalQuantity().toInt()), emptyList(), now))
        }
        return V2MyTicketsResponse((ticketGroups + waitingGroups).sortedWith(upcomingOrder(now)).map { it.response })
    }

    /** T-3 공지 바: 안 읽은 승인 알림 중 주문이 지금도 승인 상태인 내 주문 (승인 뒤 취소된 주문은 빠짐) */
    @Transactional(readOnly = true)
    fun newApproved(userId: Long): V2NewApprovedResponse {
        val uuids = notificationDomainService.unreadApprovedOrderUuids(userId)
        val approved = myTicketQuery.findOrdersByUuids(uuids)
            .filter { it.userId == userId && V2OrderStatus.of(it) == V2OrderStatus.APPROVED }
            .mapNotNull { it.uuid }.toSet()
        val orderUuids = uuids.filter { it in approved }
        return V2NewApprovedResponse(hasNew = orderUuids.isNotEmpty(), orderUuids = orderUuids)
    }

    /** T-2. 지금 내가 소유한 티켓만 (남의 티켓·없는 티켓·선물로 바뀐 옛 uuid 는 IssuedTicket_404_1) */
    @Transactional(readOnly = true)
    fun ticket(userId: Long, ticketUuid: String): V2MyTicketDetailResponse {
        val ticket = issuedTicketRepository.findByUuid(ticketUuid).orElse(null)?.takeIf { it.getUserId() == userId }
            ?: throw IssuedTicketNotFoundException.EXCEPTION
        val order = myTicketQuery.findOrdersByUuids(listOf(ticket.orderUuid!!)).single()
        // 공지 바 해제 (A8): 내 주문의 티켓을 열면 그 주문의 승인 알림을 읽음 처리 (새 트랜잭션, 멱등)
        if (order.userId == userId) notificationDomainService.markOrderApprovedRead(userId, order.uuid!!)
        return detail(ticket, order, userId)
    }

    /**
     * 보낸 사람의 '선물 완료' 티켓 상세 (T-1 SENT 행의 giftId 로 연다, #719 리뷰). 티켓 정보·선물 상태만 — uuid·QR 없음 (받은 사람의 QR).
     * 내가 보낸 선물이 아니거나, 그 티켓의 지금 선물 상태가 '선물 완료'가 아니면(반환·회수로 돌아온 티켓은 T-2) Gift_404_1
     */
    @Transactional(readOnly = true)
    fun sentTicket(userId: Long, giftId: Long): V2MyTicketDetailResponse {
        val gift = giftDomainService.querySentGift(userId, giftId)
        val ticket = myTicketQuery.findTicketsByIds(listOf(gift.issuedTicketId)).single()
        val order = myTicketQuery.findOrdersByUuids(listOf(ticket.orderUuid!!)).single()
        val latest = giftDomainService.latestGiftsOf(listOf(ticket.id!!))[ticket.id]
        if (latest?.id != gift.id || giftDomainService.giftStateOf(ticket, order.userId, latest, userId) != V2GiftState.SENT) {
            throw GiftNotFoundException.EXCEPTION
        }
        return detail(ticket, order, userId)
    }

    private fun detail(ticket: IssuedTicket, order: Order, userId: Long): V2MyTicketDetailResponse {
        val now = LocalDateTime.now()
        val event = eventAdaptor.findById(ticket.eventId!!)
        val latest = giftDomainService.latestGiftsOf(listOf(ticket.id!!))[ticket.id]
        val giftState = giftDomainService.giftStateOf(ticket, order.userId, latest, userId)
        val expired = giftState == V2GiftState.PENDING && giftDomainService.isEventEnded(event, now)
        val myOrder = order.userId == userId
        val sent = giftState == V2GiftState.SENT
        return V2MyTicketDetailResponse(
            ticketUuid = ticket.uuid.takeIf { !sent },
            issuedTicketNo = ticket.issuedTicketNo,
            ticketName = ticket.itemInfo?.ticketName,
            ticketPrice = ticket.itemInfo?.price?.longValue() ?: 0L,
            optionAnswers = mapper.ticketOptionAnswers(ticket),
            optionPrice = ticket.sumOptionPrice().longValue(),
            state = stateOf(ticket, order, giftState, expired),
            entrance = V2EntranceState.of(ticket.issuedTicketStatus),
            enteredAt = ticket.enteredAt,
            giftState = giftState,
            isGiftExpired = expired,
            isReceived = giftState == V2GiftState.RECEIVED,
            qrValue = ticket.uuid.takeIf { !sent && giftState != V2GiftState.PENDING && !ticket.issuedTicketStatus.isCanceled() },
            event = eventResponse(event, now),
            orderUuid = order.uuid.takeIf { myOrder },
            orderNo = order.orderNo.takeIf { myOrder },
            gift = latest?.let { giftInfo(it, giftState) },
            canGift = !sent && giftDomainService.giftBlocker(ticket, order, event, latest, userId, now) == null,
            canReturn = giftState == V2GiftState.RECEIVED && giftDomainService.returnBlocker(ticket, latest, event, userId, now) == null,
        )
    }

    private fun giftInfo(gift: TicketGift, giftState: V2GiftState): V2TicketGiftInfoResponse? = when (giftState) {
        V2GiftState.PENDING -> V2TicketGiftInfoResponse(
            giftId = gift.id!!,
            status = gift.status,
            giftToken = gift.token,
            linkPath = V2GiftUseCase.linkPath(gift.token),
            memo = gift.memo,
            receiverName = null,
            senderName = null,
            createdAt = gift.createdAt,
            acceptedAt = null,
        )
        V2GiftState.RECEIVED -> V2TicketGiftInfoResponse(
            giftId = gift.id!!,
            status = gift.status,
            giftToken = null,
            linkPath = null,
            memo = null,
            receiverName = null,
            senderName = runCatching { userAdaptor.queryUser(gift.senderUserId).profile?.name }.getOrNull(),
            createdAt = gift.createdAt,
            acceptedAt = gift.acceptedAt,
        )
        V2GiftState.SENT -> V2TicketGiftInfoResponse(
            giftId = gift.id!!,
            status = gift.status,
            giftToken = null,
            linkPath = null,
            memo = gift.memo,
            receiverName = gift.receiverUserId?.let { id -> runCatching { userAdaptor.queryUser(id).profile?.name }.getOrNull() },
            senderName = null,
            createdAt = gift.createdAt,
            acceptedAt = gift.acceptedAt,
        )
        else -> null
    }

    private data class Group(val order: Order, val event: Event?, val response: V2MyTicketGroupResponse)

    private fun groupResponse(
        order: Order,
        event: Event?,
        userId: Long,
        counts: Map<V2MyTicketState, Int>,
        tickets: List<V2MyTicketElement>,
        now: LocalDateTime,
    ): V2MyTicketGroupResponse {
        val myOrder = order.userId == userId
        val refused = V2OrderStatus.of(order) == V2OrderStatus.REFUSED
        return V2MyTicketGroupResponse(
            orderUuid = order.uuid.takeIf { myOrder },
            orderNo = order.orderNo.takeIf { myOrder },
            isMyOrder = myOrder,
            orderStatus = V2MyOrderStatus.of(order).takeIf { myOrder },
            refuseReasonType = order.refuseReasonType.takeIf { myOrder && refused },
            refuseReason = order.cancelReason.takeIf { myOrder && refused },
            event = event?.let { eventResponse(it, now) },
            ticketName = tickets.firstOrNull()?.ticketName ?: order.orderName,
            counts = counts,
            tickets = tickets,
        )
    }

    /** 화면 상태 판정 순서: 취소·환불 → 입장 완료 → 선물 완료 → 선물 대기(만료) → 받은 티켓 → 승인 완료 */
    private fun stateOf(ticket: IssuedTicket, order: Order, giftState: V2GiftState, expired: Boolean): V2MyTicketState = when {
        ticket.issuedTicketStatus.isCanceled() -> when {
            // 받은 티켓은 원 주문의 환불 상태를 보이지 않는다 (호스트·운영 취소 = 취소)
            giftState == V2GiftState.RECEIVED || V2MyOrderStatus.of(order) != V2MyOrderStatus.REFUNDED -> V2MyTicketState.CANCELED
            order.refundStatus == RefundStatus.REFUND_COMPLETED -> V2MyTicketState.REFUNDED
            else -> V2MyTicketState.REFUND_REQUESTED
        }
        ticket.issuedTicketStatus.isAfterEntrance() -> V2MyTicketState.ENTERED
        giftState == V2GiftState.SENT -> V2MyTicketState.GIFT_SENT
        giftState == V2GiftState.PENDING -> if (expired) V2MyTicketState.GIFT_EXPIRED else V2MyTicketState.GIFT_PENDING
        giftState == V2GiftState.RECEIVED -> V2MyTicketState.RECEIVED
        else -> V2MyTicketState.APPROVED
    }

    /** 공연 임박순: 종료 전 공연(시작 임박순, 진행 중이 앞) → 지난 공연(최근 시작 순). 같으면 최근 주문이 앞 */
    private fun upcomingOrder(now: LocalDateTime): Comparator<Group> {
        fun ended(g: Group) = g.event == null || giftDomainService.isEventEnded(g.event, now)
        return compareBy<Group> { ended(it) }
            .thenComparator { a, b ->
                val sa = a.event?.getStartAt() ?: LocalDateTime.MIN
                val sb = b.event?.getStartAt() ?: LocalDateTime.MIN
                if (ended(a)) sb.compareTo(sa) else sa.compareTo(sb)
            }
            .thenByDescending { it.order.id }
    }

    private fun eventsOf(eventIds: Collection<Long>): Map<Long?, Event> =
        if (eventIds.isEmpty()) emptyMap() else eventAdaptor.findAllByIds(eventIds.distinct()).associateBy { it.id }

    private fun eventResponse(event: Event, now: LocalDateTime) = V2MyOrderEventResponse(
        eventId = event.id!!,
        name = event.eventBasic?.name,
        posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
        startAt = event.getStartAt(),
        displayStatus = V2EventDisplayRule.of(event, now),
    )
}
