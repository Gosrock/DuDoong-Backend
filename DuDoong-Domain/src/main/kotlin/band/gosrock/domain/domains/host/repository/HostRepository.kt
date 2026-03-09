package band.gosrock.domain.domains.host.repository

import band.gosrock.domain.domains.host.domain.Host
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository

interface HostRepository : CrudRepository<Host, Long>, HostCustomRepository {
    fun findAllByMasterUserId(userId: Long): List<Host>
    fun findAllByHostUsers_UserId(userId: Long): List<Host>
    fun findAllByHostUsers_UserId(userId: Long, pageable: Pageable): Page<Host>
    fun findByHostUsersIdIn(userId: List<Long>): List<Host>
}
