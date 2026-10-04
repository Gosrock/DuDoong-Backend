package band.gosrock.api.v2.event.usecase

import band.gosrock.api.v2.event.dto.response.V2EventSectionResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.exception.EventNotFoundException
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadEventSectionsUseCase(
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val userAdaptor: UserAdaptor,
    private val v2EventDomainService: V2EventDomainService,
) {
    /**
     * 공개 API (비로그인 userId = 0). 준비중 공연은 활성 멤버(일반 포함)와 SUPER_ADMIN 만 볼 수 있고,
     * 그 외에는 존재를 드러내지 않도록 404 (H-14 가 비멤버에게 준비중 공연을 숨기는 것과 같은 기준).
     * 섹션이 없는 기존 공연은 v1 content 를 '공연 소개' 섹션으로 대체한다.
     */
    @Transactional(readOnly = true)
    fun execute(userId: Long, eventId: Long): List<V2EventSectionResponse> {
        val event = eventAdaptor.findById(eventId)
        if (event.isPreparing() && !canViewPreparing(userId, event)) throw EventNotFoundException.EXCEPTION
        return v2EventDomainService.displaySections(event).map { V2EventSectionResponse.from(it) }
    }

    private fun canViewPreparing(userId: Long, event: Event): Boolean {
        if (userId == ANONYMOUS_USER_ID) return false
        if (hostAdaptor.findById(event.hostId!!).isActiveHostUserId(userId)) return true
        return userAdaptor.queryUser(userId).accountRole == AccountRole.SUPER_ADMIN
    }

    companion object {
        private const val ANONYMOUS_USER_ID = 0L
    }
}
