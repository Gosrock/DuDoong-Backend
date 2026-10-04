package band.gosrock.domain.domains.event.repository

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface EventCustomRepository {
    fun querySliceEventsByHostIdIn(hostIds: List<Long>, pageable: Pageable): Slice<Event>

    fun querySliceEventsByStatus(status: EventStatus, pageable: Pageable): Slice<Event>

    fun querySliceEventsByKeyword(keyword: String?, pageable: Pageable): Slice<Event>

    fun queryEventsByEndAtBeforeAndStatusOpen(time: LocalDateTime): List<Event>

    /** 호스트별 공연 수 (삭제된 공연 제외). 공연이 없는 호스트는 결과에 없음 */
    fun queryEventCountsByHostIdIn(hostIds: List<Long>): Map<Long, Long>

    /** v2: 호스트들의 공연 (삭제 제외), 공연명 부분일치, 최신 생성 순 */
    fun queryPageEventsByHostIdInAndKeyword(hostIds: List<Long>, keyword: String?, pageable: Pageable): Page<Event>
}
