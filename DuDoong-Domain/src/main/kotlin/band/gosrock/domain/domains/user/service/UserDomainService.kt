package band.gosrock.domain.domains.user.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.AlreadySignUpUserException
import band.gosrock.domain.domains.user.exception.HostMasterCannotWithdrawException
import band.gosrock.domain.domains.user.repository.UserRepository
import org.springframework.transaction.annotation.Transactional

@DomainService
open class UserDomainService(
    private val userRepository: UserRepository,
    private val userAdaptor: UserAdaptor,
    private val hostAdaptor: HostAdaptor,
) {
    @Transactional
    @RedissonLock(LockName = "유저등록", identifier = "oid", paramClassType = OauthInfo::class)
    open fun registerUser(profile: Profile, oauthInfo: OauthInfo, marketingAgree: Boolean): User {
        validUserCanRegister(oauthInfo)
        val newUser = User(
            profile = profile,
            marketingAgree = marketingAgree,
            oauthInfo = oauthInfo,
        )
        userRepository.save(newUser)
        return newUser
    }

    @Transactional
    @RedissonLock(LockName = "개발용회원가입", identifier = "oid", paramClassType = OauthInfo::class)
    open fun upsertUser(profile: Profile, oauthInfo: OauthInfo): User =
        userRepository.findByOauthInfo(oauthInfo).orElseGet {
            val newUser = User(
                profile = profile,
                marketingAgree = true,
                oauthInfo = oauthInfo,
            )
            userRepository.save(newUser)
            newUser
        }

    fun checkUserCanRegister(oauthInfo: OauthInfo): Boolean = !userAdaptor.exist(oauthInfo)

    fun validUserCanRegister(oauthInfo: OauthInfo) {
        if (!checkUserCanRegister(oauthInfo)) throw AlreadySignUpUserException.EXCEPTION
    }

    @Transactional
    open fun loginUser(oauthInfo: OauthInfo): User {
        val user = userAdaptor.queryUserByOauthInfo(oauthInfo)
        user.login()
        return user
    }

    /**
     * 회원 탈퇴. `유저탈퇴` 락(새 트랜잭션) 안에서 상태를 바꾸고, 탈퇴 전 카카오 연결 해제용 oid 를 돌려준다 (탈퇴하면 oauthInfo 가 지워진다).
     * `@Transactional` 이 필요 없다: 락 AOP 가 트랜잭션 AOP 바깥이라(#743) 락을 얻은 뒤 새 트랜잭션에서 돈다 (붙여도 그 트랜잭션에 참여만 한다).
     * 호출 측은 트랜잭션 없이 부른다 — 호출 측 트랜잭션이 있으면 그 커넥션을 쥔 채 락을 기다린다 (#734)
     */
    @RedissonLock(LockName = "유저탈퇴", identifier = "userId")
    open fun withDrawUser(userId: Long): String? {
        val user = userAdaptor.queryUser(userId)
        val oid = user.oauthInfo?.oid
        validateNotActiveHostMaster(userId)
        user.withDrawUser()
        return oid
    }

    /**
     * 운영 어드민의 계정 상태 변경 (#762). 탈퇴(DELETED)는 회원 탈퇴와 같은 락·규칙([withDrawUser])을 따른다
     * (마스터 검사, 프로필·oauth 정리, 선물 연쇄). 정지는 상태만 바꾸고 선물 연쇄는 엔티티 이벤트가 처리한다.
     * 락의 새 트랜잭션에서 커밋된 유저를 돌려준다 — 호출 측은 트랜잭션 없이 부른다 ([withDrawUser] 와 같은 이유)
     */
    @RedissonLock(LockName = "유저탈퇴", identifier = "userId")
    open fun changeAccountStateByAdmin(userId: Long, newState: AccountState): User {
        val user = userAdaptor.queryUser(userId)
        if (newState == AccountState.DELETED) validateNotActiveHostMaster(userId)
        user.changeAccountState(newState)
        return user
    }

    /**
     * 활성 호스트의 마스터는 탈퇴할 수 없다 (#762). 활성 호스트 = 탈퇴하지 않은 다른 활성 멤버가 있거나 진행중·정산중 공연이 있는 호스트.
     * 정지 멤버는 센다: 운영이 정지를 풀 수 있고, 마스터가 직접 내보내면(멤버 삭제) 탈퇴할 수 있다.
     * 혼자 있고 진행 중인 공연이 없는 호스트는 남겨도 영향받는 사람이 없어 막지 않는다 (호스트 삭제 기능은 없다, DEC-013)
     */
    fun validateNotActiveHostMaster(userId: Long) {
        if (hostAdaptor.existsActiveHostMasteredBy(userId, ACTIVE_EVENT_STATUSES)) throw HostMasterCannotWithdrawException.EXCEPTION
    }

    @Transactional
    open fun toggleMarketAgree(currentUserId: Long) {
        val user = userAdaptor.queryUser(currentUserId)
        user.toggleMarketingAgree()
    }

    @Transactional
    open fun toggleMailAgree(currentUserId: Long) {
        val user = userAdaptor.queryUser(currentUserId)
        user.toggleReceiveEmail()
    }

    companion object {
        private val ACTIVE_EVENT_STATUSES = listOf(EventStatus.OPEN, EventStatus.CALCULATING)
    }
}
