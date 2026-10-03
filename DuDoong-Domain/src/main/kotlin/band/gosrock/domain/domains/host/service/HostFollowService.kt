package band.gosrock.domain.domains.host.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.host.domain.HostFollow
import band.gosrock.domain.domains.host.repository.HostFollowRepository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DomainService
class HostFollowService(
    private val hostFollowRepository: HostFollowRepository,
) {
    /**
     * 멱등 팔로우. 별도 트랜잭션(REQUIRES_NEW)으로 저장해, 동시 요청으로 unique 제약에 걸려도
     * 호출자 트랜잭션은 영향받지 않는다. 제약 위반(DataIntegrityViolationException)은 호출자가 처리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun follow(hostId: Long, userId: Long) {
        if (hostFollowRepository.existsByHostIdAndUserId(hostId, userId)) return
        hostFollowRepository.save(HostFollow(hostId = hostId, userId = userId))
    }

    /** 멱등 언팔로우 */
    @Transactional
    fun unfollow(hostId: Long, userId: Long) {
        hostFollowRepository.deleteByHostIdAndUserId(hostId, userId)
    }
}
