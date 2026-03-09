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
        val newUser = User.builder()
            .profile(profile)
            .marketingAgree(marketingAgree)
            .oauthInfo(oauthInfo)
            .build()
        userRepository.save(newUser)
        return newUser
    }

    @Transactional
    @RedissonLock(LockName = "개발용회원가입", identifier = "oid", paramClassType = OauthInfo::class)
    open fun upsertUser(profile: Profile, oauthInfo: OauthInfo): User =
        userRepository.findByOauthInfo(oauthInfo).orElseGet {
            val newUser = User.builder()
                .profile(profile)
                .marketingAgree(true)
                .oauthInfo(oauthInfo)
                .build()
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

    @Transactional
    @RedissonLock(LockName = "유저탈퇴", identifier = "userId")
    open fun withDrawUser(userId: Long) {
        val user = userAdaptor.queryUser(userId)
        user.withDrawUser()
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
