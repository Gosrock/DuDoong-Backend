package band.gosrock.domain.domains.user.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.UserNotFoundException
import band.gosrock.domain.domains.user.repository.UserRepository
import java.time.LocalDateTime

@Adaptor
class UserAdaptor(private val userRepository: UserRepository) {

    fun queryUser(userId: Long?): User =
        userRepository.findById(userId ?: throw UserNotFoundException.EXCEPTION).orElseThrow { UserNotFoundException.EXCEPTION }

    fun exist(oauthInfo: OauthInfo): Boolean =
        userRepository.findByOauthInfo(oauthInfo).isPresent

    fun queryUserByOauthInfo(oauthInfo: OauthInfo): User =
        userRepository.findByOauthInfo(oauthInfo).orElseThrow { UserNotFoundException.EXCEPTION }

    /** user id 리스트에 포함되어 있는 유저를 모두 가져오는 쿼리 */
    fun queryUserListByIdIn(userIdList: List<Long>): List<User> =
        userRepository.findAllByIdIn(userIdList)

    /** 이메일로 유저를 가져오는 쿼리 */
    fun queryUserByEmail(email: String): User =
        userRepository.findByProfileEmailAndAccountState(email, AccountState.NORMAL)
            .orElseThrow { UserNotFoundException.EXCEPTION }

    fun countNormalUserCreatedBefore(before: LocalDateTime): Long =
        userRepository.countByAccountStateAndCreatedAtBefore(AccountState.NORMAL, before)

    fun findUserByIdIn(userIds: List<Long>): List<User> =
        userRepository.findByIdIn(userIds)
}
