package band.gosrock.domain.domains.host.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.host.domain.HostFollow
import band.gosrock.domain.domains.host.repository.HostFollowRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@DomainService
class HostFollowService(
    private val hostFollowRepository: HostFollowRepository,
) {
    /** 멱등 팔로우. 동시 요청으로 unique 제약에 걸려도 이미 팔로우된 상태이므로 성공으로 본다 (저장은 자체 트랜잭션) */
    fun follow(hostId: Long, userId: Long) {
        if (hostFollowRepository.existsByHostIdAndUserId(hostId, userId)) return
        try {
            hostFollowRepository.save(HostFollow(hostId = hostId, userId = userId))
        } catch (e: DataIntegrityViolationException) {
            // 동시 팔로우: 이미 저장됨
        }
    }

    /** 멱등 언팔로우 */
    @Transactional
    fun unfollow(hostId: Long, userId: Long) {
        hostFollowRepository.deleteByHostIdAndUserId(hostId, userId)
    }
}
