package band.gosrock.domain.domains.host.service.v2

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

/**
 * v2 멤버 즉시 추가 (DEC-015, #714). [V2HostDomainService.addActiveHostUsers] 에서만 발행한다.
 * v1 경로(초대 → 수락)의 `HostUserJoinEvent` 는 본인이 수락한 것이라 "추가됨" 알림 대상이 아니므로 따로 둔다.
 * (v2 추가도 `HostUser.activate` 를 거쳐 기존 `HostUserJoinEvent` 슬랙 알림은 그대로 나간다)
 */
class V2HostMembersAddedEvent(
    val hostId: Long,
    val userIds: List<Long>,
) : DomainEvent() {
    override fun toString(): String = "V2HostMembersAddedEvent(hostId=$hostId, userIds=$userIds)"
}
