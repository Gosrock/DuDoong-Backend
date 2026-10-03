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
import band.gosrock.domain.common.vo.EventSectionVo
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
import band.gosrock.domain.domains.event.exception.CannotChangeHasTicketException
import band.gosrock.domain.domains.event.exception.CannotDeleteByOpenEventException
import band.gosrock.domain.domains.event.exception.CannotModifyEndedEventException
import band.gosrock.domain.domains.event.exception.CannotModifyOpenEventException
import band.gosrock.domain.domains.event.exception.EventCannotEndBeforeStartException
import band.gosrock.domain.domains.event.exception.EventNotOpenException
import band.gosrock.domain.domains.event.exception.EventOpenTimeExpiredException
import band.gosrock.domain.domains.event.exception.InvalidEventStatusTransitionException
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException
import band.gosrock.domain.domains.event.exception.InvalidEventContactException
import band.gosrock.domain.domains.event.exception.InvalidEventSectionException
import band.gosrock.domain.domains.event.exception.InvalidEventTagException
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Where

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
     * 종료 시각 (v2, tbl_event.end_at). v1 경로(생성 / PATCH basic / 어드민 수정)에서도 startAt + runTime 으로 함께 갱신해
     * 항상 `startAt + runTime(분)` 과 같은 값을 유지한다. null 이면 V002 이전에 만든 공연 → [getEndAt] 이 계산값으로 대체
     */
    @Column(name = "end_at")
    var storedEndAt: LocalDateTime? = eventBasic?.endAt()
        protected set

    /** 티켓 여부 (v2). false 면 체크리스트 티켓 항목 면제 (DEC-008), 등록 후 변경 불가 (DEC-007). 기존 공연은 true */
    @ColumnDefault("1")
    @Column(nullable = false)
    var hasTicket: Boolean = true
        protected set

    // v2 문의처 (N개)
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val contacts: MutableList<EventContact> = mutableListOf()

    // v2 상세 정보 섹션. 비어 있으면 v1 content 를 '공연 소개' 로 대체 표시 (displaySectionsV2)
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

    /** 저장된 종료 시각, 없으면(V002 이전 공연) startAt + runTime. 두 값은 v1/v2 어느 경로로 저장해도 같다 */
    fun getEndAt(): LocalDateTime? = this.storedEndAt ?: this.eventBasic?.endAt()

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

    // ===== v2 =====

    /** 준비중 / 등록된(OPEN) 공연만 수정 가능. 정산중·지난공연은 수정 불가 (삭제 공연은 @Where 로 조회되지 않음) */
    fun validateEditableV2() {
        if (this.status != PREPARING && this.status != OPEN) throw CannotModifyEndedEventException.EXCEPTION
    }

    /**
     * v2 기본 정보 부분 수정. null 인 항목은 변경하지 않는다. 등록(OPEN) 후에도 수정 가능 (DEC-007)
     * - hasTicket 은 준비중일 때만 바꿀 수 있다 (같은 값은 허용)
     * - startAt / endAt 중 하나만 오면 나머지는 기존 값으로 검증하고, runTime(분) 을 다시 계산해 v1 과 맞춘다
     * - posterImageKey 가 빈 문자열이면 포스터를 비운다
     */
    fun updateBasicV2(
        name: String? = null,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
        hasTicket: Boolean? = null,
        posterImageKey: String? = null,
        place: EventPlace? = null,
    ) {
        validateEditableV2()
        if (hasTicket != null && hasTicket != this.hasTicket) {
            if (this.status != PREPARING) throw CannotChangeHasTicketException.EXCEPTION
            this.hasTicket = hasTicket
        }
        if (name != null || startAt != null || endAt != null) {
            applyScheduleV2(
                name = name ?: getEventName(),
                startAt = startAt?.truncatedTo(ChronoUnit.MINUTES) ?: getStartAt(),
                endAt = endAt?.truncatedTo(ChronoUnit.MINUTES) ?: getEndAt(),
            )
        }
        if (posterImageKey != null) {
            this.eventDetail = EventDetail(posterImageKey = posterImageKey.ifBlank { null }, content = this.eventDetail?.content)
        }
        if (place != null) this.eventPlace = place
    }

    private fun applyScheduleV2(name: String?, startAt: LocalDateTime?, endAt: LocalDateTime?) {
        val runTime = if (startAt != null && endAt != null) runTimeMinutesOf(startAt, endAt) else this.eventBasic?.runTime
        this.eventBasic = EventBasic(name = name, startAt = startAt, runTime = runTime)
        this.storedEndAt = endAt ?: this.eventBasic?.endAt()
    }

    /** 문의처 전체 교체 (0~[MAX_CONTACT_COUNT]개). 체크리스트 기본 정보는 1개 이상이어야 충족 */
    fun replaceContactsV2(newContacts: List<EventContact>) {
        validateEditableV2()
        if (newContacts.size > MAX_CONTACT_COUNT) throw InvalidEventContactException.EXCEPTION
        newContacts.forEach {
            if (it.value.isBlank() || it.value.length > EventContact.VALUE_MAX_LENGTH) throw InvalidEventContactException.EXCEPTION
        }
        this.contacts.clear()
        newContacts.forEachIndexed { index, contact ->
            contact.assignTo(this, index)
            this.contacts.add(contact)
        }
    }

    /**
     * 태그 전체 교체 (중복 id 는 하나로, 최대 [MAX_TAG_COUNT]개). 태그 존재 여부는 서비스에서 검증한다.
     * unique(event_id, tag_id) 라 clear 후 다시 넣지 않고 차이만 반영한다 (Hibernate 는 insert 를 delete 보다 먼저 flush)
     */
    fun replaceTagIdsV2(tagIds: List<Long>) {
        validateEditableV2()
        val distinctIds = tagIds.distinct()
        if (distinctIds.size > MAX_TAG_COUNT) throw InvalidEventTagException.EXCEPTION
        this.tags.removeIf { it.tagId !in distinctIds }
        val existing = this.tags.map { it.tagId }.toSet()
        distinctIds.filter { it !in existing }.forEach { this.tags.add(EventTag(event = this, tagId = it)) }
    }

    fun getTagIds(): List<Long> = this.tags.map { it.tagId }

    /**
     * 섹션 전체 교체 (1~[MAX_SECTION_COUNT]개, 순서는 들어온 순서). 제목 앞뒤 공백은 제거한다.
     * v1 호환: '공연 소개' 섹션이 있으면 그 본문을 tbl_event.content 에도 기록한다 (없으면 v1 content 유지)
     */
    fun replaceSectionsV2(newSections: List<EventSection>) {
        validateEditableV2()
        if (newSections.isEmpty() || newSections.size > MAX_SECTION_COUNT) throw InvalidEventSectionException.EXCEPTION
        newSections.forEach {
            it.title = it.title.trim()
            if (it.title.isEmpty() || it.title.length > EventSection.TITLE_MAX_LENGTH) throw InvalidEventSectionException.EXCEPTION
            if ((it.content?.length ?: 0) > EventSection.CONTENT_MAX_LENGTH) throw InvalidEventSectionException.EXCEPTION
        }
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

    /** '공연 소개' 섹션 = 제목이 [EventSection.INTRO_TITLE] 인 첫 섹션 */
    fun findIntroSection(): EventSection? = this.sections.firstOrNull { it.isIntro() }

    /** v1 에서 content 가 바뀌면 v2 '공연 소개' 섹션에도 반영 (섹션이 없거나 공연 소개 섹션이 없으면 그대로) */
    private fun syncIntroSectionFromV1(content: String?) {
        findIntroSection()?.changeContent(content)
    }

    /** v2 섹션 표시. 섹션이 없는 기존 공연은 v1 content(비어 있지 않으면)를 '공연 소개' 섹션으로 대체 */
    fun displaySectionsV2(): List<EventSectionVo> {
        if (this.sections.isNotEmpty()) {
            return this.sections.map { EventSectionVo(sectionId = it.id, title = it.title, content = it.content, sortOrder = it.sortOrder) }
        }
        val content = this.eventDetail?.content?.takeIf { it.isNotBlank() } ?: return emptyList()
        return listOf(EventSectionVo(sectionId = null, title = EventSection.INTRO_TITLE, content = content, sortOrder = 0))
    }

    /** 기본 정보 충족: 이름·일정(시작/종료)·장소(이름/주소/좌표)·문의처 1개 이상 */
    fun hasBasicV2(): Boolean =
        !getEventName().isNullOrBlank() && getStartAt() != null && getEndAt() != null && hasEventPlace() && this.contacts.isNotEmpty()

    /** 상세 정보 충족: 본문이 비어 있지 않은 섹션 1개 이상 (섹션이 없는 기존 공연은 v1 content) */
    fun hasDetailV2(): Boolean = displaySectionsV2().any { !it.content.isNullOrBlank() }

    /** @param hasValidTicket 유효(삭제 안 된) 티켓 존재 여부 */
    fun checklistV2(hasValidTicket: Boolean, now: LocalDateTime): EventChecklist {
        val basic = hasBasicV2()
        val detail = hasDetailV2()
        val filled = basic && detail && (!this.hasTicket || hasValidTicket)
        val startAt = getStartAt()
        return EventChecklist(
            basic = basic,
            detail = detail,
            ticket = hasValidTicket,
            ticketRequired = this.hasTicket,
            canOpen = filled && this.status == PREPARING && startAt != null && startAt.isAfter(now),
        )
    }

    companion object {
        const val MAX_CONTACT_COUNT = 10
        const val MAX_SECTION_COUNT = 10
        const val MAX_TAG_COUNT = 10

        /** v2 간편 생성. 상태는 PREPARING. runTime(분) 은 종료 - 시작으로 계산해 v1 과 맞춘다 */
        fun createV2(hostId: Long, name: String, startAt: LocalDateTime, endAt: LocalDateTime, hasTicket: Boolean): Event {
            val start = startAt.truncatedTo(ChronoUnit.MINUTES)
            val end = endAt.truncatedTo(ChronoUnit.MINUTES)
            return Event(hostId = hostId, name = name, startAt = start, runTime = runTimeMinutesOf(start, end)).also {
                it.storedEndAt = end
                it.hasTicket = hasTicket
            }
        }

        /** 종료가 시작보다 늦어야 한다 */
        fun runTimeMinutesOf(startAt: LocalDateTime, endAt: LocalDateTime): Long {
            if (!endAt.isAfter(startAt)) throw EventCannotEndBeforeStartException.EXCEPTION
            return Duration.between(startAt, endAt).toMinutes()
        }
    }
}
