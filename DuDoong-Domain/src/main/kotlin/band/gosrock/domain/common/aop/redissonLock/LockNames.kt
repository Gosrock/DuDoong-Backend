package band.gosrock.domain.common.aop.redissonLock

/** 여러 서비스가 함께 쓰는 `@RedissonLock` 이름 (#724). 같은 이름 + 같은 식별자 = 같은 락 */
object LockNames {
    /**
     * `티켓관리:{ticketItemId}` — 티켓 재고·판매 조건·승인 대기 검사를 한 줄로 세운다:
     * v1·v2 발급·재고 감소, 티켓 수정·옵션 변경, 운영 재고 조정, 승인형 주문 생성(v1·v2)
     */
    const val TICKET = "티켓관리"

    /** `주문생성:{userId}` — v1 결제형·선착순·쿠폰 주문 생성 (승인형은 [TICKET]) */
    const val ORDER_CREATE = "주문생성"
}
