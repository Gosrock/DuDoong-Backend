package band.gosrock.api.v2.host.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.host.dto.request.V2AddHostMembersRequest
import band.gosrock.api.v2.host.dto.response.V2HostMemberResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.exception.HostErrorCode
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.host.service.v2.V2HostDomainService
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2AddHostMembersUseCase(
    private val hostAdaptor: HostAdaptor,
    private val userAdaptor: UserAdaptor,
    private val v2HostDomainService: V2HostDomainService,
    private val hostRepository: HostRepository,
    private val readHostMembersUseCase: V2ReadHostMembersUseCase,
) {
    /**
     * 이메일로 일괄 추가. 수락 없이 즉시 활성 멤버 (DEC-015).
     * 하나라도 문제가 있으면 전체를 추가하지 않고, 어떤 이메일이 문제인지 reason 에 담는다.
     */
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, request: V2AddHostMembersRequest): List<V2HostMemberResponse> {
        val host = hostAdaptor.findByIdForUpdate(hostId)
        val members = request.members!!.map { it.email!!.trim() to it.role!! }

        // 역할 검증 먼저 (MASTER 지정 불가, 매니저 요청자는 GUEST 만)
        members.forEach { (_, role) -> v2HostDomainService.validateCanManageRole(host, userId, role) }

        val duplicated = members.groupBy { it.first.lowercase() }.filterValues { it.size > 1 }.keys
        if (duplicated.isNotEmpty()) {
            throw HostErrorCode.DUPLICATED_MEMBER_EMAIL.toDetailException(duplicated.joinToString(", "))
        }

        val usersByEmailGroup = userAdaptor.queryUsersByEmailIn(members.map { it.first })
            .groupBy { it.profile?.email?.lowercase() }
        // 같은 이메일의 정상 계정이 여러 개면 누구를 추가할지 알 수 없다
        val ambiguous = members.map { it.first }.filter { (usersByEmailGroup[it.lowercase()]?.size ?: 0) > 1 }
        if (ambiguous.isNotEmpty()) {
            throw HostErrorCode.AMBIGUOUS_MEMBER_EMAIL.toDetailException(ambiguous.joinToString(", "))
        }
        val usersByEmail = usersByEmailGroup.mapValues { it.value.single() }
        val notFound = members.map { it.first }.filter { usersByEmail[it.lowercase()] == null }
        if (notFound.isNotEmpty()) {
            throw HostErrorCode.MEMBER_EMAIL_NOT_FOUND.toDetailException(notFound.joinToString(", "))
        }

        val alreadyMember = members.map { it.first }.filter { host.hasHostUserId(usersByEmail.getValue(it.lowercase()).id!!) }
        if (alreadyMember.isNotEmpty()) {
            throw HostErrorCode.ALREADY_HOST_MEMBER_EMAIL.toDetailException(alreadyMember.joinToString(", "))
        }

        val hostUsers = members.map { (email, role) ->
            HostUser(host = host, userId = usersByEmail.getValue(email.lowercase()).id, role = role)
        }
        val saved = try {
            // 호스트 락 밖(v1 초대 등)에서 같은 유저가 동시에 들어온 경우 unique(host_id, user_id) 위반.
            // IDENTITY 라 insert 는 save 시점에 나가고, 남은 변경은 flush 로 여기서 확인한다
            v2HostDomainService.addActiveHostUsers(host, userId, hostUsers).also { hostRepository.flushChanges() }
        } catch (e: DataIntegrityViolationException) {
            throw HostErrorCode.ALREADY_HOST_MEMBER_EMAIL.toDetailException(members.joinToString(", ") { it.first })
        }
        return readHostMembersUseCase.toMemberResponses(saved)
    }
}
