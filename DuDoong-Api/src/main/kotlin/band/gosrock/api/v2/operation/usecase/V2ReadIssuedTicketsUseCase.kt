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
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketQuery
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketSearch
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import org.springframework.data.domain.PageRequest

@UseCase
class V2ReadIssuedTicketsUseCase(
    private val eventAdaptor: EventAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val optionAdaptor: OptionAdaptor,
    private val v2IssuedTicketQuery: V2IssuedTicketQuery,
    private val mapper: V2OperationMapper,
    private val excelService: AdminExcelService,
) {
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
        val users = mapper.usersOf(tickets.content.map { it.getUserId() })
        val orderNos = mapper.orderNosOf(tickets.content.map { it.orderUuid })
        return V2IssuedTicketListResponse(
            counts = V2EntranceStatsResponse.of(v2IssuedTicketQuery.stats(search)),
            tickets = V2PageResponse.of(tickets.map { mapper.toTicketElement(it, users[it.getUserId()], orderNos[it.orderUuid]) }),
        )
    }

    /** I-2 발급 티켓 상세 (취소 티켓 포함). 다른 공연 티켓은 404 */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun detail(userId: Long, eventId: Long, ticketUuid: String): V2IssuedTicketDetailResponse {
        val ticket = issuedTicketAdaptor.queryByIssuedTicketUuid(ticketUuid)
        if (ticket.eventId != eventId) throw IssuedTicketNotFoundException.EXCEPTION
        val user = mapper.usersOf(listOf(ticket.getUserId()))[ticket.getUserId()]
        return V2IssuedTicketDetailResponse(
            ticket = mapper.toTicketElement(ticket, user, mapper.orderNosOf(listOf(ticket.orderUuid))[ticket.orderUuid]),
            buyerPhone = mapper.phoneOf(user),
            optionAnswers = mapper.ticketOptionAnswers(ticket),
        )
    }

    /**
     * I-3 엑셀 (I-1 과 같은 필터, 전체 행). 기본 컬럼 + 이 공연 티켓에 붙은 옵션별 응답 컬럼
     * (옵션 이름이 같으면 옵션 그룹 id 를 붙여 구분)
     */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun export(userId: Long, eventId: Long, entrance: V2EntranceFilter, searchType: AdminTableSearchType?, keyword: String?): ByteArray {
        eventAdaptor.findById(eventId)
        val tickets = v2IssuedTicketQuery.findAll(V2IssuedTicketSearch(eventId = eventId, entrance = entrance.domain, searchType = searchType, keyword = keyword))
        val users = mapper.usersOf(tickets.map { it.getUserId() })
        val orderNos = mapper.orderNosOf(tickets.map { it.orderUuid })
        // 답변의 optionId(옵션 행) → 옵션 그룹. 컬럼은 옵션 그룹 단위
        val options = optionAdaptor.findAllByIds(tickets.flatMap { t -> t.issuedTicketOptionAnswers.mapNotNull { it.optionId } }.distinct())
        val groupOfOption = options.associate { it.id to it.getOptionGroupId() }
        val groups = options.mapNotNull { o -> o.getOptionGroupId()?.let { it to o.getQuestionName() } }.distinct().sortedBy { it.first }
        val duplicated = groups.groupBy { it.second }.filterValues { it.size > 1 }.keys
        val optionHeaders = groups.map { (id, name) -> if (name in duplicated || name == null) "${name ?: "옵션"}($id)" else name }
        val rows = tickets.map { t ->
            val e = mapper.toTicketElement(t, users[t.getUserId()], orderNos[t.orderUuid])
            val answers = t.issuedTicketOptionAnswers.associate { groupOfOption[it.optionId] to it.answer }
            listOf(
                e.issuedTicketNo, PAY_TYPE_LABELS[e.payType?.name], e.ticketName, e.buyerName, mapper.phoneOf(users[t.getUserId()]), e.orderNo,
                e.issuedAt?.format(V2ReadOrdersUseCase.EXCEL_DATE), if (e.entrance == V2EntranceState.DONE) "입장 완료" else "입장 전",
                e.enteredAt?.format(V2ReadOrdersUseCase.EXCEL_DATE),
            ) + groups.map { (groupId, _) -> answers[groupId] }
        }
        return excelService.generateTableExcel("발급 티켓 목록", TICKET_HEADERS + optionHeaders, rows)
    }

    companion object {
        val TICKET_HEADERS = listOf("티켓번호", "티켓 종류", "티켓 이름", "주문자", "연락처", "주문번호", "발급일시", "입장", "체크인 시각")
        private val PAY_TYPE_LABELS = mapOf("DUDOONG" to "두둥티켓", "FREE" to "무료티켓", "PRICE" to "유료티켓")
    }
}
