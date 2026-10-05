package band.gosrock.domain.domains.order.service.v2

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import java.util.UUID

/**
 * 이미 입력된 환불 계좌를 사용자가 다른 값으로 바꿈 (#728 리뷰). [V2UserOrderDomainService.putRefundAccount] 에서만 발행하고 첫 입력은 발행하지 않는다.
 * 호스트가 옛 계좌로 송금하지 않도록 마스터·매니저에게 알린다. [changeId] 는 변경 한 번마다 새 값 — 알림 dedup 키 (재처리는 1건, 변경마다 새 알림)
 */
class V2RefundAccountChangedEvent(
    val orderUuid: String,
    val changeId: String = UUID.randomUUID().toString(),
) : DomainEvent() {
    override fun toString(): String = "V2RefundAccountChangedEvent(orderUuid=$orderUuid, changeId=$changeId)"
}
