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
    /** 호스트 행에 비관적 쓰기 락을 걸고 최신 상태로 다시 읽는다 (영속성 컨텍스트에 이미 있어도 refresh) */
    fun findByIdForUpdate(hostId: Long): Host?

    /** 지금까지의 변경을 flush 한다. 제약 위반은 DataIntegrityViolationException 으로 변환된다 */
    fun flushChanges()

    fun queryPageHostsByActiveUserId(userId: Long, keyword: String?, pageable: Pageable): Page<Host>
}
