package band.gosrock.api.v2.operation.usecase

/** v2 엑셀 기본 열 (옵션 열 앞). 옵션 헤더 이름 충돌 검사에 쓰므로 UseCase 와 매퍼가 서로 참조하지 않도록 여기 둔다 (#730) */
object V2ExcelHeaders {
    /** R-6 주문 엑셀 */
    val ORDER = listOf("주문번호", "주문자", "연락처", "입금자명", "티켓", "매수", "결제금액", "주문일시", "상태", "환불", "거절·취소 사유")

    /** I-3 발급 티켓 엑셀 */
    val ISSUED_TICKET = listOf("티켓번호", "티켓 종류", "티켓 이름", "주문자", "연락처", "주문번호", "발급일시", "입장", "체크인 시각")
}
