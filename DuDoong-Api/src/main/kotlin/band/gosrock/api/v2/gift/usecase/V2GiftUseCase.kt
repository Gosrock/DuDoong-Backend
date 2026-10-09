package band.gosrock.api.v2.gift.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.gift.dto.V2GiftDirection
import band.gosrock.api.v2.gift.dto.response.V2GiftCreatedResponse
import band.gosrock.api.v2.gift.dto.response.V2GiftElement
import band.gosrock.api.v2.gift.dto.response.V2GiftEventResponse
import band.gosrock.api.v2.gift.dto.response.V2GiftLandingResponse
import band.gosrock.api.v2.gift.dto.response.V2GiftResultResponse
import band.gosrock.api.v2.gift.dto.response.V2GiftTicketResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.gift.domain.TicketGift
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.service.v2.V2GiftViewState
import band.gosrock.domain.domains.gift.service.v2.V2TicketGiftDomainService
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * G-1 ~ G-8 선물 (#719). 전이는 도메인 서비스의 `주문` 락 안에서 하고, 락 전에는 잠금 키(orderUuid)만 스칼라로 읽는다 (open-in-view).
 * 응답은 전이 트랜잭션이 커밋된 뒤 다시 읽는다. 로그에는 선물 토큰을 남기지 않는다 (링크를 가진 사람이 받으므로)
 */
@UseCase
class V2GiftUseCase(
    private val giftDomainService: V2TicketGiftDomainService,
    private val issuedTicketRepository: IssuedTicketRepository,
    private val eventAdaptor: EventAdaptor,
    private val userAdaptor: UserAdaptor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** G-1 */
    fun create(userId: Long, ticketUuid: String, memo: String?): V2GiftCreatedResponse {
        val gift = giftDomainService.create(giftDomainService.orderUuidOfTicket(ticketUuid), userId, ticketUuid, memo)
        log.info("[V2GiftUseCase] 선물 생성 userId={} giftId={} issuedTicketId={}", userId, gift.id, gift.issuedTicketId)
        return V2GiftCreatedResponse(giftId = gift.id!!, giftToken = gift.token, linkPath = linkPath(gift.token), memo = gift.memo)
    }

    /** G-2 */
    fun cancel(userId: Long, giftId: Long): V2GiftResultResponse {
        giftDomainService.cancel(giftDomainService.orderUuidOfMyGift(userId, giftId), userId, giftId)
        log.info("[V2GiftUseCase] 선물 회수 userId={} giftId={}", userId, giftId)
        return result(giftDomainService.querySentGift(userId, giftId), withMemo = true)
    }

    /** G-8 */
    fun changeMemo(userId: Long, giftId: Long, memo: String?): V2GiftResultResponse {
        giftDomainService.changeMemo(giftDomainService.orderUuidOfMyGift(userId, giftId), userId, giftId, memo)
        return result(giftDomainService.querySentGift(userId, giftId), withMemo = true)
    }

    /** G-4. 응답의 ticketUuid = 받은 사람의 새 QR */
    fun accept(userId: Long, token: String): V2GiftResultResponse {
        val gift = giftDomainService.accept(giftDomainService.orderUuidOfToken(token), userId, token)
        log.info("[V2GiftUseCase] 선물 수락 userId={} giftId={}", userId, gift.id)
        return result(gift, withMemo = false, ticketUuid = currentUuid(gift))
    }

    /** G-5 */
    fun reject(userId: Long, token: String): V2GiftResultResponse {
        giftDomainService.reject(giftDomainService.orderUuidOfToken(token), userId, token)
        val gift = giftDomainService.queryByToken(token)
        log.info("[V2GiftUseCase] 선물 거절 userId={} giftId={}", userId, gift.id)
        return result(gift, withMemo = false)
    }

    /** G-6. 티켓은 보낸 사람에게 돌아가고 uuid 가 바뀐다 (새 uuid 는 반환한 사람에게 주지 않는다) */
    fun returnTicket(userId: Long, ticketUuid: String): V2GiftResultResponse {
        val gift = giftDomainService.returnTicket(giftDomainService.orderUuidOfTicket(ticketUuid), userId, ticketUuid)
        log.info("[V2GiftUseCase] 선물 반환 userId={} giftId={}", userId, gift.id)
        return result(gift, withMemo = false)
    }

    /** G-3 선물 랜딩 (비로그인 = userId 0). 대기 중(만료·본인 링크 포함)이 아니면 상태만 */
    @Transactional(readOnly = true)
    fun landing(viewerId: Long, token: String): V2GiftLandingResponse {
        val gift = giftDomainService.queryByToken(token)
        // 삭제된 공연은 조회되지 않는다(@Where) — 운영 삭제로 취소된 선물도 404 가 아니라 CANCELED 화면을 보여 준다
        val event = eventAdaptor.findByIdOrNull(gift.eventId)
        val viewState = giftDomainService.viewStateOf(gift, event, viewerId, LocalDateTime.now())
        val pending = gift.status == TicketGiftStatus.PENDING && event != null
        val ticket = if (pending) issuedTicketRepository.findById(gift.issuedTicketId).orElse(null) else null
        return V2GiftLandingResponse(
            status = gift.status,
            viewState = viewState,
            isLoggedIn = viewerId != ANONYMOUS,
            isReceiver = viewState == V2GiftViewState.ALREADY_ACCEPTED && viewerId != ANONYMOUS && gift.receiverUserId == viewerId,
            giftId = gift.id.takeIf { viewerId != ANONYMOUS && gift.senderUserId == viewerId },
            senderName = if (pending) nameOf(gift.senderUserId)?.let(::maskName) else null,
            event = if (pending && event != null) {
                V2GiftEventResponse(
                    eventId = event.id!!,
                    name = event.eventBasic?.name,
                    posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
                    startAt = event.getStartAt(),
                    placeName = event.eventPlace?.placeName,
                )
            } else {
                null
            },
            ticket = ticket?.let { V2GiftTicketResponse(ticketName = it.itemInfo?.ticketName, unitPrice = it.itemInfo?.price?.longValue() ?: 0L) },
        )
    }

    /** G-7 보낸/받은 선물 내역 (최신 순). 메모·링크는 보낸 사람에게만 */
    @Transactional(readOnly = true)
    fun history(userId: Long, direction: V2GiftDirection, page: Int, size: Int): V2PageResponse<V2GiftElement> {
        val gifts = when (direction) {
            V2GiftDirection.SENT -> giftDomainService.sentPage(userId, page, size)
            V2GiftDirection.RECEIVED -> giftDomainService.receivedPage(userId, page, size)
        }
        val tickets = issuedTicketRepository.findAllById(gifts.content.map { it.issuedTicketId }.toSet()).associateBy { it.id }
        val events = gifts.content.map { it.eventId }.distinct().let { ids -> if (ids.isEmpty()) emptyMap() else eventAdaptor.findAllByIds(ids).associateBy { it.id } }
        val counterpartIds = gifts.content.mapNotNull { if (direction == V2GiftDirection.SENT) it.receiverUserId else it.senderUserId }.distinct()
        val names = if (counterpartIds.isEmpty()) emptyMap() else userAdaptor.findUserByIdIn(counterpartIds).associate { it.id to it.profile?.name }
        val now = LocalDateTime.now()
        return V2PageResponse.of(
            gifts.map { gift ->
                val sent = direction == V2GiftDirection.SENT
                val event = events[gift.eventId]
                val ticket: IssuedTicket? = tickets[gift.issuedTicketId]
                V2GiftElement(
                    giftId = gift.id!!,
                    status = gift.status,
                    cancelReason = gift.cancelReason,
                    isGiftExpired = gift.isPending() && event != null && giftDomainService.isEventEnded(event, now),
                    counterpartName = names[if (sent) gift.receiverUserId else gift.senderUserId],
                    memo = gift.memo.takeIf { sent },
                    giftToken = gift.token.takeIf { sent && gift.isPending() },
                    linkPath = linkPath(gift.token).takeIf { sent && gift.isPending() },
                    event = event?.let { eventResponse(it, now) },
                    ticketName = ticket?.itemInfo?.ticketName,
                    issuedTicketNo = ticket?.issuedTicketNo,
                    createdAt = gift.createdAt,
                    acceptedAt = gift.acceptedAt,
                    rejectedAt = gift.rejectedAt,
                    returnedAt = gift.returnedAt,
                    canceledAt = gift.canceledAt,
                )
            },
        )
    }

    private fun result(gift: TicketGift, withMemo: Boolean, ticketUuid: String? = null) = V2GiftResultResponse(
        giftId = gift.id!!,
        status = gift.status,
        cancelReason = gift.cancelReason,
        memo = gift.memo.takeIf { withMemo },
        ticketUuid = ticketUuid,
    )

    /** 커밋 뒤 티켓의 현재 uuid (락 트랜잭션에서 바뀐 값) */
    private fun currentUuid(gift: TicketGift): String? = issuedTicketRepository.findById(gift.issuedTicketId).orElse(null)?.uuid

    private fun nameOf(userId: Long): String? = runCatching { userAdaptor.queryUser(userId).profile?.name }.getOrNull()

    private fun eventResponse(event: Event, now: LocalDateTime) = V2MyOrderEventResponse(
        eventId = event.id!!,
        name = event.eventBasic?.name,
        posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
        startAt = event.getStartAt(),
        displayStatus = V2EventDisplayRule.of(event, now),
    )

    companion object {
        /** 비로그인 (SecurityUtils: 익명 = 0) */
        private const val ANONYMOUS = 0L

        /** 공개 랜딩용 이름 가림: 첫 글자 + 가운데 * + 끝 글자 (2자면 첫 글자 + *, 1자면 *) — 김*수, 김*, 남**희 */
        fun maskName(name: String): String {
            // 코드 포인트 단위 (이모지 등 보조 문자를 반으로 자르지 않는다)
            val cps = name.codePoints().toArray()
            fun str(cp: Int) = String(Character.toChars(cp))
            return when {
                cps.size <= 1 -> "*"
                cps.size == 2 -> str(cps.first()) + "*"
                else -> str(cps.first()) + "*".repeat(cps.size - 2) + str(cps.last())
            }
        }

        /** 선물 랜딩 프론트 경로 (8-4 A9, 프론트 협의 전 초안). origin 은 프론트가 붙인다 */
        fun linkPath(token: String): String = "/gifts/$token"
    }
}
