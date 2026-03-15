package band.gosrock.domain.domains.event.domain

import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.event.EventContentChangeEvent
import band.gosrock.domain.common.events.event.EventCreationEvent
import band.gosrock.domain.common.events.event.EventDeletionEvent
import band.gosrock.domain.common.events.event.EventStatusChangeEvent
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.EventBasicVo
import band.gosrock.domain.common.vo.EventDetailVo
import band.gosrock.domain.common.vo.EventInfoVo
import band.gosrock.domain.common.vo.EventPlaceVo
import band.gosrock.domain.common.vo.EventProfileVo
import band.gosrock.domain.common.vo.RefundInfoVo
import band.gosrock.domain.domains.event.domain.EventStatus.CALCULATING
import band.gosrock.domain.domains.event.domain.EventStatus.CLOSED
import band.gosrock.domain.domains.event.domain.EventStatus.DELETED
import band.gosrock.domain.domains.event.domain.EventStatus.OPEN
import band.gosrock.domain.domains.event.domain.EventStatus.PREPARING
import band.gosrock.domain.domains.event.exception.AlreadyCalculatingStatusException
import band.gosrock.domain.domains.event.exception.AlreadyCloseStatusException
import band.gosrock.domain.domains.event.exception.AlreadyDeletedStatusException
import band.gosrock.domain.domains.event.exception.AlreadyOpenStatusException
import band.gosrock.domain.domains.event.exception.AlreadyPreparingStatusException
import band.gosrock.domain.domains.event.exception.CannotDeleteByOpenEventException
import band.gosrock.domain.domains.event.exception.CannotModifyOpenEventException
import band.gosrock.domain.domains.event.exception.EventNotOpenException
import band.gosrock.domain.domains.event.exception.EventOpenTimeExpiredException
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import org.hibernate.annotations.Where

@Where(clause = "status != 'DELETED'")
@Entity(name = "tbl_event")
class Event() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    var id: Long? = null
        protected set

    // 호스트 정보
    var hostId: Long? = null
        protected set

    @Embedded
    var eventBasic: EventBasic? = null
        protected set

    @Embedded
    var eventPlace: EventPlace? = null
        protected set

    @Embedded
    var eventDetail: EventDetail? = null
        protected set

    // 이벤트 상태
    @Enumerated(EnumType.STRING)
    var status: EventStatus = PREPARING
        protected set

    constructor(hostId: Long?, name: String?, startAt: LocalDateTime?, runTime: Long?) : this() {
        this.hostId = hostId
        this.eventBasic = EventBasic.builder().name(name).startAt(startAt).runTime(runTime).build()
        Events.raise(EventCreationEvent.of(hostId, name))
    }

    fun getStartAt(): LocalDateTime? = this.eventBasic?.startAt

    fun getEndAt(): LocalDateTime? = this.eventBasic?.endAt()

    fun hasEventBasic(): Boolean = this.eventBasic?.isUpdated() == true

    fun hasEventPlace(): Boolean = this.eventPlace?.isUpdated() == true

    fun hasEventDetail(): Boolean = this.eventDetail?.isUpdated() == true

    fun isPreparing(): Boolean = this.status == PREPARING

    fun isClosed(): Boolean = this.status == CLOSED

    fun updateEventBasic(eventBasic: EventBasic?) {
        validateOpenStatus()
        this.eventBasic = eventBasic
    }

    fun updateEventDetail(eventDetail: EventDetail) {
        this.eventDetail = eventDetail
        Events.raise(EventContentChangeEvent.of(this))
    }

    fun updateEventPlace(eventPlace: EventPlace) {
        validateOpenStatus()
        this.eventPlace = eventPlace
    }

    fun validateStartAt() {
        val startAt = getStartAt() ?: throw IllegalStateException("Event startAt must be set")
        if (startAt.isBefore(LocalDateTime.now())) throw EventOpenTimeExpiredException.EXCEPTION
    }

    fun validateOpenStatus() {
        if (status == OPEN) throw CannotModifyOpenEventException.EXCEPTION
    }

    fun validateNotOpenStatus() {
        if (status != OPEN) throw EventNotOpenException.EXCEPTION
    }

    fun validateTicketingTime() {
        if (!isTimeBeforeStartAt()) throw EventTicketingTimeIsPassedException.EXCEPTION
    }

    fun isRefundDateNotPassed(): Boolean = toRefundInfoVo().availAble

    fun isTimeBeforeStartAt(): Boolean {
        val startAt = getStartAt() ?: throw IllegalStateException("Event startAt must be set")
        return LocalDateTime.now().isBefore(startAt)
    }

    fun toRefundInfoVoWithOrderStatus(orderStatus: OrderStatus): RefundInfoVo {
        val startAt = getStartAt() ?: throw IllegalStateException("Event startAt must be set")
        return RefundInfoVo.of(startAt, orderStatus)
    }

    fun toRefundInfoVo(): RefundInfoVo {
        val startAt = getStartAt() ?: throw IllegalStateException("Event startAt must be set")
        return RefundInfoVo.from(startAt)
    }

    fun toEventInfoVo(): EventInfoVo = EventInfoVo.from(this)

    fun toEventDetailVo(): EventDetailVo = EventDetailVo.from(this)

    fun toEventBasicVo(): EventBasicVo = EventBasicVo.from(this)

    fun toEventProfileVo(): EventProfileVo = EventProfileVo.from(this)

    fun toEventPlaceVo(): EventPlaceVo = EventPlaceVo.from(this)

    fun prepare() {
        updateStatus(PREPARING, AlreadyPreparingStatusException.EXCEPTION)
    }

    fun open() {
        validateStartAt()
        updateStatus(OPEN, AlreadyOpenStatusException.EXCEPTION)
    }

    fun calculate() {
        updateStatus(CALCULATING, AlreadyCalculatingStatusException.EXCEPTION)
    }

    fun close() {
        updateStatus(CLOSED, AlreadyCloseStatusException.EXCEPTION)
    }

    private fun updateStatus(status: EventStatus, exception: DuDoongCodeException) {
        if (this.status == status) throw exception
        this.status = status
        Events.raise(EventStatusChangeEvent.of(this))
    }

    fun deleteSoft() {
        // 오픈된 이벤트는 삭제 불가
        if (this.status == OPEN) throw CannotDeleteByOpenEventException.EXCEPTION
        if (this.status == DELETED) throw AlreadyDeletedStatusException.EXCEPTION
        this.status = DELETED
        Events.raise(EventDeletionEvent.of(this))
    }

    fun getEventName(): String? = eventBasic?.name

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var hostId: Long? = null
        private var name: String? = null
        private var startAt: LocalDateTime? = null
        private var runTime: Long? = null

        fun hostId(hostId: Long?) = apply { this.hostId = hostId }
        fun name(name: String?) = apply { this.name = name }
        fun startAt(startAt: LocalDateTime?) = apply { this.startAt = startAt }
        fun runTime(runTime: Long?) = apply { this.runTime = runTime }
        fun build() = Event(hostId, name, startAt, runTime)
    }
}
