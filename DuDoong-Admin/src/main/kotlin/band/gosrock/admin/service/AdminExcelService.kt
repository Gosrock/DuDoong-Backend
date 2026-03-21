package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.admin.model.dto.response.AdminOrderResponse
import band.gosrock.admin.model.dto.response.AdminUserResponse
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class AdminExcelService {

    fun generateOrdersExcel(orders: List<AdminOrderResponse>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("주문 목록")
        val headerRow = sheet.createRow(0)
        listOf("주문번호", "사용자", "이벤트", "티켓", "금액", "상태", "주문일").forEachIndexed { i, h ->
            headerRow.createCell(i).setCellValue(h)
        }
        orders.forEachIndexed { idx, order ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(order.orderId ?: "")
            row.createCell(1).setCellValue(order.userName ?: "")
            row.createCell(2).setCellValue(order.eventName ?: "")
            row.createCell(3).setCellValue(order.ticketName ?: "")
            row.createCell(4).setCellValue(order.totalAmount.toDouble())
            row.createCell(5).setCellValue(order.orderStatus.toString())
            row.createCell(6).setCellValue(order.createdAt?.toString() ?: "")
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

    private fun toByteArray(workbook: XSSFWorkbook): ByteArray {
        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return out.toByteArray()
    }
}
