package band.gosrock.api.v2.operation.usecase

import band.gosrock.admin.service.AdminExcelService
import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.operation.dto.V2OrderStatusFilter
import band.gosrock.api.v2.operation.dto.V2RefundStatusFilter
import band.gosrock.api.v2.operation.dto.response.V2OrderCountsResponse
import band.gosrock.api.v2.operation.dto.response.V2OrderDetailResponse
import band.gosrock.api.v2.operation.dto.response.V2OrderIssuedTicketResponse
import band.gosrock.api.v2.operation.dto.response.V2OrderLineResponse
import band.gosrock.api.v2.operation.dto.response.V2OrderListResponse
import band.gosrock.api.v2.operation.dto.response.V2RefundElement
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType
import band.gosrock.domain.domains.order.service.v2.V2OrderDomainService
import band.gosrock.domain.domains.order.service.v2.V2OrderQuery
import band.gosrock.domain.domains.order.service.v2.V2OrderSearch
import java.time.format.DateTimeFormatter
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadOrdersUseCase(
    private val eventAdaptor: EventAdaptor,
    private val orderAdaptor: OrderAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val v2OrderQuery: V2OrderQuery,
    private val v2OrderDomainService: V2OrderDomainService,
    private val mapper: V2OperationMapper,
    private val excelService: AdminExcelService,
) {
    /** R-1 주문 목록 (최신 순) + 상태별 건수 */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(
        userId: Long,
        eventId: Long,
        status: V2OrderStatusFilter,
        searchType: AdminTableSearchType?,
        keyword: String?,
        page: Int,
        size: Int,
    ): V2OrderListResponse {
        eventAdaptor.findById(eventId)
        val search = V2OrderSearch(eventId = eventId, status = status.domain, searchType = searchType, keyword = keyword)
        val orders = v2OrderQuery.findPage(search, PageRequest.of(page, size))
        val users = mapper.usersOf(orders.content.map { it.userId })
        return V2OrderListResponse(
            counts = V2OrderCountsResponse.of(v2OrderQuery.counts(search)),
            orders = V2PageResponse.of(orders.map { mapper.toOrderElement(it, users[it.userId]) }),
        )
    }

    /** R-2 주문 상세 */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun detail(userId: Long, eventId: Long, orderUuid: String): V2OrderDetailResponse = readDetail(eventId, orderUuid)

    /** 변경 API 응답용 (권한 검사는 호출한 UseCase 에서 끝남). 변경 트랜잭션이 커밋된 뒤 새로 읽는다 */
    @Transactional(readOnly = true)
    fun readDetail(eventId: Long, orderUuid: String): V2OrderDetailResponse {
        val order = v2OrderDomainService.queryEventOrder(eventId, orderUuid)
        val user = mapper.usersOf(listOf(order.userId))[order.userId]
        val lines = order.orderLineItems.sortedBy { it.id }.map { line ->
            V2OrderLineResponse(
                ticketItemId = line.orderItem?.itemId,
                ticketName = line.orderItem?.name,
                unitPrice = line.orderItem?.price?.longValue() ?: 0L,
                quantity = line.quantity ?: 0L,
                linePrice = line.getTotalOrderLinePrice().longValue(),
                optionAnswers = mapper.toOptionAnswers(line.orderOptionAnswers.sortedBy { it.id }.map { Triple(it.optionId, it.answer, it.additionalPrice.longValue()) }),
            )
        }
        val tickets = issuedTicketAdaptor.findAllByOrderUuid(orderUuid).sortedBy { it.id }.map { t ->
            V2OrderIssuedTicketResponse(
                ticketUuid = t.uuid,
                issuedTicketNo = t.issuedTicketNo,
                ticketName = t.itemInfo?.ticketName,
                entrance = V2EntranceState.of(t.issuedTicketStatus),
                enteredAt = t.enteredAt,
                optionAnswers = mapper.ticketOptionAnswers(t),
            )
        }
        return V2OrderDetailResponse(
            order = mapper.toOrderElement(order, user),
            buyerEmail = user?.profile?.email,
            approvedAt = order.approvedAt,
            withdrawnAt = order.withDrawAt,
            refundStatusChangedAt = order.refundStatusChangedAt,
            lines = lines,
            issuedTickets = tickets,
        )
    }

    /** R-6 엑셀 (R-1 과 같은 필터, 전체 행) */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun export(userId: Long, eventId: Long, status: V2OrderStatusFilter, searchType: AdminTableSearchType?, keyword: String?): ByteArray {
        eventAdaptor.findById(eventId)
        val orders = v2OrderQuery.findAll(V2OrderSearch(eventId = eventId, status = status.domain, searchType = searchType, keyword = keyword))
        val users = mapper.usersOf(orders.map { it.userId })
        val rows = orders.map { order ->
            val e = mapper.toOrderElement(order, users[order.userId])
            listOf(
                e.orderNo, e.buyerName, e.buyerPhone, users[order.userId]?.profile?.email, e.ticketName, e.totalQuantity, e.totalPaymentAmount,
                e.orderedAt?.format(EXCEL_DATE), e.status?.let { STATUS_LABELS[it.name] }, REFUND_LABELS[e.refundStatus.name],
                e.refuseReason ?: e.cancelReason,
            )
        }
        return excelService.generateTableExcel("주문 목록", ORDER_HEADERS, rows)
    }

    /** F-1 환불 목록 (v1 환불 조회 쿼리 재사용, 최신 순) */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun refunds(userId: Long, eventId: Long, status: V2RefundStatusFilter?, page: Int, size: Int): V2PageResponse<V2RefundElement> {
        eventAdaptor.findById(eventId)
        val orders = orderAdaptor.findRefunds(eventId, status?.domain, null, PageRequest.of(page, size))
        val users = mapper.usersOf(orders.content.map { it.userId })
        return V2PageResponse.of(orders.map { mapper.toRefundElement(it, users[it.userId]) })
    }

    companion object {
        val EXCEL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        val ORDER_HEADERS = listOf("주문번호", "주문자", "연락처", "이메일", "티켓", "매수", "결제금액", "주문일시", "상태", "환불", "거절·취소 사유")
        private val STATUS_LABELS = mapOf(
            "PENDING_APPROVE" to "승인 대기", "APPROVED" to "승인 완료", "REFUSED" to "승인 거절", "CANCELED" to "취소", "FAILED" to "주문 실패",
        )
        private val REFUND_LABELS = mapOf("NONE" to "", "REQUESTED" to "환불 요청", "COMPLETED" to "환불 완료")
    }
}
