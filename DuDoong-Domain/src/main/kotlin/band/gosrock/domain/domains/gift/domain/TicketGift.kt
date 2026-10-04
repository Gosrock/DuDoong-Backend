package band.gosrock.domain.domains.gift.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 티켓 선물 링크 (v2 전용, #719, V008). 받는 사람을 미리 고르지 않는 링크 방식 (DEC-022).
 * 한 티켓에 기록이 여러 개일 수 있고(반환·거절·취소 뒤 재선물) 가장 최근 기록으로 티켓의 선물 상태를 판정한다.
 * 전이는 `service.v2` 에서 `주문:{orderUuid}` 락 → 티켓 행 잠금 안에서만 한다 (상태 검증은 서비스, 엔티티는 전이 기록만).
 *
 * - token: 추측 불가 랜덤(32바이트 SecureRandom, base64url 43자). PENDING 일 때만 수락·거절에 쓰인다
 * - receiver_user_id: 수락·거절한 사람 (대기·취소면 null)
 * - order_uuid·event_id: 티켓에서 복사한 값 (바뀌지 않음). 주문·공연 단위 연쇄 처리와 잠금 키 조회용
 */
@Table(
    name = "tbl_ticket_gift",
    indexes = [
        Index(name = "uk_ticket_gift_token", columnList = "token", unique = true),
        Index(name = "idx_ticket_gift_issued_ticket_id", columnList = "issued_ticket_id, ticket_gift_id"),
        Index(name = "idx_ticket_gift_sender_user_id", columnList = "sender_user_id, ticket_gift_id"),
        Index(name = "idx_ticket_gift_receiver_user_id", columnList = "receiver_user_id, ticket_gift_id"),
        Index(name = "idx_ticket_gift_event_id_status", columnList = "event_id, status"),
    ],
)
@Entity
class TicketGift(
    @Column(name = "issued_ticket_id", nullable = false)
    val issuedTicketId: Long,
    @Column(name = "order_uuid", nullable = false, length = 64)
    val orderUuid: String,
    @Column(name = "event_id", nullable = false)
    val eventId: Long,
    @Column(name = "sender_user_id", nullable = false)
    val senderUserId: Long,
    @Column(name = "token", nullable = false, length = TOKEN_MAX_LENGTH)
    val token: String,
    memo: String?,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_gift_id")
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: TicketGiftStatus = TicketGiftStatus.PENDING
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    var cancelReason: TicketGiftCancelReason? = null
        protected set

    @Column(name = "receiver_user_id")
    var receiverUserId: Long? = null
        protected set

    @Column(name = "memo", length = MEMO_MAX_LENGTH)
    var memo: String? = memo
        protected set

    @Column(name = "accepted_at")
    var acceptedAt: LocalDateTime? = null
        protected set

    @Column(name = "rejected_at")
    var rejectedAt: LocalDateTime? = null
        protected set

    @Column(name = "returned_at")
    var returnedAt: LocalDateTime? = null
        protected set

    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null
        protected set

    fun isPending(): Boolean = status == TicketGiftStatus.PENDING

    // ===== 전이 기록 (검증·잠금은 service.v2) =====

    internal fun accept(receiverUserId: Long, now: LocalDateTime) {
        this.status = TicketGiftStatus.ACCEPTED
        this.receiverUserId = receiverUserId
        this.acceptedAt = now
    }

    internal fun reject(receiverUserId: Long, now: LocalDateTime) {
        this.status = TicketGiftStatus.REJECTED
        this.receiverUserId = receiverUserId
        this.rejectedAt = now
    }

    internal fun returnToSender(now: LocalDateTime) {
        this.status = TicketGiftStatus.RETURNED
        this.returnedAt = now
    }

    internal fun cancel(reason: TicketGiftCancelReason, now: LocalDateTime) {
        this.status = TicketGiftStatus.CANCELED
        this.cancelReason = reason
        this.canceledAt = now
    }

    internal fun changeMemo(memo: String?) {
        this.memo = memo
    }

    companion object {
        const val TOKEN_MAX_LENGTH = 64
        const val MEMO_MAX_LENGTH = 50
    }
}
