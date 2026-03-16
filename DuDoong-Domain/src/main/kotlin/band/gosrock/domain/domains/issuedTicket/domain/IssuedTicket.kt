package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.common.consts.DuDoongStatic.NO_START_NUMBER
import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.issuedTicket.EntranceIssuedTicketEvent
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelEntranceException
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelException
import band.gosrock.domain.domains.issuedTicket.exception.CanNotEntranceException
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketAlreadyEntranceException
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderLineItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.infrastructure.config.mail.dto.EmailIssuedTicketInfo
import java.time.LocalDateTime
import java.util.UUID
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.PostPersist
import jakarta.persistence.PrePersist

@Entity(name = "tbl_issued_ticket")
class IssuedTicket() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_ticket_id")
    var id: Long? = null
        protected set

    var issuedTicketNo: String? = null
        protected set

    var eventId: Long? = null
        protected set

    @Embedded
    var userInfo: IssuedTicketUserInfoVo? = null
        protected set

    @Embedded
    var itemInfo: IssuedTicketItemInfoVo? = null
        protected set

    var orderUuid: String? = null
        protected set

    var enteredAt: LocalDateTime? = null
        protected set

    var orderLineId: Long? = null
        protected set

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "issued_ticket_id")
    var issuedTicketOptionAnswers: MutableList<IssuedTicketOptionAnswer> = mutableListOf()
        protected set

    @Column(nullable = false)
    var uuid: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    var issuedTicketStatus: IssuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE
        protected set

    constructor(
        eventId: Long?,
        userInfo: IssuedTicketUserInfoVo?,
        orderUuid: String?,
        orderLineId: Long?,
        itemInfo: IssuedTicketItemInfoVo?,
        issuedTicketStatus: IssuedTicketStatus?,
        issuedTicketOptionAnswers: List<IssuedTicketOptionAnswer>,
    ) : this() {
        this.eventId = eventId
        this.userInfo = userInfo
        this.itemInfo = itemInfo
        this.orderUuid = orderUuid
        this.orderLineId = orderLineId
        this.issuedTicketStatus = issuedTicketStatus ?: IssuedTicketStatus.ENTRANCE_INCOMPLETE
        this.issuedTicketOptionAnswers.addAll(issuedTicketOptionAnswers)
    }

    fun addOptionAnswers(answers: List<IssuedTicketOptionAnswer>) {
        issuedTicketOptionAnswers.addAll(answers)
    }

    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */

    @PrePersist
    fun createUUID() {
        this.uuid = UUID.randomUUID().toString()
    }

    @PostPersist
    fun createIssuedTicketNo() {
        this.issuedTicketNo = "T" + java.lang.Long.sum(NO_START_NUMBER, this.id!!)
    }

    fun sumOptionPrice(): Money =
        issuedTicketOptionAnswers
            .map { it.additionalPrice }
            .fold(Money.ZERO) { acc, money -> acc.plus(money) }

    fun toIssuedTicketInfoVo(): IssuedTicketInfoVo = IssuedTicketInfoVo.from(this)

    fun toEmailIssuedTicketInfo(): EmailIssuedTicketInfo = EmailIssuedTicketInfo(
        this.issuedTicketNo!!,
        this.itemInfo!!.ticketName!!,
        createdAtKt(),
        this.issuedTicketStatus.kr,
        this.itemInfo!!.price.toString(),
    )

    /** ---------------------------- 상태 변환 관련 메서드 ---------------------------------- */

    fun cancel() {
        if (!this.issuedTicketStatus.isBeforeEntrance()) {
            throw CanNotCancelException.EXCEPTION
        }
        this.issuedTicketStatus = IssuedTicketStatus.CANCELED
    }

    fun entrance() {
        if (this.issuedTicketStatus.isCanceled()) {
            throw CanNotEntranceException.EXCEPTION
        }
        if (this.issuedTicketStatus.isAfterEntrance()) {
            throw IssuedTicketAlreadyEntranceException.EXCEPTION
        }
        this.issuedTicketStatus = IssuedTicketStatus.ENTRANCE_COMPLETED
        this.enteredAt = LocalDateTime.now()
        Events.raise(EntranceIssuedTicketEvent.from(this))
    }

    fun entranceCancel() {
        if (!this.issuedTicketStatus.isAfterEntrance()) {
            throw CanNotCancelEntranceException.EXCEPTION
        }
        this.issuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE
    }

    fun getUserId(): Long? = this.userInfo?.userId

    companion object {
        @JvmStatic
        fun create(
            ticketItem: TicketItem,
            user: User,
            order: Order,
            eventId: Long?,
            orderLineItem: OrderLineItem,
        ): IssuedTicket {
            val orderOptionAnswers = orderLineItem.orderOptionAnswers
            return builder()
                .issuedTicketOptionAnswers(orderOptionAnswers.map { IssuedTicketOptionAnswer.from(it) })
                .itemInfo(IssuedTicketItemInfoVo.from(ticketItem))
                .orderLineId(orderLineItem.id)
                .orderUuid(order.uuid)
                .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
                .userInfo(IssuedTicketUserInfoVo.from(user))
                .eventId(eventId)
                .build()
        }

        @JvmStatic
        fun orderLineToIssuedTicket(
            ticketItem: TicketItem,
            user: User,
            order: Order,
            eventId: Long?,
            orderLineItem: OrderLineItem,
        ): List<IssuedTicket> {
            val quantity = orderLineItem.quantity!!
            return (0 until quantity).map { create(ticketItem, user, order, eventId, orderLineItem) }
        }

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var eventId: Long? = null
        private var userInfo: IssuedTicketUserInfoVo? = null
        private var orderUuid: String? = null
        private var orderLineId: Long? = null
        private var itemInfo: IssuedTicketItemInfoVo? = null
        private var issuedTicketStatus: IssuedTicketStatus? = null
        private var issuedTicketOptionAnswers: List<IssuedTicketOptionAnswer> = emptyList()

        fun eventId(eventId: Long?) = apply { this.eventId = eventId }
        fun userInfo(userInfo: IssuedTicketUserInfoVo?) = apply { this.userInfo = userInfo }
        fun orderUuid(orderUuid: String?) = apply { this.orderUuid = orderUuid }
        fun orderLineId(orderLineId: Long?) = apply { this.orderLineId = orderLineId }
        fun itemInfo(itemInfo: IssuedTicketItemInfoVo?) = apply { this.itemInfo = itemInfo }
        fun issuedTicketStatus(issuedTicketStatus: IssuedTicketStatus?) = apply { this.issuedTicketStatus = issuedTicketStatus }
        fun issuedTicketOptionAnswers(issuedTicketOptionAnswers: List<IssuedTicketOptionAnswer>) = apply { this.issuedTicketOptionAnswers = issuedTicketOptionAnswers }

        fun build(): IssuedTicket = IssuedTicket(
            eventId, userInfo, orderUuid, orderLineId, itemInfo, issuedTicketStatus, issuedTicketOptionAnswers,
        )
    }
}
