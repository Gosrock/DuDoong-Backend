package band.gosrock.domain.domains.host.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.event.domain.EventStatus
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

    /** 멤버 변경(추가/삭제/역할/양도)용. 트랜잭션 안에서 호출해야 하며 커밋까지 호스트 행을 잠근다 (v2) */
    fun findByIdForUpdate(hostId: Long): Host =
        hostRepository.findByIdForUpdate(hostId) ?: throw HostNotFoundException.EXCEPTION

    /** 자신이 속해있는 호스트 리스트를 무한스크롤로 가져오는 쿼리 요청 */
    fun querySliceHostsByUserId(userId: Long, pageable: Pageable): Slice<Host> =
        hostRepository.querySliceHostsByUserId(userId, pageable)

    /** 탈퇴하지 않은 다른 활성 멤버나 [eventStatuses] 상태의 공연이 있는, 자신이 마스터인 호스트가 있는지 (#762) */
    fun existsActiveHostMasteredBy(userId: Long, eventStatuses: Collection<EventStatus>): Boolean =
        hostRepository.existsActiveHostMasteredBy(userId, eventStatuses)

    /** 자신이 마스터인 호스트 리스트를 가져오는 쿼리 요청 */
    fun findAllByMasterUserId(userId: Long): List<Host> =
        hostRepository.findAllByMasterUserId(userId)

    /** 자신이 속해있는 호스트 리스트를 가져오는 쿼리 요청 */
    fun findAllByHostUsers_UserId(userId: Long, pageable: Pageable): Page<Host> =
        hostRepository.findAllByHostUsers_UserId(userId, pageable)

    fun findAllByHostUsers_UserId(userId: Long): List<Host> =
        hostRepository.findAllByHostUsers_UserId(userId)

    fun findAllForAdmin(keyword: String?, pageable: Pageable): Page<Host> =
        hostRepository.findAllForAdmin(keyword, pageable)

    /** 자신이 속해있는 호스트 리스트 중 초대 수락한 호스트만 가져오는 쿼리 요청 */
    fun querySliceHostsByActiveUserId(userId: Long): List<Host> =
        hostRepository.queryHostsByActiveUserId(userId)

    fun querySliceHostsByActiveUserId(userId: Long, pageable: Pageable): Slice<Host> =
        hostRepository.querySliceHostsByActiveUserId(userId, pageable)

    /** 초대 수락한(활성) 호스트를 이름 부분일치로 페이지 조회 (v2) */
    fun queryPageHostsByActiveUserId(userId: Long, keyword: String?, pageable: Pageable): Page<Host> =
        hostRepository.queryPageHostsByActiveUserId(userId, keyword, pageable)
}
