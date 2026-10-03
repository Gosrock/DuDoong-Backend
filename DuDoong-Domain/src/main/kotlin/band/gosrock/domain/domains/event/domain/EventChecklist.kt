package band.gosrock.domain.domains.event.domain

/**
 * v2 공연 등록 체크리스트.
 * @property ticket 유효(삭제 안 된) 티켓이 1개 이상인지 (면제 여부와 무관한 실제 상태)
 * @property ticketRequired 티켓 항목이 필요한지. hasTicket=false 공연은 면제 (DEC-008)
 * @property canOpen 모든 항목 충족 + 시작 전 + 준비중
 */
data class EventChecklist(
    val basic: Boolean,
    val detail: Boolean,
    val ticket: Boolean,
    val ticketRequired: Boolean,
    val canOpen: Boolean,
) {
    /** 상태·시각을 제외한 입력 항목 충족 여부 */
    fun isFilled(): Boolean = basic && detail && (!ticketRequired || ticket)
}
