package band.gosrock.domain.domains.host.repository

import band.gosrock.domain.domains.host.domain.Host
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface HostCustomRepository {
    fun querySliceHostsByUserId(id: Long, pageable: Pageable): Slice<Host>
    fun queryHostsByActiveUserId(id: Long): List<Host>
    fun querySliceHostsByActiveUserId(id: Long, pageable: Pageable): Slice<Host>
    fun findAllForAdmin(keyword: String?, pageable: Pageable): Page<Host>
}
