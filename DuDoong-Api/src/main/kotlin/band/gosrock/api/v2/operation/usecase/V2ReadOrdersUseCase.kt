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
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderPaymentChannel
import band.gosrock.domain.domains.order.service.v2.V2OrderDomainService
import band.gosrock.domain.domains.order.service.v2.V2OrderQuery
import band.gosrock.domain.domains.order.service.v2.V2OrderSearch
import band.gosrock.domain.domains.order.service.v2.V2OrderSearchType
import band.gosrock.domain.domains.order.service.v2.V2UserOrderDomainService
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadOrdersUseCase(
    private val eventAdaptor: EventAdaptor,
    private val orderAdaptor: OrderAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val v2OrderQuery: V2OrderQuery,
    private val v2OrderDomainService: V2OrderDomainService,
    private val v2UserOrderDomainService: V2UserOrderDomainService,
    private val mapper: V2OperationMapper,
    private val excelService: AdminExcelService,
    @Value("\${v2.export.max-rows:${V2OrderQuery.EXPORT_MAX_ROWS}}") private val exportMaxRows: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** R-1 주문 목록 (최신 순) + 상태별 건수 */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(
        userId: Long,
        eventId: Long,
        status: V2OrderStatusFilter,
        searchType: V2OrderSearchType?,
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
    fun detail(userId: Long, eventId: Long, orderUuid: String): V2OrderDetailResponse =
        readDetail(eventId, orderUuid, showRefundAccount = mapper.canSeeRefundAccount(userId, eventId))

    /**
     * 변경 API 응답용 (권한 검사는 호출한 UseCase 에서 끝남). 변경 트랜잭션이 커밋된 뒤 새로 읽는다.
     * [showRefundAccount]: 사용자 환불 계좌 노출 (매니저 이상, 변경 API 는 모두 매니저 이상이라 true)
     */
    @Transactional(readOnly = true)
    fun readDetail(eventId: Long, orderUuid: String, showRefundAccount: Boolean): V2OrderDetailResponse {
        val order = v2OrderDomainService.queryEventOrder(eventId, orderUuid)
        val user = mapper.usersOf(listOf(order.userId))[order.userId]
        val orderLines = order.orderLineItems.sortedBy { it.id }
        val lineAnswers = orderLines.associate { line ->
            line.id to line.orderOptionAnswers.sortedBy { it.id }.map { Triple(it.optionId, it.answer, it.additionalPrice.longValue()) }
        }
        val issued = issuedTicketAdaptor.findAllByOrderUuid(orderUuid).sortedBy { it.id }
        val ticketAnswers = issued.associate { it.id to mapper.ticketAnswerRows(it) }
        // 라인·발급 티켓 답변의 옵션 이름을 한 번에 조회
        val names = mapper.optionQuestionsOf((lineAnswers.values + ticketAnswers.values).flatten().map { it.first })
        val lines = orderLines.map { line ->
            V2OrderLineResponse(
                ticketItemId = line.orderItem?.itemId,
                ticketName = line.orderItem?.name,
                unitPrice = line.orderItem?.price?.longValue() ?: 0L,
                quantity = line.quantity ?: 0L,
                linePrice = line.getTotalOrderLinePrice().longValue(),
                optionAnswers = mapper.toOptionAnswers(lineAnswers.getValue(line.id), names),
            )
        }
        val tickets = issued.map { t ->
            V2OrderIssuedTicketResponse(
                ticketUuid = t.uuid,
                issuedTicketNo = t.issuedTicketNo,
                ticketName = t.itemInfo?.ticketName,
                entrance = V2EntranceState.of(t.issuedTicketStatus),
                enteredAt = t.enteredAt,
                optionAnswers = mapper.toOptionAnswers(ticketAnswers.getValue(t.id), names),
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
            refundAccount = if (showRefundAccount) order.id?.let { v2UserOrderDomainService.refundAccountOf(it) }?.let(mapper::toRefundAccount) else null,
        )
    }

    /**
     * R-6 엑셀 (R-1 과 같은 필터, 전체 행, 상한 [exportMaxRows] 초과 시 Order_400_19).
     * 개인정보: 연락처·입금자명(v2 두둥티켓 주문, #726)은 입금 확인용으로 포함(v1 수준), 이메일은 넣지 않는다(주문 상세에서만). 다운로드는 감사 로그를 남긴다.
     * 입금자명·옵션 응답(#730, 주관식)은 사용자가 입력한 값이라 수식 인젝션 방어(escapeFormula)가 필요하다
     */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun export(userId: Long, eventId: Long, status: V2OrderStatusFilter, searchType: V2OrderSearchType?, keyword: String?): ByteArray {
        eventAdaptor.findById(eventId)
        val search = V2OrderSearch(eventId = eventId, status = status.domain, searchType = searchType, keyword = keyword)
        val orders = v2OrderQuery.findAllForExport(search, exportMaxRows)
        val users = mapper.usersOf(orders.map { it.userId })
        // 옵션 컬럼은 I-3 과 같은 규칙 (#730). 라인 답변은 findAllForExport 가 쿼리 1개로 적재, 옵션 이름은 한 번에 조회
        val columns = mapper.excelOptionColumnsOf(orders.flatMap { o -> o.orderLineItems.flatMap { line -> line.orderOptionAnswers.map { it.optionId } } }, V2ExcelHeaders.ORDER)
        val rows = orders.map { order ->
            val e = mapper.toOrderElement(order, users[order.userId])
            listOf(
                e.orderNo, e.buyerName, e.buyerPhone, e.depositorName, paymentChannelLabel(order), e.ticketName, e.quantity, e.totalAmount,
                e.orderedAt?.format(EXCEL_DATE), e.status?.let { STATUS_LABELS[it.name] }, REFUND_LABELS[e.refundStatus.name],
                e.refuseReason ?: e.cancelReason,
            ) + mapper.excelOptionCells(order.orderLineItems, columns).let { cells -> columns.groupIds.map { cells[it] } }
        }
        log.info("[V2 엑셀] 주문 다운로드 userId={} eventId={} status={} rows={}", userId, eventId, status, rows.size)
        return excelService.generateTableExcel("주문 목록", V2ExcelHeaders.ORDER + columns.headers, rows, escapeFormula = true)
    }

    /** F-1 환불 목록 (v1 환불 조회 쿼리 재사용, 최신 순) */
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun refunds(userId: Long, eventId: Long, status: V2RefundStatusFilter, page: Int, size: Int): V2PageResponse<V2RefundElement> {
        eventAdaptor.findById(eventId)
        val orders = orderAdaptor.findRefunds(eventId, status.domain, null, PageRequest.of(page, size))
        val users = mapper.usersOf(orders.content.map { it.userId })
        val accounts = if (mapper.canSeeRefundAccount(userId, eventId)) v2UserOrderDomainService.refundAccountsOf(orders.content.mapNotNull { it.id }) else emptyMap()
        return V2PageResponse.of(orders.map { mapper.toRefundElement(it, users[it.userId], accounts[it.id]) })
    }

    companion object {
        val EXCEL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        private val STATUS_LABELS = mapOf(
            "PENDING_APPROVE" to "승인 대기", "APPROVED" to "승인 완료", "REFUSED" to "승인 거절", "CANCELED" to "취소", "FAILED" to "주문 실패",
        )
        private val REFUND_LABELS = mapOf("NONE" to "", "REQUESTED" to "환불 요청", "COMPLETED" to "환불 완료")

        /**
         * R-6 '결제 방식' 열 (#740): v2 주문은 저장된 결제 방식. v1 주문(값 없음)은 무료면 '무료', 카드(PG) 결제면 'PG 결제',
         * 그 밖(v1 두둥티켓 승인형 — 계좌이체·토스 구분 기록 없음)은 빈 칸
         */
        fun paymentChannelLabel(order: Order): String = order.paymentChannel?.let(::paymentChannelLabel) ?: when {
            !order.getTotalPaymentPrice().isGreaterThan(Money.ZERO) -> "무료"
            order.orderMethod == OrderMethod.PAYMENT -> "PG 결제"
            else -> ""
        }

        private fun paymentChannelLabel(channel: OrderPaymentChannel): String = when (channel) {
            OrderPaymentChannel.BANK_TRANSFER -> "계좌이체"
            OrderPaymentChannel.TOSS_TRANSFER -> "토스 송금"
            OrderPaymentChannel.FREE -> "무료"
        }
    }
}
