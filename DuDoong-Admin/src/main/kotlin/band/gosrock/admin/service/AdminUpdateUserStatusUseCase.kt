package band.gosrock.admin.service

import band.gosrock.admin.exception.AdminSuperAdminRequiredException
import band.gosrock.admin.model.dto.request.AdminUpdateUserStatusRequest
import band.gosrock.admin.model.dto.response.AdminUserResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.common.consts.DuDoongStatic.LOCAL_OID_PREFIX
import band.gosrock.common.properties.OauthProperties
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.service.UserDomainService
import band.gosrock.infrastructure.outer.api.oauth.client.KakaoInfoClient
import band.gosrock.infrastructure.outer.api.oauth.dto.UnlinkKaKaoTarget
import org.slf4j.LoggerFactory

@UseCase
class AdminUpdateUserStatusUseCase(
    private val userAdaptor: UserAdaptor,
    private val userDomainService: UserDomainService,
    private val refreshTokenAdaptor: RefreshTokenAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
    private val kakaoInfoClient: KakaoInfoClient,
    private val oauthProperties: OauthProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 트랜잭션을 두지 않는다: 상태 변경은 `유저탈퇴` 락의 새 트랜잭션에서 커밋된다 (회원 탈퇴 [UserDomainService.withDrawUser] 와 같은 이유, #734).
     * 정상이 아닌 상태로 바꾸면 refresh 를 지운다 (access 토큰은 JwtTokenFilter 가 계정 상태로 거부한다)
     */
    fun execute(userId: Long, targetUserId: Long, request: AdminUpdateUserStatusRequest): AdminUserResponse {
        val operator = adminAuthValidator.validateAdminOrAbove(userId)
        val targetUser = userAdaptor.queryUser(targetUserId)
        if (targetUser.accountRole == AccountRole.SUPER_ADMIN && operator.accountRole != AccountRole.SUPER_ADMIN) {
            throw AdminSuperAdminRequiredException.EXCEPTION
        }
        val oid = targetUser.oauthInfo?.oid

        val changed = userDomainService.changeAccountStateByAdmin(targetUserId, request.status)
        if (request.status != AccountState.NORMAL) refreshTokenAdaptor.deleteByUserId(targetUserId)
        if (request.status == AccountState.DELETED) unlinkKakao(targetUserId, oid)
        return AdminUserResponse.from(changed)
    }

    /** 운영 탈퇴도 회원 탈퇴처럼 카카오 연결을 해제한다. 탈퇴는 이미 커밋됐으므로 실패해도 기록만 남긴다 */
    private fun unlinkKakao(targetUserId: Long, oid: String?) {
        if (oid == null || oid.startsWith(LOCAL_OID_PREFIX)) return
        try {
            kakaoInfoClient.unlinkUser("KakaoAK ${oauthProperties.getKakaoAdminKey()}", UnlinkKaKaoTarget.from(oid))
        } catch (e: Exception) {
            log.warn("[AdminUpdateUserStatusUseCase] 카카오 연결 해제 실패 userId={} error={}", targetUserId, e.toString())
        }
    }
}
