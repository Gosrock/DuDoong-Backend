package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.admin.model.dto.response.AdminIssuedTicketResponse
import band.gosrock.admin.model.dto.response.AdminOrderResponse
import band.gosrock.admin.model.dto.response.AdminTicketItemResponse
import band.gosrock.admin.model.dto.response.AdminUserResponse
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.ticket_item.domain.Option
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class AdminExcelService {

    fun generateOrdersExcel(orders: List<AdminOrderResponse>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("주문 목록")
        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "주문번호", "주문번호(읽기용)", "사용자", "이벤트", "티켓", "금액", "상태",
            "주문방식", "결제수단", "승인일시", "취소일시", "할인금액", "쿠폰명", "주문일",
        )
        headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }
        orders.forEachIndexed { idx, order ->
            val row = sheet.createRow(idx + 1)
            var col = 0
            row.createCell(col++).setCellValue(order.orderId ?: "")
            row.createCell(col++).setCellValue(order.orderNo ?: "")
            row.createCell(col++).setCellValue(order.userName ?: "")
            row.createCell(col++).setCellValue(order.eventName ?: "")
            row.createCell(col++).setCellValue(order.ticketName ?: "")
            row.createCell(col++).setCellValue(order.totalAmount.toDouble())
            row.createCell(col++).setCellValue(order.orderStatus.toString())
            row.createCell(col++).setCellValue(order.orderMethod ?: "")
            row.createCell(col++).setCellValue(order.paymentMethod ?: "")
            row.createCell(col++).setCellValue(order.approvedAt?.toString() ?: "")
            row.createCell(col++).setCellValue(order.withDrawAt?.toString() ?: "")
            row.createCell(col++).setCellValue(order.discountAmount ?: "")
            row.createCell(col++).setCellValue(order.couponName ?: "")
            row.createCell(col++).setCellValue(order.createdAt?.toString() ?: "")
        }
        return toByteArray(workbook)
    }

    fun generateEventsExcel(events: List<AdminEventResponse>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("이벤트 목록")
        val headerRow = sheet.createRow(0)
        listOf("ID", "이벤트명", "호스트", "상태", "시작일", "런타임(분)", "생성일").forEachIndexed { i, h ->
            headerRow.createCell(i).setCellValue(h)
        }
        events.forEachIndexed { idx, event ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(event.id.toDouble())
            row.createCell(1).setCellValue(event.name ?: "")
            row.createCell(2).setCellValue(event.hostName ?: "")
            row.createCell(3).setCellValue(event.status.toString())
            row.createCell(4).setCellValue(event.startAt?.toString() ?: "")
            row.createCell(5).setCellValue(event.runTime?.toDouble() ?: 0.0)
            row.createCell(6).setCellValue(event.createdAt?.toString() ?: "")
        }
        return toByteArray(workbook)
    }

    fun generateUsersExcel(users: List<AdminUserResponse>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("유저 목록")
        val headerRow = sheet.createRow(0)
        listOf("ID", "이름", "이메일", "역할", "상태", "가입일").forEachIndexed { i, h ->
            headerRow.createCell(i).setCellValue(h)
        }
        users.forEachIndexed { idx, user ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(user.id.toDouble())
            row.createCell(1).setCellValue(user.name ?: "")
            row.createCell(2).setCellValue(user.email ?: "")
            row.createCell(3).setCellValue(user.accountRole.toString())
            row.createCell(4).setCellValue(user.accountState.toString())
            row.createCell(5).setCellValue(user.createdAt?.toString() ?: "")
        }
        return toByteArray(workbook)
    }

    fun generateTicketItemsExcel(items: List<AdminTicketItemResponse>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("티켓 종류 목록")
        val headerRow = sheet.createRow(0)
        listOf("이름", "설명", "가격", "수량", "판매수", "구매제한", "타입", "상태").forEachIndexed { i, h ->
            headerRow.createCell(i).setCellValue(h)
        }
        items.forEachIndexed { idx, item ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(item.name ?: "")
            row.createCell(1).setCellValue(item.description ?: "")
            row.createCell(2).setCellValue(item.price?.toDouble() ?: 0.0)
            row.createCell(3).setCellValue(item.quantity?.toDouble() ?: 0.0)
            row.createCell(4).setCellValue(item.supplyCount?.toDouble() ?: 0.0)
            row.createCell(5).setCellValue(item.purchaseLimit?.toDouble() ?: 0.0)
            row.createCell(6).setCellValue(item.type?.toString() ?: "")
            row.createCell(7).setCellValue(item.ticketItemStatus.toString())
        }
        return toByteArray(workbook)
    }

    fun generateIssuedTicketsExcel(tickets: List<AdminIssuedTicketResponse>): ByteArray {
        return generateIssuedTicketsExcelWithOptions(tickets, emptyList(), emptyList())
    }

    /**
     * 발급 티켓 엑셀에 옵션 응답을 동적 컬럼으로 포함하여 생성합니다.
     *
     * @param tickets 발급 티켓 응답 목록
     * @param options 이벤트에 등록된 옵션 목록 (동적 컬럼 헤더용)
     * @param issuedTickets 발급 티켓 엔티티 목록 (옵션 응답 데이터 접근용)
     */
    fun generateIssuedTicketsExcelWithOptions(
        tickets: List<AdminIssuedTicketResponse>,
        options: List<Option>,
        issuedTickets: List<IssuedTicket>,
    ): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("발급 티켓 목록")
        val headerRow = sheet.createRow(0)

        // 기본 헤더
        val baseHeaders = listOf("티켓번호", "유저명", "티켓종류", "주문번호", "입장여부", "발급일")
        baseHeaders.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

        // 옵션 동적 헤더
        val optionHeaders = options.map { it.getQuestionName() ?: "옵션(${it.id})" }
        optionHeaders.forEachIndexed { i, h ->
            headerRow.createCell(baseHeaders.size + i).setCellValue(h)
        }

        // IssuedTicket id -> IssuedTicket 맵 (옵션 응답 조회용)
        val ticketEntityMap = issuedTickets.associateBy { it.id }

        tickets.forEachIndexed { idx, ticket ->
            val row = sheet.createRow(idx + 1)
            var col = 0
            row.createCell(col++).setCellValue(ticket.issuedTicketNo ?: "")
            row.createCell(col++).setCellValue(ticket.userName ?: "")
            row.createCell(col++).setCellValue(ticket.ticketName ?: "")
            row.createCell(col++).setCellValue(ticket.orderUuid ?: "")
            row.createCell(col++).setCellValue(if (ticket.enteredAt != null) "입장" else "미입장")
            row.createCell(col++).setCellValue(ticket.createdAt?.toString() ?: "")

            // 옵션 응답 채우기
            if (options.isNotEmpty()) {
                val entity = ticketEntityMap[ticket.id]
                val answerMap = entity?.issuedTicketOptionAnswers
                    ?.associateBy { it.optionId } ?: emptyMap()
                options.forEach { option ->
                    val answer = answerMap[option.id]?.answer ?: ""
                    row.createCell(col++).setCellValue(answer)
                }
            }
        }
        return toByteArray(workbook)
    }

    private fun toByteArray(workbook: XSSFWorkbook): ByteArray {
        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return out.toByteArray()
    }
}
