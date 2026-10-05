package band.gosrock.api.v2.operation.usecase

import band.gosrock.admin.service.AdminExcelService
import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.operation.dto.V2EntranceFilter
import band.gosrock.api.v2.operation.dto.response.V2EntranceStatsResponse
import band.gosrock.api.v2.operation.dto.response.V2IssuedTicketDetailResponse
import band.gosrock.api.v2.operation.dto.response.V2IssuedTicketListResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.issuedTicket.service.v2.V2HostGiftState
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketQuery
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketSearch
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType
import band.gosrock.domain.domains.order.service.v2.V2OrderQuery
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest

@UseCase
class V2ReadIssuedTicketsUseCase(
    private val eventAdaptor: EventAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val v2IssuedTicketQuery: V2IssuedTicketQuery,
    private val mapper: V2OperationMapper,
    private val excelService: AdminExcelService,
    @Value("\${v2.export.max-rows:${V2OrderQuery.EXPORT_MAX_ROWS}}") private val exportMaxRows: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** I-1 발급 티켓 목록 (유효 티켓, 최신 순) + 입장 상태별 건수 */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(
        userId: Long,
        eventId: Long,
        entrance: V2EntranceFilter,
        searchType: AdminTableSearchType?,
        keyword: String?,
        page: Int,
        size: Int,
    ): V2IssuedTicketListResponse {
        eventAdaptor.findById(eventId)
        val search = V2IssuedTicketSearch(eventId = eventId, entrance = entrance.domain, searchType = searchType, keyword = keyword)
        val tickets = v2IssuedTicketQuery.findPage(search, PageRequest.of(page, size))
        val elements = mapper.ticketViewsOf(tickets.content).associate { it.ticket to it.element }
        return V2IssuedTicketListResponse(
            counts = V2EntranceStatsResponse.of(v2IssuedTicketQuery.stats(search)),
            tickets = V2PageResponse.of(tickets.map { elements.getValue(it) }),
        )
    }

    /** I-2 발급 티켓 상세 (취소 티켓 포함). 다른 공연 티켓은 404 */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun detail(userId: Long, eventId: Long, ticketUuid: String): V2IssuedTicketDetailResponse {
        val ticket = issuedTicketAdaptor.queryByIssuedTicketUuid(ticketUuid)
        if (ticket.eventId != eventId) throw IssuedTicketNotFoundException.EXCEPTION
        val view = mapper.ticketViewsOf(listOf(ticket)).single()
        return V2IssuedTicketDetailResponse(
            ticket = view.element,
            buyerPhone = mapper.phoneOf(view.buyer),
            ownerPhone = mapper.phoneOf(view.owner),
            optionAnswers = mapper.ticketOptionAnswers(ticket),
        )
    }

    /**
     * I-3 엑셀 (I-1 과 같은 필터, 전체 행, 상한 [exportMaxRows] 초과 시 IssuedTicket_400_7). 기본 컬럼 + 답변이 있는 옵션별 응답 컬럼
     * (옵션 이름이 같으면 옵션 그룹 id 를 붙여 구분). 답변은 fetch join, 옵션 이름은 한 번에 조회.
     * 개인정보: 연락처 포함(입금 확인용, v1 수준), 이메일 제외. 다운로드는 감사 로그를 남긴다
     */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun export(userId: Long, eventId: Long, entrance: V2EntranceFilter, searchType: AdminTableSearchType?, keyword: String?): ByteArray {
        eventAdaptor.findById(eventId)
        val search = V2IssuedTicketSearch(eventId = eventId, entrance = entrance.domain, searchType = searchType, keyword = keyword)
        val tickets = v2IssuedTicketQuery.findAllForExport(search, exportMaxRows)
        // 답변의 optionId(옵션 행) → 옵션 그룹. 컬럼은 옵션 그룹 단위 (R-6 과 같은 규칙)
        val columns = mapper.excelOptionColumnsOf(tickets.flatMap { t -> t.issuedTicketOptionAnswers.map { it.optionId } }, V2ExcelHeaders.ISSUED_TICKET)
        val rows = mapper.ticketViewsOf(tickets).map { view ->
            val (t, e) = view.ticket to view.element
            val answers = t.issuedTicketOptionAnswers.associate { columns.groupOfOption[it.optionId] to it.answer }
            listOf(
                e.issuedTicketNo, PAY_TYPE_LABELS[e.payType?.name], e.ticketName, e.buyerName, mapper.phoneOf(view.buyer),
                e.ownerName, mapper.phoneOf(view.owner), GIFT_LABELS[e.giftState], e.orderNo,
                e.issuedAt?.format(V2ReadOrdersUseCase.EXCEL_DATE), if (e.entrance == V2EntranceState.DONE) "입장 완료" else "입장 전",
                e.enteredAt?.format(V2ReadOrdersUseCase.EXCEL_DATE),
            ) + columns.groupIds.map { answers[it] }
        }
        log.info("[V2 엑셀] 발급 티켓 다운로드 userId={} eventId={} entrance={} rows={}", userId, eventId, entrance, rows.size)
        return excelService.generateTableExcel("발급 티켓 목록", V2ExcelHeaders.ISSUED_TICKET + columns.headers, rows, escapeFormula = true)
    }

    companion object {
        private val PAY_TYPE_LABELS = mapOf("DUDOONG" to "두둥티켓", "FREE" to "무료티켓", "PRICE" to "유료티켓")
        private val GIFT_LABELS = mapOf(V2HostGiftState.NONE to "", V2HostGiftState.PENDING to "선물 대기", V2HostGiftState.ACCEPTED to "선물 완료")
    }
}
