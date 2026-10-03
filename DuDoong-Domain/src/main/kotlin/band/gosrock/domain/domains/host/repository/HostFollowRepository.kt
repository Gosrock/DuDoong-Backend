package band.gosrock.domain.domains.host.repository

import band.gosrock.domain.domains.host.domain.HostFollow
import org.springframework.data.repository.CrudRepository

interface HostFollowRepository : CrudRepository<HostFollow, Long> {
    fun existsByHostIdAndUserId(hostId: Long, userId: Long): Boolean
    fun countByHostId(hostId: Long): Long
    fun deleteByHostIdAndUserId(hostId: Long, userId: Long)
}
