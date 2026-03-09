package band.gosrock.domain.domains.host.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.exception.HostNotFoundException
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

@Adaptor
class HostAdaptor(private val hostRepository: HostRepository) {

    fun findById(hostId: Long): Host =
        hostRepository.findById(hostId).orElseThrow { HostNotFoundException.EXCEPTION }

    /** 자신이 속해있는 호스트 리스트를 무한스크롤로 가져오는 쿼리 요청 */
    fun querySliceHostsByUserId(userId: Long, pageable: Pageable): Slice<Host> =
        hostRepository.querySliceHostsByUserId(userId, pageable)

    /** 자신이 마스터인 호스트 리스트를 가져오는 쿼리 요청 */
    fun findAllByMasterUserId(userId: Long): List<Host> =
        hostRepository.findAllByMasterUserId(userId)

    /** 자신이 속해있는 호스트 리스트를 가져오는 쿼리 요청 */
    fun findAllByHostUsers_UserId(userId: Long, pageable: Pageable): Page<Host> =
        hostRepository.findAllByHostUsers_UserId(userId, pageable)

    fun findAllByHostUsers_UserId(userId: Long): List<Host> =
        hostRepository.findAllByHostUsers_UserId(userId)

    /** 자신이 속해있는 호스트 리스트 중 초대 수락한 호스트만 가져오는 쿼리 요청 */
    fun querySliceHostsByActiveUserId(userId: Long): List<Host> =
        hostRepository.queryHostsByActiveUserId(userId)

    fun querySliceHostsByActiveUserId(userId: Long, pageable: Pageable): Slice<Host> =
        hostRepository.querySliceHostsByActiveUserId(userId, pageable)
}
