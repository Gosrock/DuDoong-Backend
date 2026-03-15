package band.gosrock.helper.excel

import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component

@Component
class ExcelOrderHelper {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun execute(excelOrders: List<ExcelOrderDto>): ByteArrayOutputStream {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("orderList")
            for (i in 0..8) {
                sheet.setColumnWidth(i, 6000)
            }

            // create header
            val header = sheet.createRow(0)
            val headerStyle = workbook.createCellStyle()
            headerStyle.fillForegroundColor = IndexedColors.LIGHT_GREEN.index
            headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont()
            font.fontName = "Arial"
            font.fontHeightInPoints = 16
            font.bold = true
            headerStyle.setFont(font)

            val headers = listOf(
                "주문번호", "주문 방식", "주문 상태", "주문 이름",
                "주문자 아이디", "총액", "티켓 수량", "생성 일시", "환불 일시",
            )
            headers.forEachIndexed { idx, title ->
                val cell = header.createCell(idx)
                cell.setCellValue(title)
                cell.cellStyle = headerStyle
            }

            val style = workbook.createCellStyle()
            style.wrapText = true

            excelOrders.forEachIndexed { idx, excelOrderDto ->
                val row = sheet.createRow(idx + 1)
                row.createCell(0).setCellValue(excelOrderDto.orderNo)
                row.createCell(1).setCellValue(excelOrderDto.orderMethod.kr)
                row.createCell(2).setCellValue(excelOrderDto.orderStatus.kr)
                row.createCell(3).setCellValue(excelOrderDto.orderName)
                row.createCell(4).setCellValue(excelOrderDto.userId.toDouble())
                row.createCell(5).setCellValue(excelOrderDto.amount.toString())
                row.createCell(6).setCellValue(excelOrderDto.quantity.toDouble())
                row.createCell(7).setCellValue(excelOrderDto.createdAt.format(formatter))
                row.createCell(8).setCellValue(excelOrderDto.refundAt?.format(formatter))
            }

            val outputStream = ByteArrayOutputStream()
            workbook.write(outputStream)
            workbook.close()
            return outputStream
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
