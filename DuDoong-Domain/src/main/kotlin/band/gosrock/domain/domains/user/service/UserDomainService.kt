package band.gosrock.domain.domains.user.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.AlreadySignUpUserException
import band.gosrock.domain.domains.user.repository.UserRepository
import org.springframework.transaction.annotation.Transactional

@DomainService
open class UserDomainService(
    private val userRepository: UserRepository,
    private val userAdaptor: UserAdaptor,
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
     * `@Transactional` 을 붙이지 않는다 (#734): 붙이면 그 트랜잭션이 락 AOP 바깥에서 먼저 시작돼, 락의 새 트랜잭션과 함께 커넥션 2개를 쥔다.
     * 같은 이유로 호출 측도 트랜잭션 없이 부른다
     */
    @RedissonLock(LockName = "유저탈퇴", identifier = "userId")
    open fun withDrawUser(userId: Long): String? {
        val user = userAdaptor.queryUser(userId)
        val oid = user.oauthInfo?.oid
        user.withDrawUser()
        return oid
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
}
