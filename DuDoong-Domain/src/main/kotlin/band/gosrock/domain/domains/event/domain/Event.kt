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
import band.gosrock.domain.domains.event.exception.InvalidEventStatusTransitionException
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Where

/** check_in_token unique: 셀프 체크인 QR 토큰 (v2, #712, V004) */
@Table(indexes = [Index(name = "uk_event_check_in_token", columnList = "check_in_token", unique = true)])
@Where(clause = "status != 'DELETED'")
@Entity(name = "tbl_event")
class Event(
    // 호스트 정보
    var hostId: Long? = null,
    name: String? = null,
    startAt: LocalDateTime? = null,
    runTime: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    var id: Long? = null
        protected set

    @Embedded
    var eventBasic: EventBasic? = if (name != null || startAt != null || runTime != null) {
        EventBasic(name = name, startAt = startAt, runTime = runTime)
    } else null
        protected set

    /**
     * 종료 시각 비정규화 컬럼 (v2, tbl_event.end_at). **쓰기 전용** — v1 이 살아있는 동안 기준값은 [getEndAt] (startAt + runTime) 이다.
     * v1 / v2 / 어드민 어느 경로로 저장해도 startAt + runTime(분) 으로 함께 기록한다. 조회·정산·종료 배치는 이 컬럼을 읽지 않는다
     */
    @Column(name = "end_at")
    var storedEndAt: LocalDateTime? = eventBasic?.endAt()
        protected set

    /** 티켓 여부 (v2). false 면 체크리스트 티켓 항목 면제 (DEC-008), 등록 후 변경 불가 (DEC-007). 기존 공연은 true */
    @ColumnDefault("1")
    @Column(nullable = false)
    var hasTicket: Boolean = true
        protected set

    /**
     * 셀프 체크인 QR 토큰 (v2, DEC-011). 공연별 고정, 공연 등록 시가 아니라 Q-4 최초 조회 시 생성.
     *
     * **읽기 전용 매핑**: 엔티티 저장으로는 쓰지 않는다(insertable/updatable=false). v1·v2 의 공연 저장(전체 컬럼 UPDATE)이
     * 동시에 만든 토큰을 덮어쓰지 않도록 V2CheckInDomainService 가 `check_in_token IS NULL` 조건부 native UPDATE 로만 기록한다.
     * 그래서 생성 직후에는 이미 영속성 컨텍스트에 올라온 이 엔티티의 값이 갱신되지 않는다(stale, null 일 수 있음).
     * 토큰 값은 `V2CheckInDomainService.getOrCreateCheckInToken` 결과나 `EventRepository.findCheckInTokenById` 로 읽는다
     */
    @Column(name = "check_in_token", length = 64, insertable = false, updatable = false)
    var checkInToken: String? = null
        protected set

    // v2 문의처 (N개)
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val contacts: MutableList<EventContact> = mutableListOf()

    // v2 상세 정보 섹션. 비어 있으면 v1 content 를 '공연 소개' 로 대체 표시 (V2EventDomainService.displaySections)
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val sections: MutableList<EventSection> = mutableListOf()

    // v2 태그
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    val tags: MutableList<EventTag> = mutableListOf()

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

    init {
        if (hostId != null && name != null) {
            Events.raise(EventCreationEvent.of(hostId, name))
        }
    }

    fun getStartAt(): LocalDateTime? = this.eventBasic?.startAt

    /**
     * 종료 시각 기준값 = startAt + runTime(분). v1 이 살아있는 동안 end_at 컬럼([storedEndAt])이 아니라 이 계산값을 쓴다.
     * 종료 배치(queryEventsByEndAtBeforeAndStatusOpen 의 TIMESTAMPADD) / 정산 배치(getEndAt) 와 같은 기준
     */
    fun getEndAt(): LocalDateTime? = this.eventBasic?.endAt()

    fun hasEventBasic(): Boolean = this.eventBasic?.isUpdated() == true

    fun hasEventPlace(): Boolean = this.eventPlace?.isUpdated() == true

    fun hasEventDetail(): Boolean = this.eventDetail?.isUpdated() == true

    fun isPreparing(): Boolean = this.status == PREPARING

    fun isClosed(): Boolean = this.status == CLOSED

    fun updateEventBasic(eventBasic: EventBasic?) {
        validateOpenStatus()
        this.eventBasic = eventBasic
        // v2 호환: runTime 이 바뀌면 end_at 도 갱신
        this.storedEndAt = eventBasic?.endAt()
    }

    fun updateEventDetail(eventDetail: EventDetail) {
        this.eventDetail = eventDetail
        syncIntroSectionFromV1(eventDetail.content)
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

    private fun updateStatus(newStatus: EventStatus, alreadySameException: DuDoongCodeException) {
        if (this.status == newStatus) throw alreadySameException
        if (!this.status.canTransitionTo(newStatus)) {
            throw InvalidEventStatusTransitionException.EXCEPTION
        }
        this.status = newStatus
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

    /** 어드민 전용: 상태 전이 밸리데이션 없이 직접 상태 변경 */
    fun adminUpdateStatus(newStatus: EventStatus) {
        this.status = newStatus
    }

    /** 어드민 전용: OPEN 여부 무관하게 기본 정보 부분 수정 */
    fun adminUpdate(
        name: String?,
        startAt: java.time.LocalDateTime?,
        runTime: Long?,
        content: String?,
        placeName: String?,
        placeAddress: String?,
    ) {
        val currentBasic = this.eventBasic
        this.eventBasic = EventBasic(
            name = name ?: currentBasic?.name,
            startAt = startAt ?: currentBasic?.startAt,
            runTime = runTime ?: currentBasic?.runTime,
        )
        this.storedEndAt = this.eventBasic?.endAt()
        if (content != null) {
            val currentDetail = this.eventDetail
            this.eventDetail = EventDetail(
                posterImageKey = currentDetail?.posterImage?.imageKey,
                content = content,
            )
            syncIntroSectionFromV1(content)
        }
        if (placeName != null || placeAddress != null) {
            val currentPlace = this.eventPlace
            this.eventPlace = EventPlace(
                latitude = currentPlace?.latitude,
                longitude = currentPlace?.longitude,
                placeName = placeName ?: currentPlace?.placeName,
                placeAddress = placeAddress ?: currentPlace?.placeAddress,
            )
        }
    }

    // ===== v2 공유 데이터 (검증·조합 규칙은 service.v2.V2EventDomainService) =====

    /** 티켓 여부 변경. 변경 가능 여부(준비중, 유효 티켓)는 V2EventDomainService 에서 검증한다 */
    internal fun changeHasTicket(hasTicket: Boolean) {
        this.hasTicket = hasTicket
    }

    /** 이름·일정 변경. end_at 컬럼은 v1 과 같은 기준(startAt + runTime)으로 함께 기록한다 */
    internal fun changeSchedule(name: String?, startAt: LocalDateTime?, runTime: Long?) {
        this.eventBasic = EventBasic(name = name, startAt = startAt, runTime = runTime)
        this.storedEndAt = this.eventBasic?.endAt()
    }

    /** 포스터 변경 (null 이면 비움). v1 상세 본문(content)은 유지한다 */
    internal fun changePosterImage(posterImageKey: String?) {
        this.eventDetail = EventDetail(posterImageKey = posterImageKey, content = this.eventDetail?.content)
    }

    internal fun changePlace(place: EventPlace) {
        this.eventPlace = place
    }

    /** 문의처 전체 교체. 개수·길이 검증은 V2EventDomainService 에서 한다 */
    internal fun replaceContacts(newContacts: List<EventContact>) {
        this.contacts.clear()
        newContacts.forEachIndexed { index, contact ->
            contact.assignTo(this, index)
            this.contacts.add(contact)
        }
    }

    /**
     * 태그 전체 교체 (중복 제거된 id). 개수·존재 검증은 V2EventDomainService 에서 한다.
     * unique(event_id, tag_id) 라 clear 후 다시 넣지 않고 차이만 반영한다 (Hibernate 는 insert 를 delete 보다 먼저 flush)
     */
    internal fun replaceTagIds(distinctIds: List<Long>) {
        this.tags.removeIf { it.tagId !in distinctIds }
        val existing = this.tags.map { it.tagId }.toSet()
        distinctIds.filter { it !in existing }.forEach { this.tags.add(EventTag(event = this, tagId = it)) }
    }

    fun getTagIds(): List<Long> = this.tags.map { it.tagId }

    /**
     * 섹션 전체 교체 (순서는 들어온 순서). 개수·제목·본문 길이 검증은 V2EventDomainService 에서 한다.
     * v1 호환: 첫 섹션('공연 소개') 본문을 tbl_event.content 에도 그대로 기록한다
     */
    internal fun replaceSections(newSections: List<EventSection>) {
        this.sections.clear()
        newSections.forEachIndexed { index, section ->
            section.assignTo(this, index)
            this.sections.add(section)
        }
        findIntroSection()?.let { intro ->
            this.eventDetail = EventDetail(posterImageKey = this.eventDetail?.posterImage?.imageKey, content = intro.content ?: "")
        }
        Events.raise(EventContentChangeEvent.of(this))
    }

    /** '공연 소개' 섹션 = 첫 번째 섹션 (sortOrder 최소, 제목 무관). v1 content 와 동기화되는 섹션 */
    fun findIntroSection(): EventSection? = this.sections.minByOrNull { it.sortOrder }

    /** v1 에서 content 가 바뀌면 v2 첫 섹션에도 반영하고 형식을 MARKDOWN 으로 표시 (섹션이 없으면 그대로 — 조회 시 대체 표시) */
    private fun syncIntroSectionFromV1(content: String?) {
        findIntroSection()?.changeContentFromV1(content)
    }
}
