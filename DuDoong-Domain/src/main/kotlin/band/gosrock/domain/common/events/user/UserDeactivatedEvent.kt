package band.gosrock.domain.common.events.user

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.user.domain.AccountState

/**
 * 계정이 정상(NORMAL)이 아니게 됨 — 회원 탈퇴(v1 `DELETE /v1/auth/me`) 또는 운영 상태 변경(정지·탈퇴, #719 DEC-026 #9·#10).
 * 같은 트랜잭션(BEFORE_COMMIT)에서 그 사용자가 보낸 대기 선물을 취소한다
 */
class UserDeactivatedEvent(val userId: Long, val accountState: AccountState) : DomainEvent() {
    override fun toString(): String = "UserDeactivatedEvent(userId=$userId, accountState=$accountState)"
}
