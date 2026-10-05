package band.gosrock.api.v2.mypage.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.mypage.dto.response.V2FollowingHostResponse
import band.gosrock.api.v2.mypage.dto.response.V2RepresentativeEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.service.v2.V2RepresentativeEventRule
import band.gosrock.domain.domains.host.service.v2.V2FollowingHostFilter
import band.gosrock.domain.domains.host.service.v2.V2MyPageHostQuery
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadFollowingHostsUseCase(
    private val v2MyPageHostQuery: V2MyPageHostQuery,
) {
    /** M-4 (#729). 호스트 목록 + 건수 + 페이지 호스트들의 공개 공연 1번 — 페이지 크기와 관계없이 쿼리 3개 (N+1 없음) */
    @Transactional(readOnly = true)
    fun execute(userId: Long, filter: V2FollowingHostFilter, page: Int, size: Int): V2PageResponse<V2FollowingHostResponse> {
        val now = LocalDateTime.now()
        val hosts = v2MyPageHostQuery.findFollowingHosts(userId, filter, now, PageRequest.of(page, size))
        val eventsByHost = v2MyPageHostQuery.findPublicEventsByHostIds(hosts.content.map { it.hostId }).groupBy { it.hostId }
        return V2PageResponse.of(
            hosts.map { host ->
                val event = V2RepresentativeEventRule.pick(eventsByHost[host.hostId].orEmpty(), now)
                V2FollowingHostResponse.of(host, event?.let { V2RepresentativeEventResponse.of(it, now) })
            },
        )
    }
}
