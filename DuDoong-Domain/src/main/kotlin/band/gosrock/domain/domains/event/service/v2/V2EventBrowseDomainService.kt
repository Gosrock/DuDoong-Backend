package band.gosrock.domain.domains.event.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.vo.HostContactVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.EventNotFoundException
import band.gosrock.domain.domains.event.exception.InvalidEventTagException
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.service.v2.V2HostDomainService
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import org.springframework.transaction.annotation.Transactional

/**
 * v2 사용자 앱 공연 탐색 규칙 (#716, DEC-018). v1 코드는 이 서비스를 호출하지 않는다.
 * 공개 공연 = 등록(OPEN)·정산중(CALCULATING)·지난공연(CLOSED). 준비중(PREPARING)·삭제(DELETED, @Where 로 조회 안 됨)는 존재를 숨긴다.
 */
@DomainService
@Transactional(readOnly = true)
class V2EventBrowseDomainService(
    private val tagAdaptor: TagAdaptor,
    private val v2HostDomainService: V2HostDomainService,
) {

    /** 공개 공연이 아니면 404 (준비중 공연의 존재를 드러내지 않는다) */
    fun validatePublic(event: Event) {
        if (event.status !in PUBLIC_STATUSES) throw EventNotFoundException.EXCEPTION
    }

    /**
     * 태그 필터 묶음: 같은 분류는 OR, 분류끼리는 AND (DEC-014). 분류 순서(enum 선언 순)로 묶는다.
     * 중복 id 는 하나로, 존재하지 않는 태그 id 가 있으면 400 (Event_400_23: INVALID_EVENT_TAG). 개수 상한은 컨트롤러 `@Size`
     */
    fun tagFilterGroups(tagIds: Collection<Long>): List<List<Long>> {
        val distinctIds = tagIds.distinct()
        if (distinctIds.isEmpty()) return emptyList()
        val tags = tagAdaptor.findAllByIdIn(distinctIds)
        if (tags.size != distinctIds.size) throw InvalidEventTagException.EXCEPTION
        return tags.groupBy { it.category }
            .toSortedMap(compareBy { it.ordinal })
            .values
            .map { group -> group.map { it.id!! }.sorted() }
    }

    /** 공연 문의처. 공연 문의처가 없으면 호스트 연락처(v2 연락처, 없으면 v1 전화/이메일 — [V2HostDomainService.displayContacts]) */
    fun displayContacts(event: Event, host: Host): List<HostContactVo> =
        if (event.contacts.isNotEmpty()) {
            event.contacts.map { HostContactVo(type = it.type, value = it.value) }
        } else {
            v2HostDomainService.displayContacts(host)
        }

    companion object {
        val PUBLIC_STATUSES: Set<EventStatus> = setOf(EventStatus.OPEN, EventStatus.CALCULATING, EventStatus.CLOSED)
    }
}
