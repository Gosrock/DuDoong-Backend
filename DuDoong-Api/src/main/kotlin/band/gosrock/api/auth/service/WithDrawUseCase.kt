package band.gosrock.api.auth.service

import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor
import band.gosrock.domain.domains.user.service.UserDomainService
import org.slf4j.LoggerFactory

@UseCase
class WithDrawUseCase(
    private val refreshTokenAdaptor: RefreshTokenAdaptor,
    private val userDomainService: UserDomainService,
    private val kakaoOauthHelper: KakaoOauthHelper
) {
    private val log = LoggerFactory.getLogger(WithDrawUseCase::class.java)

    /**
     * 바깥 트랜잭션을 두지 않는다 (#734): 탈퇴는 `유저탈퇴` 락의 새 트랜잭션(커넥션 1)에서, 선물 연쇄 후보 읽기는 그 안의 짧은 새 트랜잭션(커넥션 2)에서 한다.
     * 이 메서드에 트랜잭션이 있으면 그 커넥션을 쥔 채 락을 기다려 최대 3개가 된다 (락 AOP 순서를 바로잡은 #743 이후에도 호출 측 트랜잭션은 그대로 남는다).
     * 탈퇴는 락 트랜잭션에서 이미 커밋되므로(예전에도 같음) 카카오 연결 해제가 실패해도 탈퇴는 유지된다
     */
    fun execute(userId: Long) {
        log.info("[WithDrawUseCase][execute] 회원 탈퇴 userId={}", userId)
        refreshTokenAdaptor.deleteByUserId(userId)
        val oid = userDomainService.withDrawUser(userId)
        if (oid == null) {
            log.warn("[WithDrawUseCase][execute] 카카오 oid 가 없어 연결 해제를 건너뜀 userId={}", userId)
            return
        }
        kakaoOauthHelper.unlink(oid)
    }
}
